package ru.biosoft.bsa.track.big;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.TreeMap;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.track.combined.SiteGroup;
import ru.biosoft.bsa.view.MapJobControl;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;

public class BigWigTrackViewBuilder extends TrackViewBuilder
{
    
    @Override
    public SiteViewOptions createViewOptions()
    {
        return new BigWigViewOptions();
    }
    
    @Override
    public CompositeView createTrackView(SequenceView sequenceView, DataCollection<Site> sites, SiteViewOptions siteViewOptions,
            int start, int end, int direction, Graphics graphics, MapJobControl control)
    {
        CompositeView result = new CompositeView();
        if(sites.isEmpty())
            return result;
        
        BigWigViewOptions viewOptions = (BigWigViewOptions)siteViewOptions;
        
        float maxScore = 0;
        float minScore = 0;
        for(Site s : sites) {
            float score = getSiteScore(s);
            if(score > maxScore)
                maxScore = score;
            if(score < minScore)
                minScore = score;
        }
        if(maxScore == minScore)//==0
            return result;
        
        int maxHeight = viewOptions.getMaxProfileHeight();
        
        
        double pixelsPerNucleotide = sequenceView.getNucleotideWidth();
        if(pixelsPerNucleotide < 1)
        {
        	int pixelStart = sequenceView.getStartPoint(0, graphics).x;
        	int pixelEnd = sequenceView.getEndPoint(end-start, graphics).x;
        	TreeMap<Integer, IntervalData> groups = new TreeMap<>();
        	for(int pixelOffset = pixelStart; pixelOffset <= pixelEnd;pixelOffset++)
        	{
        		int seqPos = start + sequenceView.getPosition(pixelOffset, graphics);
        		int nextSeqPos = start + sequenceView.getPosition(pixelOffset+1, graphics);
        		IntervalData id = new IntervalData(seqPos,nextSeqPos-1, pixelOffset);
        		groups.put(seqPos, id);
        	}
        	for(Site s : sites)
        	{
        		for(IntervalData id : groups.headMap(s.getTo(), true).descendingMap().values())
        		{
        			int intersectionFrom = Math.max(s.getFrom(), id.from);
        			int intersectionTo = Math.min(s.getTo(), id.to);
        			if(intersectionFrom > intersectionTo)
        				break;
        			int intersectionLength = (intersectionTo-intersectionFrom+1); 
        			id.sum += intersectionLength*getSiteScore(s);
        			id.covered += intersectionLength;
        		}
        	}
        	maxScore = 0;
        	minScore = 0;
        	for(IntervalData id : groups.values())
        	{
        		id.build();
        		if(id.getMean() > maxScore)
        			maxScore = id.getMean();
        		else if(id.getMean() < minScore)
        			minScore = id.getMean();
        	}
        	Pen pen = new Pen(1, viewOptions.getColor());
        	for(IntervalData id : groups.values())
        	{
        		float score = (float)id.getMean();
        		int height = getHeightForScore(score, minScore, maxScore, viewOptions);
        		if(height == 0)
        			continue;
        		View view = new LineView(pen, id.pixel, maxHeight, id.pixel, maxHeight-height);
        		result.add(view);
        	}
		} else {

			Brush brush = new Brush(viewOptions.getColor());
			Pen pen = new Pen(1, viewOptions.getColor());
			for (Site s : sites) {
				float score = getSiteScore(s);

				int height = getHeightForScore(score, minScore, maxScore, viewOptions);
				if (height == 0)
					continue;
				View msSiteView = barView(s.getFrom(), s.getTo(), maxHeight, height, pen, brush, sequenceView, graphics,
						start, end);
				msSiteView.setSelectable(true);
				msSiteView.setModel(s);
				result.add(msSiteView);
			}

		}
        
        if(viewOptions.isShowValuesRange())
        {
            TextView scoreRangeView = new TextView( "["+minScore+" - "+maxScore+"]", new Point(0,0), View.LEFT | View.TOP, viewOptions.getFont() , graphics );
            result.add( scoreRangeView );
        }else
        {
            result.add(new LineView( new Pen( 1, new Color(0,0,0,0)), 0, 0, 0, maxHeight ));//invisible line just to fix the height of result
        }
        
        return result;
    }

	public int getHeightForScore(float score, float minScore, float maxScore, BigWigViewOptions viewOptions) {
		int height;
		int maxHeight = viewOptions.getMaxProfileHeight();
		if (viewOptions.isAutoScale())
			height = Math.round(maxHeight * score / (maxScore - minScore));
		else {
			height = Math.round(score * viewOptions.getScale());
			if (height > maxHeight)
				height = maxHeight;
		}
		return height;
	}

	public float getSiteScore(Site s) {
		Object scoreObj = s.getProperties().getValue(Site.SCORE_PROPERTY);
		float score = 0;
		if (scoreObj instanceof Number)
			score = ((Number) scoreObj).floatValue();
		return score;
	}
    
    static class IntervalData
    {
    	IntervalData(int from, int to, int pixel)
    	{
    		this.from = from;
    		this.to = to;
    		this.pixel = pixel;
    	}
    	int from, to;
    	int pixel;
    	double sum;
    	int covered;
    	
    	float mean;
    	void build()
    	{
    		mean = (float) ( sum / (to-from+1) );
    	}
    	float getMean()
    	{
    		return mean;
    	}
    }
    
    public static View barView(int start, int end, int maxHeight, int height, Pen pen, Brush brush, SequenceView sequenceView, Graphics graphics, int seqStart, int seqEnd )
    {
        // Beginning drawing sites from 1
        start -= seqStart - 1;
        end -= seqStart - 1;
        Point startPoint = sequenceView.getStartPoint( start, graphics );
        Point endPoint = sequenceView.getEndPoint( end, graphics );
        int width = Math.max( 1, endPoint.x - startPoint.x );
        return new BoxView( pen, brush, startPoint.x, maxHeight-height, width, height );
    }
}
