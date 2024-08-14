package ru.biosoft.bsa.view;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Option;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.project.Region;

/**
 * Collect view options for regions and tracks
 */
public class ViewOptions implements PropertyChangeListener
{
    protected PropertyChangeListener changeListener;
    public void setPropertyChangeListener(PropertyChangeListener changeListener)
    {
        removePropertyChangeListener();
        this.changeListener = changeListener;
        addPropertyChangeListener();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
    }

    protected MapViewOptions regionViewOptions = new MapViewOptions();

    /**
     * Get view options by region
     */
    public MapViewOptions getRegionViewOptions()
    {
        return regionViewOptions;
    }

    /**
     * Put view options for region
     */
    public void addRegionViewOptions(Region region, MapViewOptions mapViewOptions)
    {
        this.regionViewOptions = mapViewOptions;
    }

    protected Map<ru.biosoft.access.core.DataElementPath, SiteViewOptions> trackViewOptions = new HashMap<>();

    public SiteViewOptions getTrackViewOptions(DataElementPath trackPath)
    {
        return trackViewOptions.computeIfAbsent( trackPath, path -> {
            Track track = path.getDataElement( Track.class );
            SiteViewOptions result = track.getViewBuilder().createViewOptions();
            result.addPropertyChangeListener( changeListener );
            return result;
        } );
    }
    

    /**
     * Semantic zoom
     */
    public void semanticZoom(float coef)
    {
        Graphics graphics = ApplicationUtils.getGraphics();
        zoomRegionViewOptions(regionViewOptions, coef, graphics);
    }

    public double semanticZoomSet(double density)
    {
        Graphics graphics = ApplicationUtils.getGraphics();
        density = setRegionDensity(regionViewOptions, density, graphics);
        return density;
    }

    protected void zoomRegionViewOptions(MapViewOptions viewOption, float coef, Graphics g)
    {
        //process density
        double oldDensity = viewOption.getSequenceViewOptions().getDensity();
        double newDensity = (float) ( oldDensity * Math.pow(2.0, coef) );

        setRegionDensity(viewOption, newDensity, g);
    }

    private double setRegionDensity(MapViewOptions viewOption, double newDensity, Graphics g)
    {
        if( newDensity < 0.000001f )
            newDensity = 0.000001f;

        //process ruler type
        FontMetrics fontMetrics = g.getFontMetrics(viewOption.getSequenceViewOptions().getFont().getFont());
        int letterWidth = fontMetrics.stringWidth("a");
        if( newDensity >= letterWidth )
        {
            viewOption.getSequenceViewOptions().setType(SequenceViewOptions.PT_BOTH);
            newDensity = letterWidth;
        }
        else
        {
            viewOption.getSequenceViewOptions().setType(SequenceViewOptions.PT_RULER);
        }
        viewOption.getSequenceViewOptions().setDensity(newDensity);
        return newDensity;
    }

    /**
     * Put view options for track
     */
    public void addTrackViewOptions(DataElementPath trackPath, SiteViewOptions trackOptions)
    {
        trackViewOptions.put(trackPath, trackOptions);
    }

    /**
     * Remove PropertyChangeListener from all view options
     */
    protected void removePropertyChangeListener()
    {
        regionViewOptions.removePropertyChangeListener(changeListener);
        for(Option option: trackViewOptions.values())
        {
            option.removePropertyChangeListener(changeListener);
        }
    }

    /**
     * Add PropertyChangeListener to all view options
     */
    protected void addPropertyChangeListener()
    {
        regionViewOptions.addPropertyChangeListener(changeListener);
        for(Option option: trackViewOptions.values())
        {
            option.addPropertyChangeListener(changeListener);
        }
    }
}
