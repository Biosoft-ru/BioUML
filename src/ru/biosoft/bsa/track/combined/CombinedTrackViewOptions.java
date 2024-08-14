package ru.biosoft.bsa.track.combined;

import java.util.Collections;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.view.SiteViewOptions;

public class CombinedTrackViewOptions extends SiteViewOptions
{
    public static final String COLOR_OVERLAY = "Color overlay";
    public static final String COLOR_SHIFT = "Color shift";
    public static final String[] VIZ_STYLES = {COLOR_OVERLAY, COLOR_SHIFT, "Coverage"};

    private String visualStyle;
    private boolean statisticsOn = false;
    private DataElementPath trackPath;


    public CombinedTrackViewOptions()
    {
        super();
        this.showBox = true;
        visualStyle = COLOR_SHIFT;
        CombinedColorScheme colorScheme = new CombinedColorScheme();
        schemes = Collections.singletonMap( colorScheme.getName(), colorScheme );
        super.setColorScheme( colorScheme );
        setViewTagger( model -> {
            if( ! ( model instanceof Site ) )
                return null;
            Site site = (Site)model;
            if( site.getProperties().getValue( "OriginalTrack" ) != null )
                return site.getProperties().getValueAsString( "OriginalTrack" );
            else
                return null;
        } );
    }

    @PropertyName ( "Visualization style" )
    public String getVisualStyle()
    {
        return visualStyle;
    }
    public void setVisualStyle(String visualStyle)
    {
        Object oldValue = this.visualStyle;
        this.visualStyle = visualStyle;
        firePropertyChange( "visualStyle", oldValue, visualStyle );
    }

    @PropertyName ( "Show statistics" )
    public boolean isStatisticsOn()
    {
        return statisticsOn;
    }
    public void setStatisticsOn(boolean statisticsOn)
    {
        Object oldValue = this.statisticsOn;
        this.statisticsOn = statisticsOn;
        firePropertyChange( "statisticsOn", oldValue, statisticsOn );
    }

    @Override
    public void initFromTrack(Track track)
    {
        if( ! ( track instanceof CombinedTrack ) )
            return;
        this.trackPath = track.getCompletePath();
        ( (CombinedColorScheme)getColorScheme() ).setTrackPath( trackPath );
    }
}