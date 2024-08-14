
package ru.biosoft.bsa.view;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.util.logging.Level;
import java.util.List;
import java.util.function.BiConsumer;

import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.bsa.view.sitelayout.SiteLayoutAlgorithm;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;

/**
 * Class for visualisation of the maps.
 */
public class MapView extends CompositeView
{
    private static Logger log = Logger.getLogger(MapView.class.getName());


    /** Left offset for tracks title*/
    public static final int LEFT_TITLE_OFFSET = 200;

    /** Region to display*/
    protected Region region;

    /** Interval in corresponding sequence. */
    protected Interval interval;

    /** View of sequence on which this map is defined */
    protected SequenceView sequenceView;

    /**
     * Add error message to the category and generates an image,
     * that will putted in the map view.
     */
    protected View processError(String msg, Throwable e, Graphics g)
    {
        log.log(Level.SEVERE, msg, e);
        return new TextView("Error! " + msg + ": " + e.getMessage(), new ColorFont("SansSerif", Font.BOLD, 12), g);
    }

    /**
     * Constructor.
     * @param map      <code>SiteSet</code> for which this <code>MapView</code> is created.
     * @param options  Settings of the view.
     * @param start    Begin of <code>map</code> in corresponding sequence.
     * @param end      End of <code>map</code> in corresponding sequence.
     */
    public MapView(Project model, Region region, ViewOptions options, Interval interval, Graphics graphics, MapJobControl control)
    {
        this.model = model;
        this.region = region;
        this.interval = interval;

        MapViewOptions regionViewoptions = options.getRegionViewOptions();

        // create sequence view
        try
        {
            sequenceView = ViewFactory.createSequenceView(region.getSequence(), regionViewoptions.getSequenceViewOptions(),
                    interval.getFrom(), interval.getTo(), graphics);
        }
        catch( Throwable e )
        {
            String msg = "Creating sequence view: project=" + model.getName() + ", region=" + region.getTitle() + " " + interval;
            View error = processError(msg, e, graphics);
            add(error);
            return;
        }

        List<TrackInfo> tracks = StreamEx.of(model.getTracks()).sorted().toList();

        int width = sequenceView.getBounds().width;
        int bottomTrackStartIndex = IntStreamEx.ofIndices( tracks, ti -> ti.getOrder() >= 0 ).findFirst().orElse( tracks.size() );
        BiConsumer<Integer, TrackInfo> trackCreator = (i, trackInfo) -> createTrackView( trackInfo, options, graphics, i, width, control );
        EntryStream.of( tracks ).limit( bottomTrackStartIndex ).forKeyValue( trackCreator );

        add(sequenceView, CompositeView.Y_BT, new Point(LEFT_TITLE_OFFSET, 0));

        EntryStream.of( tracks ).skip( bottomTrackStartIndex ).forKeyValue( trackCreator );

        // create Background of halftone strips
        double letterWidth = sequenceView.getNucleotideWidth(); //* mapOptions.getStripWidth();
        int letters = regionViewoptions.getSequenceViewOptions().getRulerOptions().getStep();

        insert(new MapBackgroundView(width, getBounds().height, letters, letterWidth, LEFT_TITLE_OFFSET, interval.getFrom()), 0);

        setModel(model);
        setActive(true);
    }

    protected void createTrackView(TrackInfo trackInfo, ViewOptions options, Graphics graphics, int i, int width, MapJobControl control)
    {
        if( trackInfo.isVisible() )
        {
            try
            {
                CompositeView trackView = null;
                SiteViewOptions siteViewOptions = options.getTrackViewOptions(trackInfo.getTrack().getCompletePath());
                DataCollection<Site> bottomSiteSet = trackInfo.getTrack().getSites(region.getSequenceName(), interval.getFrom(), interval.getTo());
                if( bottomSiteSet != null )
                {
                    trackView = trackInfo.getTrack().getViewBuilder().createTrackView(sequenceView, bottomSiteSet, siteViewOptions,
                            interval.getFrom(), interval.getTo(), SiteLayoutAlgorithm.BOTTOM, graphics, control);
                }

                if( trackView != null )
                {
                    trackView.insert(new TrackBackgroundView(trackView.getBounds(), width), 0);
                    add(trackView, CompositeView.Y_BT, new Point(LEFT_TITLE_OFFSET, ( i == 0 ) ? 0 : 10));
                    TextView trackName = new TextView(trackInfo.getTitle(), siteViewOptions.getTrackTitleFont(), graphics);
                    add(trackName, CompositeView.Y_BB, new Point(10, trackView.getBounds().height - trackName.getBounds().height));
                    trackName.setModel(trackInfo.getTrack());
                    trackName.setActive(true);
                    trackName.setSelectable(true);
                }
            }
            catch( Throwable e )
            {
                String msg = "Creating track view: " + trackInfo.getTrack().getName();
                log.log(Level.SEVERE, msg, e);

                View error = processError(msg, e, graphics);
                add(error, CompositeView.Y_BT);
            }
        }
    }
    
    /** Return map location in sequence (chromosome) */
    public Interval getRange()
    {
        return interval;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Selection issues
    //

    protected View selection;
    public boolean haveSelection()
    {
        return selection != null;
    }

    /**
     * Set specified view as selection
     * @param selection view to set as selection
     */
    public void setSelection(View selection)
    {
        //remove(this.selection);
        this.selection = selection;
        insert(selection, 1);
    }

    /**
     * Get current selection
     * @return view of current selection
     */
    public View getSelection()
    {
        return selection;
    }

    /**
     * Remove current selection (if exist)
     * @return true if selection was really removed
     */
    public boolean removeSelection()
    {
        return remove(selection);
    }
}
