package ru.biosoft.bsa.view.sitelayout;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Comparator;
import java.util.Vector;

import ru.biosoft.bsa.view.TrackView;
import ru.biosoft.graphics.View;

/**
 * ListLayoutAlgorithm - algorithm for sorting
 * sites in SiteSetView using Vector to contain
 * sorting data
 */
public class OptimizedListLayoutAlgorithm extends AbstractLayoutAlgorithm
{
    /** layout algorithm function */
    @Override
    public void layout(TrackView view, int direction)
    {
        //sort SiteSetView
        view.sort( Comparator.comparingInt( v -> v.getBounds().x ) );

        View v = null;
        Vector<Rectangle> list = new Vector<>();
        int viewSize = view.size();
        if( viewSize < 2 )
            return;

        Rectangle next;
        //for every view
        for( int currentIndex = 0; currentIndex < viewSize; currentIndex++ )
        {
            v = view.elementAt(currentIndex);
            //get it`s bounds
            Rectangle rect = (Rectangle)v.getBounds().clone();
            Point p = rect.getLocation();
            // Set y position to start position and increase it until putting of rectangle is clean
            p.y = direction * STARTYPOSITION;
            rect.setLocation(p.x, p.y);
            while( rangeIntersect(list, rect) )
            {
                // @todo low
                // (cher) : 10 should be adjusted
                p.y += direction * 10;
                rect.setLocation(p.x, p.y);
            }
            // add rect to list
            list.add(rect);
            // Set new view location
            v.setLocation(p.x, p.y);
            // every STEP count of sites we adjust positions
            if( currentIndex % ADJUST_START_STEP == 0 )
            {
                //changing start position
                Vector<Rectangle> newList = new Vector<>();
                for( int i = list.size() - 1; i >= 0; i-- )
                {
                    next = list.get(i);
                    if( next.x + next.width >= p.x )
                    {
                        // add this rectangle to new list
                        newList.add(next);
                    }
                }
                list = newList;
            }
        }
        //completely finishing algorithm work - updating bounds of view
        view.updateBounds();
        if( direction < 0 )
        {
            int h = view.getBounds().height;
            for( int currentIndex = 0; currentIndex < viewSize; currentIndex++ )
            {
                view.elementAt(currentIndex).move(0, h);
            }
            view.updateBounds();
        }
    }

    /**
     * Auxiliary function for layout algorithm
     * @param list to search
     * @param bounds of current site
     * @return true if it intersect with anything
     * @todo low there are some constants, which should be adjusted
     */
    protected boolean rangeIntersect(Vector<Rectangle> list, Rectangle rect1)
    {
        Rectangle rect = (Rectangle)rect1.clone();
        rect.x -= 5;
        rect.width += 10;
        rect.y -= 1;
        rect.height += 2;
        //
        // Check intersection with elements from current downto start
        for( int i = list.size() - 1; i >= 0; i-- )
        {
            if( list.get(i).intersects(rect) )
            {
                //it intersects - exiting
                return true;
            }
        }
        // it not intersects with anything
        return false;
    }
}
