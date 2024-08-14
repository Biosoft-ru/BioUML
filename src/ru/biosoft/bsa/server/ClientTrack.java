package ru.biosoft.bsa.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.server.access.ClientDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SlicedTrack;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.ConnectionPool;
import ru.biosoft.server.Request;

/**
 * Client track element
 * TODO: support getSite query
 */
public class ClientTrack extends SlicedTrack
{
    protected static final Logger log = Logger.getLogger(ClientTrack.class.getName());

    protected String pathOnServer;
    protected BSAClient connection;

    public ClientTrack(DataCollection<?> origin, String name) throws Exception
    {
        // TODO: receive optimal slice length from the server
        super(name, origin, 1000000);

        ClientConnection conn = ConnectionPool.getConnection(this);
        connection = new BSAClient(new Request(conn, log), log);

        if( origin.getInfo() != null && origin.getInfo().getProperties() != null
                && origin.getInfo().getProperties().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME) != null )
        {
            pathOnServer = origin.getInfo().getProperties().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME);
        }
        else
        {
            pathOnServer = origin.getCompletePath().toString();
        }
        pathOnServer += ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(name);
    }

    @Override
    protected Collection<Site> loadSlice(String sequence, Interval interval)
    {
        try
        {
            Sequence sequenceElement = ( (ru.biosoft.bsa.AnnotatedSequence)CollectionFactory.getDataElement(sequence) ).getSequence();
            return Arrays.asList(connection.loadSlice(pathOnServer, sequenceElement, getPathOnServer(sequence), interval));
        }
        catch( Exception e )
        {
            throw new RuntimeException("Unable to load sites", e);
        }
    }

    @Override
    protected int countSitesLimited(String sequence, Interval interval, int limit)
    {
        try
        {
            return connection.calculateSiteCount(pathOnServer, getPathOnServer(sequence), interval);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not load slice from server", e);
        }
        return -1;
    }

    protected String getPathOnServer(String clientPath)
    {
        String currentModuleClientName = getModuleSubstring(getOrigin().getCompletePath().toString());
        if( currentModuleClientName.equals(getModuleSubstring(clientPath)) )
        {
            return clientPath.replaceFirst(currentModuleClientName, getModuleSubstring(pathOnServer));
        }
        return clientPath;
    }

    private String getModuleSubstring(String path)
    {
        String[] pathComponents = DataElementPath.create(path).getPathComponents();
        return DataElementPath.create(pathComponents[0]).getChildPath(pathComponents[1]).toString();
    }

    public void setViewBuilder(TrackViewBuilder viewBuilder)
    {
        this.viewBuilder = viewBuilder;
    }
    
    public void setSliceLength(int sliceLength)
    {
        this.sliceLength = sliceLength;
    }
}
