package ru.biosoft.bsa.finder;

import ru.biosoft.access.CacheableBeanProvider;
import ru.biosoft.access.core.DataElementPath;

public class TrackFinderBeanProvider implements CacheableBeanProvider
{
    @Override
    public Object getBean(String parameters)
    {
        String[] parts = parameters.split( "/", 2 );
        String databaseName = parts[0];
        String genomePath = parts[1];
        DataElementPath databasePath = DataElementPath.create( "databases", databaseName );
        TrackFinder trackFinder = TrackFinderRegistry.instance.createTrackFinder( databasePath );
        trackFinder.setGenome( DataElementPath.create( genomePath ) );
        return trackFinder;
    }
}
