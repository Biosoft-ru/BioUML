package ru.biosoft.bsa.view;

import java.awt.Graphics;
import java.awt.Point;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author lan
 *
 */
public class SqlTrackViewBuilder extends DefaultTrackViewBuilder
{
    /**
     * Creates CompositeView for selected part of track
     */
    @Override
    public CompositeView createTrackView(SequenceView sequenceView, DataCollection<Site> sites, SiteViewOptions siteViewOptions, int start,
            int end, int direction, Graphics graphics, MapJobControl control)
    {
        int size = sites.getSize();

        if(size > SITE_COUNT_HARD_LIMIT)
        {
            CompositeView trackView = new CompositeView();
            String labelString = String.valueOf(size) + " sites";
            TextView siteCountLabel = new TextView(labelString, siteViewOptions.getTrackTitleFont(), ApplicationUtils.getGraphics());
            ( trackView ).add(siteCountLabel, CompositeView.X_CC,
                    new Point(sequenceView.getStartPoint( ( start + end ) / 2 - start + 1, graphics).x, 0));
            return trackView;
        } else if(size > SITE_COUNT_LIMIT)
        {
            return createProfileView(sequenceView, sites, siteViewOptions, start, end, direction, graphics, control);
        }

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

}
