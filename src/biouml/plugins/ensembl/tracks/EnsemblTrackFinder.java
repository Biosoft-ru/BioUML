package biouml.plugins.ensembl.tracks;

import java.util.Collections;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.finder.TrackFinder;
import ru.biosoft.bsa.finder.TrackSummary;

public class EnsemblTrackFinder extends TrackFinder
{
    public EnsemblTrackFinder(DataElementPath databasePath)
    {
        super( databasePath );
    }

    @Override
    public List<ru.biosoft.access.core.DataElementPath> getSupportedGenomes()
    {
        DataElementPath genome = TrackUtils.getPrimarySequencesPath( databasePath );
        return Collections.singletonList( genome );
    }

    @Override
    public DataCollection<? extends TrackSummary> findTracks()
    {
        DataCollection<TrackSummary> result = new VectorDataCollection<>( "" );
        for(Track t : databasePath.getChildPath("Tracks").getDataCollection(Track.class))
        {
            TrackSummary ts = new TrackSummary( t );
            //ts.setSize(fetchSiteCount( t ));//Takes too long time
            result.put( ts );
        }
        return result;
    }

    private int fetchSiteCount(Track t)
    {
        int size = 0;
        for(DataElementPath chrPath : genome.getChildren())
        {
            int length = chrPath.getDataElement( AnnotatedSequence.class ).getSequence().getLength();
            size += t.getSites( chrPath.toString(), 0, length ).getSize();
        }
        return size;
    }

}
