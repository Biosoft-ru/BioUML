package ru.biosoft.bsa.server;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.access.SitesTableCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.table.access.TableResolver;

public class TrackTableResolver extends TableResolver
{
    public TrackTableResolver(BiosoftWebRequest arguments)
    {

    }
    
    @Override
    public int accept(DataElement de) throws Exception
    {
        if(de instanceof Track)
            return 1;
        return 0;
    }
    
    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        Track track = de.cast( Track.class );
        return new SitesTableCollection( track, track.getAllSites() );
    }
}