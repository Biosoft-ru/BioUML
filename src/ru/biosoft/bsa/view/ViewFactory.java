
package ru.biosoft.bsa.view;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;

import ru.biosoft.jobcontrol.JobControlException;

/**
 *  Class used to create maps view.
 */
public class ViewFactory
{
    /** Stores the maximum width of sub-maps when map painted as a set of map one under another. */
    static Logger cat = Logger.getLogger(ViewFactory.class.getName());

    /////////////////////////////////////////
    // Public functions to create map views
    //


    /**
     * Creates view of the map from <code>DataCollection</code> using <code>mapOptions</code>
     * to adjust visualization of view properties.
     * @param graphics <code>Graphics</code> in which view created
     * @param control <code>JobControl</code> to indicate view creating process
     * @param regions regions to display
     * @return new <code>View</code> of the map
     */
    public static View createProjectView(Project model, final Graphics graphics, final MapJobControl control)
    {
        final CompositeView mapView = new CompositeView();

        try
        {
            synchronized( model )
            {
                if( graphics != null )
                {
                    Region[] regions = model.getRegions();
                    if( control != null )
                        control.setMapNumber(regions.length);
                    int currentMapNumber = 0;
                    if( control != null )
                    {
                        control.setCurrentMap(currentMapNumber);
                    }

                    for( Region region : regions )
                    {
                        if( !region.isVisible() )
                        {
                            continue;
                        }

                        View view = createMapView(model, region, graphics, control);
                        if( view == null )
                        {
                            continue;
                        }
                        if( control != null )
                        {
                            control.resultsAreReady();
                        }

                        // add map name
                        currentMapNumber++;
                        if( control != null )
                        {
                            control.setCurrentMap(currentMapNumber);
                        }
                        MapViewOptions regionViewoptions = model.getViewOptions().getRegionViewOptions();
                        TextView nameView = new TextView(region.getTitle(), regionViewoptions.getFont(), graphics);
                        mapView.add(nameView, CompositeView.Y_BT, new Point(0, regionViewoptions.getInterval()));
                        mapView.add(view, CompositeView.Y_BT);
                    }

                    if( control != null )
                    {
                        control.functionFinished("Ready");
                    }
                }
            }
        }
        catch( JobControlException ex )
        {
            cat.log(Level.SEVERE, "", ex);
        }
        catch( Throwable t )
        {
            cat.log(Level.SEVERE, "", t);
        }

        return mapView;
    }

    /**
     * Creates view of the part of map from <code>start</code> to <code>end</code>
     * using <code>mapOptions</code> to adjust visualization of view properties.
     * @param graphics <code>Graphics</code> in which view created
     * @param control <code>JobControl</code> to indicate view creating process
     * @param map <code>Map</code> map top display
     * @return new <code>View</code> of the map
     */
    public static View createMapView(Project model, Region region, Graphics graphics, MapJobControl control) throws JobControlException
    {
        ViewOptions options = model.getViewOptions();

        CompositeView maps = new CompositeView();
        MapView mapView;

        Interval slice = region.getInterval().intersect(region.getSequence().getInterval());

        MapViewOptions regionViewoptions = options.getRegionViewOptions();
        int maxSequenceLength = regionViewoptions.getMaxSequenceLength();
        int mw = regionViewoptions.getMaxWidth();
        int interval = regionViewoptions.getInterval();

        if( control != null )
        {
            control.setSequenceLength(slice.getLength());
            control.setCurrentLength(0);
            control.functionStarted("Creating image:");
        }
        if( maxSequenceLength != 0 && maxSequenceLength < slice.getLength() )
        {
            CompositeView cv = new CompositeView();
            String[] msgs = {" This sequence is too long to be displayed in detailed mode.",
                    " Current sequence length is " + slice.getLength() + ", but maximum length is " + maxSequenceLength,
                    " To display the sequence, increase the maximum allowed",
                    " sequence length in the preferences or use the region filter", " to reduce the size of the current sequence."

            };
            for( String msg : msgs )
                cv.add(new TextView(msg, new ColorFont("SansSerif", Font.BOLD, 12), graphics), CompositeView.Y_BT);
            return cv;
        }

        for(Interval subSlice: slice.splitByStep(mw))
        {
            if( control != null )
            {
                control.setCurrentLength(subSlice.getFrom());
            }
            mapView = new MapView(model, region, options, subSlice, graphics, control);

            if( subSlice.getFrom() != slice.getFrom() ) // make distance between map parts
                mapView.move(0, maps.getBounds().height + interval);
            if( mapView.size() > 0 )
                maps.add(mapView);
            if( control != null )
            {
                control.checkStatus();
                if( cat.isLoggable( Level.FINE ) )
                    cat.log( Level.FINE, "createMapView " + subSlice );
            }
        }

        return maps.size() != 0 ? maps : null;
    }



    ////////////////////////////////////////
    // Protected functions
    //

    /**
     * Creates view of the sequence from <code>start</code> to <code>end</code>
     * using <code>mapOptions</code> to adjust visualization of
     * view properties.
     * @author Igor V. Tyazhev
     * @param map    <code>Map</code> containing interesting sequence
     * @param mapOptions
     *               options of the view
     * @param start  where sequence starts
     * @param end    where sequence ends
     * @param graphics <code>Graphics</code> in which view created
     *
     * @return new <code>SequenceView</code> of the sequence
     */
    public static SequenceView createSequenceView(Sequence sequence, SequenceViewOptions sequenceViewOptions, int start, int end,
            Graphics graphics)
    {
        return new SequenceView(sequence, sequenceViewOptions, start, end, graphics);
    }

    public static View createLine(boolean asArrow, Pen p, Brush b, Point start, Point end, int arrowStart, int arrowEnd)
    {
        return asArrow ? (View)new ArrowView(p, b, start, end, arrowStart, arrowEnd) : (View)new LineView(p, start, end);
    }
}
