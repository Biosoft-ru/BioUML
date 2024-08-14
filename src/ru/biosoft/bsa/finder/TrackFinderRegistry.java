package ru.biosoft.bsa.finder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class TrackFinderRegistry
{
    private static final Logger log = Logger.getLogger( TrackFinderRegistry.class.getName() );

    public static final TrackFinderRegistry instance = new TrackFinderRegistry();

    private Map<ru.biosoft.access.core.DataElementPath, Class<? extends TrackFinder>> classesByDatabase = new HashMap<>();
    private Map<ru.biosoft.access.core.DataElementPath, List<ru.biosoft.access.core.DataElementPath>> databasesByGenome = new HashMap<>();

    private TrackFinderRegistry()
    {
        for(DataCollection<?> db : CollectionFactoryUtils.getDatabases())
        {
            if(db.getInfo().getProperty( "trackFinder" ) == null)
                continue;
            try
            {
                Class<? extends TrackFinder> trackFinderClass = db.getInfo().getPropertyClass( "trackFinder", TrackFinder.class );
                classesByDatabase.put(db.getCompletePath(), trackFinderClass);
                TrackFinder trackFinder = createTrackFinder( trackFinderClass, db.getCompletePath() );
                for(DataElementPath genome : trackFinder.getSupportedGenomes())
                    databasesByGenome
                        .computeIfAbsent( genome, k->new ArrayList<>() )
                        .add( db.getCompletePath() );
                        
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Can not init trackFinder for " + db.getCompletePath(), e );
            }
        }
    }

    private TrackFinder createTrackFinder(Class<? extends TrackFinder> clazz, DataElementPath database) throws Exception
    {
        return clazz.getConstructor( DataElementPath.class ).newInstance( database );
    }

    public List<ru.biosoft.access.core.DataElementPath> getDatabasesForGenome(DataElementPath genome)
    {
        List<ru.biosoft.access.core.DataElementPath> result = databasesByGenome.get( genome );
        if(result == null)
            return Collections.emptyList();
        return result;
    }

    public TrackFinder createTrackFinder(DataElementPath database)
    {
        Class<? extends TrackFinder> clazz = classesByDatabase.get( database );
        if( clazz == null )
            return null;

        try
        {
            return createTrackFinder( clazz, database );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Can not create trackFinder for " + database, e );
        }
        return null;
    }
}
