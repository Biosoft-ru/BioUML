package ru.biosoft.bsa.track.big;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Site;
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
            Object scoreObj = s.getProperties().getValue( Site.SCORE_PROPERTY );
            float score = 0;
            if(scoreObj instanceof Number)
                score = ((Number)scoreObj).floatValue();
            if(score > maxScore)
                maxScore = score;
            if(score < minScore)
                minScore = score;
        }
        if(maxScore == minScore)//==0
            return result;
        
        int maxHeight = viewOptions.getMaxProfileHeight();
        
        
        if(viewOptions.isShowValuesRange())
        {
            TextView scoreRangeView = new TextView( "["+minScore+" - "+maxScore+"]", new Point(0,0), View.LEFT | View.TOP, viewOptions.getFont() , graphics );
            result.add( scoreRangeView );
        }else
        {
            result.add(new LineView( new Pen( 1, new Color(0,0,0,0)), 0, 0, 0, maxHeight ));//invisible line just to fix the height of result
        }
        
        
        for(Site s : sites) {
            Object scoreObj = s.getProperties().getValue( Site.SCORE_PROPERTY );
            float score = 0;
            if(scoreObj instanceof Number)
                score = ((Number)scoreObj).floatValue();
            
            Brush brush = siteViewOptions.getColorScheme().getBrush( s );
            Pen pen = null;
            
            int height;
            
            if(viewOptions.isAutoScale())
                height = Math.round(maxHeight * score / (maxScore - minScore));
            else
            {
                height = Math.round( score*viewOptions.getScale() );
                if(height > maxHeight)
                    height = maxHeight;
            }
            if(height == 0)
                continue;
            View msSiteView = barView( s.getFrom(), s.getTo(), maxHeight, height, pen, brush, sequenceView, graphics, start, end );
            msSiteView.setSelectable( true );
            msSiteView.setModel( s );
            result.add( msSiteView );
        }
        
        return result;
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
