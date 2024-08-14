package ru.biosoft.bsa.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Properties;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LimitedSizeSitesCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;

/**
 * Basic interface for drawing track
 */
public class TrackViewBuilder
{
    public static final int SITE_COUNT_LIMIT = 1000;
    public static final int SITE_COUNT_HARD_LIMIT = 100000;
    /**
     *  Initialize with properties
     */
    public void init(Properties properties) {}
    
    public SiteViewOptions createViewOptions()
    {
        return new SiteViewOptions();
    }
    
    /**
     * Creates CompositeView for selected part of track
     */
    public CompositeView createTrackView(SequenceView sequenceView, DataCollection<Site> sites, SiteViewOptions siteViewOptions, int start,
            int end, int direction, Graphics graphics, MapJobControl control)
    {
        int size;
        String label;
        if(sites instanceof LimitedSizeSitesCollection)
        {
            size = ( (LimitedSizeSitesCollection)sites ).getSizeLimited(SITE_COUNT_HARD_LIMIT+1);
            if(size > SITE_COUNT_HARD_LIMIT) label = ">"+SITE_COUNT_HARD_LIMIT; else label = String.valueOf(size);
        } else
        {
            size = sites.getSize();
            label = String.valueOf(size);
        }

        if(size > SITE_COUNT_LIMIT)
        {
            CompositeView trackView = new CompositeView();
            String labelString = label + " sites";
            TextView siteCountLabel = new TextView(labelString, siteViewOptions.getTrackTitleFont(), ApplicationUtils.getGraphics());
            ( trackView ).add(siteCountLabel, CompositeView.X_CC,
                    new Point(sequenceView.getStartPoint( ( start + end ) / 2 - start + 1, graphics).x, 0));
            return trackView;
        }

        return doCreateTrackView(sequenceView, sites, siteViewOptions, start, end, direction, graphics, control, size);
    }

    protected CompositeView doCreateTrackView(SequenceView sequenceView, DataCollection<Site> sites, SiteViewOptions siteViewOptions,
            int start, int end, int direction, Graphics graphics, MapJobControl control, int size)
    {
        int i = 0;
        TrackView trackView = new TrackView();
        
        for( Site site : sites )
        {
            View view = createSiteView(sequenceView, site, siteViewOptions, start, end, graphics, size);
            if( view != null )
                trackView.add(view, CompositeView.Y_BT);
            if( control != null )
                control.setCurrentLength(start + (int) ( ( (double) ( end - start ) ) * ( (double) ( i++ ) ) / ( size ) ));
        }
        
        siteViewOptions.getLayoutAlgorithm().layout(trackView, direction);
        return trackView;
    }
    
    /**
     * Creates <code>View</code> of the specified <code>site</code>
     * in the current SiteSet
     * @param site   <code>site</code> for which view should be created
     * @return new copy of (possibly already exist) <code>View</code> of the
     *         specified <code>site</code>
     */

    protected View createSiteView(SequenceView sequenceView, Site site, SiteViewOptions siteViewOptions, int start, int end,
            Graphics graphics, int sitesCount)
    {
        return null;
    }

    protected CompositeView createProfileView(SequenceView sequenceView, DataCollection<Site> sites, SiteViewOptions siteViewOptions, int start, int end, int direction, Graphics graphics,
            MapJobControl control)
    {
        float[] buckets = getCoverage(sequenceView, sites, start, end, graphics);
        int startPoint = sequenceView.getStartPoint(1, graphics).x;
        double maxHeight = DoubleStreamEx.of( buckets ).max().orElse( 0 );
        CompositeView result = new CompositeView();
        if(maxHeight == 0) return result;
        int realHeight = Math.max(Math.min((int)maxHeight, siteViewOptions.getMaxProfileHeight()), siteViewOptions.getMinProfileHeight());
        float lastHeight = buckets[0];
        int lastPos = 0;
        Brush brush = siteViewOptions.getColorScheme().getDefaultBrush();
        for(int i=1; i<=buckets.length; i++)
        {
            float height = i==buckets.length?-1:buckets[i];
            if(height != lastHeight)
            {
                if(lastHeight > 0)
                {
                    int startBlockPoint = lastPos+startPoint;
                    int endBlockPoint = i+startPoint;
                    int viewHeight = (int) ( lastHeight*realHeight/maxHeight );
                    BoxView view = new BoxView(null, brush, startBlockPoint, realHeight-viewHeight, endBlockPoint-startBlockPoint, viewHeight);
                    view.setSelectable(true);
                    view.setDescription("Coverage: "+String.format("%.2f", lastHeight));
                    result.add(view);
                }
                lastHeight = height;
                lastPos = i;
            }
        }
        
        int tick = (int)Math.pow(10, (int)Math.log10(maxHeight));
        if(tick == 0) tick = 1;
        tick = ((int)maxHeight / tick) * tick;
        float tickPosY = (float) ( realHeight - tick * realHeight / maxHeight );
        result.add(new LineView(new Pen(1, Color.BLACK), startPoint, tickPosY, startPoint + 5, tickPosY));
        result.add(new LineView(new Pen(1, Color.BLACK), startPoint, realHeight, startPoint + 5, realHeight));
        result.add(new LineView(new Pen(1, Color.BLACK), startPoint + 1, realHeight, startPoint + 1, 0));
        result.add(new TextView(tick + "x", new Point(startPoint + 5, (int)tickPosY), View.LEFT, siteViewOptions.getFont(), graphics));
        
        return result;
    }

    protected float[] getCoverage(SequenceView sequenceView, DataCollection<Site> sites, int start, int end, Graphics graphics)
    {
        int startPoint = sequenceView.getStartPoint(1, graphics).x;
        int endPoint = sequenceView.getEndPoint(end-start+1, graphics).x;
        float[] buckets = new float[endPoint-startPoint+1];
        int[] positions = IntStreamEx.rangeClosed( startPoint, endPoint + 1 )
                .map( i -> sequenceView.getPosition( i, graphics ) + start - 1 ).toArray();
        for(Site s: sites)
        {
            int siteStartPoint = Math.max(startPoint, sequenceView.getStartPoint(s.getFrom()-start+1, graphics).x);
            int siteEndPoint = Math.min(endPoint, sequenceView.getEndPoint(s.getTo()-start+1, graphics).x);
            for(int i=siteStartPoint; i<=siteEndPoint; i++)
            {
                buckets[i - startPoint] += getIntersectionLength(s, positions[i - startPoint], positions[i
                        - startPoint + 1] - 1)
                        / Math.max(positions[i - startPoint + 1] - positions[i - startPoint], 1.0);
            }
        }
        return buckets;
    }

    protected int getIntersectionLength(Site site, int from2, int to2)
    {
        return getIntersectionLength(site.getFrom(), site.getTo(), from2, to2);
    }
    
    protected int getIntersectionLength(int from1, int to1, int from2, int to2)
    {
        if(to2<from2) to2=from2;
        return Math.max(0, Math.min(to1, to2)-Math.max(from1, from2)+1);
    }

    protected Interval getInterval(Site site, int start, int end, SequenceView sequenceView, Graphics graphics)
    {
        // evaluate coordinates and name
        int startX = site.getFrom();
        int endX = site.getTo();
    
        if( startX > end || endX < start )
        {
            return null;
        }
    
        if( startX < start )
        {
            startX = start;
        }
        if( endX > end )
        {
            endX = end;
        }
    
        // Beginning drawing sites from 1
        startX -= start - 1;
        endX -= start - 1;
    
        Point startPoint = sequenceView.getStartPoint(startX, graphics);
        Point endPoint = sequenceView.getEndPoint(endX, graphics);
        if(endPoint.x == startPoint.x) endPoint.x++;
        return new Interval(startPoint.x, endPoint.x);
    }
}
