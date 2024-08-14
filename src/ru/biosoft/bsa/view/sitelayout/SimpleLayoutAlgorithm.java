package ru.biosoft.bsa.view.sitelayout;

import java.awt.Point;
import java.awt.Rectangle;

import ru.biosoft.bsa.view.TrackView;
import ru.biosoft.graphics.View;

/**
 * Class with algorithm for simle sorting of SiteViews in SiteSetView.
 * @author Evgenij S. Cheremushkin
 */
public class SimpleLayoutAlgorithm extends AbstractLayoutAlgorithm
{
    /** layout algorithm */
    @Override
    public void layout(TrackView view, int direction)
    {
        View v = null;
        int viewSize = view.size();
        if( viewSize < 2 )
        {
            return;
        }
        for( int i = 0; i < viewSize; i++ )
        {
            v = view.elementAt(i);
            //get rectangle of this view
            Rectangle rect = (Rectangle)v.getBounds().clone();
            Point p = rect.getLocation();
            // Set y position to start position and increase it until putting of it is clean
            p.y = STARTYPOSITION;
            rect.setLocation(p.x, p.y);
            // Increase y position while rect intersect with anything before(!) it
            while( rangeIntersect(view, 0, i, rect) )
            {
                // PENDING (cher) : 10 should be adjusted
                p.y += 10;
                rect.setLocation(p.x, p.y);
            }
            // Set new view location
            v.setLocation(p.x, p.y);
        }
        view.updateBounds();
    }

    /**
     * Auxiliary function for layout algorithm
     * @param SiteView to search
     * @param starting site
     * @param current site
     * @param bounds of current site
     * @return true if it intercect with anything
     * @pending make some constants
     */
    protected boolean rangeIntersect(TrackView view, int start, int current, Rectangle rect1)
    {
        Rectangle rect = (Rectangle)rect1.clone();
        rect.x -= 5;
        rect.width += 10;
        rect.y -= 1;
        rect.height += 2;
        //
        // Check intersection with elements from current downto start
        for( int index = current - 1; index >= start; index-- )
        {
            if( view.elementAt(index).getBounds().intersects(rect) )
            {
                //it intersects - exiting
                return true;
            }
        }
        // it not intersects with anything
        return false;
    }
}
