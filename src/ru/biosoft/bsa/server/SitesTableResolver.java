package ru.biosoft.bsa.server;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.view.SitesViewPart;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.table.access.TableResolver;

/**
 * Helper class to respond create tables associated with ru.biosoft.bsa.Map element
 */
public class SitesTableResolver extends TableResolver
{
    protected int from;
    protected int to;
    protected DataElementPath trackPath;

    public SitesTableResolver(BiosoftWebRequest arguments) throws WebException
    {
        from = arguments.optInt( "from" );
        to = arguments.optInt( "to" );
        trackPath = arguments.getDataElementPath( "track" );
    }
    public SitesTableResolver(int from, int to, DataElementPath trackPath)
    {
        this.from = from;
        this.to = to;
        this.trackPath = trackPath;
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        de.cast( AnnotatedSequence.class );
        Track track = trackPath.getDataElement( Track.class );
        return SitesViewPart.getSitesCollection( track, DataElementPath.create( de ).toString(), from, to );
    }
}