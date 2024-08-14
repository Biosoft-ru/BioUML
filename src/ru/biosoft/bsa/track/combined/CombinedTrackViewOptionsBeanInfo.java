package ru.biosoft.bsa.track.combined;


import ru.biosoft.bsa.view.SiteViewOptionsBeanInfo;

public class CombinedTrackViewOptionsBeanInfo extends SiteViewOptionsBeanInfo
{
    public CombinedTrackViewOptionsBeanInfo()
    {
        super( CombinedTrackViewOptions.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "boxHeight" );
        add( "showPositions" );
        add( "showStrand" );
        addWithTags( "visualStyle", CombinedTrackViewOptions.VIZ_STYLES );
        add( "statisticsOn" );
        add( "colorScheme" );
    }
}