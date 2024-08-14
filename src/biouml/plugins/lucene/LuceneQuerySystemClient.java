package biouml.plugins.lucene;

import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.JFrame;

import org.apache.lucene.queryparser.classic.ParseException;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Module;
import biouml.plugins.server.access.ClientDataCollection;
import biouml.plugins.server.access.ClientModule;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Request;
import ru.biosoft.util.DPSUtils;

/**
 *
 */
public class LuceneQuerySystemClient extends DefaultQuerySystem implements LuceneQuerySystem
{
    protected String host;
    protected String serverDCname;

    protected Request connection;

    protected ClientModule module = null;

    public LuceneQuerySystemClient(DataCollection dc) throws Exception
    {
        super(dc);

        module = dc.cast( ClientModule.class );
        ClientConnection conn = module.getClientConnection();
        connection = new Request(conn, log);

        Properties properties = dc.getInfo().getProperties();
        host = properties.getProperty(ClientConnection.URL_PROPERTY);
        serverDCname = properties.getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME);
        if( serverDCname == null )
            throw new DataElementReadException(dc, ClientDataCollection.SERVER_DATA_COLLECTION_NAME);
    }

    public String getHost()
    {
        return host;
    }

    @Override
    public Module getCollection()
    {
        return module;
    }

    @Override
    public void close()
    {
        if( connection != null )
            connection.close();
        super.close();
    }

    /**
     * Opens the connection with the server,
     * sends request, reads the answer,
     * check it, and close the connection.
     */
    public byte[] request(int command, Map<String, String> arguments, boolean readAnswer) throws BiosoftNetworkException
    {
        if( connection != null )
        {
            return connection.request(LuceneProtocol.LUCENE_SERVICE, command, arguments, readAnswer);
        }
        return null;
    }


    ///////////////////////////////////////////////////////////////////
    // LuceneQuerySystem implementation
    //

    private Boolean haveLuceneDir = null;
    @Override
    public boolean testHaveLuceneDir()
    {
        if( !module.isValid() )
        {
            haveLuceneDir = null;
            return false;
        }
        if( haveLuceneDir == null )
        {
            haveLuceneDir = false;
            Map<String, String> map = new HashMap<>();
            map.put(Connection.KEY_DC, serverDCname);
            try
            {
                request(LuceneProtocol.DB_TEST_HAVE_LUCENE_DATABASE, map, false);
                haveLuceneDir = true;
            }
            catch( BiosoftNetworkException e )
            {
                log.log(Level.SEVERE, module.getCompletePath()+": lucene search is not available: "+ExceptionRegistry.log(e));
            }
        }
        return haveLuceneDir;
    }

    @Override
    public String getIndexes(String relativeName)
    {
        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(Connection.KEY_DC, serverDCname);
            map.put(LuceneProtocol.KEY_NAME, relativeName);
            byte[] result = request(LuceneProtocol.DB_GET_INDEXES, map, true);
            if( result != null )
                return new String(result, "UTF-16BE");
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void setIndexes(String relativeName, String indexes) throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, serverDCname);
        map.put(LuceneProtocol.KEY_NAME, relativeName);
        if( indexes != null && indexes.trim().length() > 0 )
            map.put(LuceneProtocol.KEY_INDEXES, indexes);
        request(LuceneProtocol.DB_SEND_INDEXES, map, false);
    }

    @Override
    public boolean testHaveIndex() throws IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, serverDCname);
        request(LuceneProtocol.DB_TEST_HAVE_INDEX, map, false);
        return true;
    }

    @Override
    public Vector<String> getDCWithBuildIndex() throws IOException
    {
        Vector<String> v = new Vector<>();
        String result = "";

        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(Connection.KEY_DC, serverDCname);
            byte[] b = request(LuceneProtocol.DB_LIST_WITH_BUILD_INDEX, map, true);
            if( b != null )
                result = new String(b, "UTF-16BE");
        }
        catch( IOException e )
        {
            return v;
        }

        StringTokenizer tokens = new StringTokenizer(result, "\r\n");
        while( tokens.hasMoreTokens() )
        {
            v.add(tokens.nextToken());
        }

        return v;
    }

    @Override
    public void createIndex(Logger cat, JobControl jobControl) throws IOException, ParseException
    {
        if( cat != null )
            cat.info("Creating index start");
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, serverDCname);
        request(LuceneProtocol.DB_CREATE_INDEX, map, false);
        if( jobControl != null )
            jobControl.setPreparedness(100);
        if( cat != null )
            cat.info("Creating index finish");
    }

    @Override
    public void deleteIndex(Logger cat) throws IOException, ParseException
    {
        if( cat != null )
            cat.info("Delete index");
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, serverDCname);
        request(LuceneProtocol.DB_DELETE_INDEX, map, false);
    }

    @Override
    public void addToIndex(String relativeName, boolean deep, Logger cat, JobControl jobControl) throws IOException, ParseException
    {
        if( cat != null )
            cat.info("Add to index " + relativeName);
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, serverDCname);
        map.put(LuceneProtocol.KEY_NAME, relativeName);
        request(LuceneProtocol.DB_ADD_TO_INDEX, map, false);
        if( jobControl != null )
            jobControl.setPreparedness(100);
        if( cat != null )
            cat.info("Creating index finish");
    }

    @Override
    public void deleteFromIndex(String relativeName, Logger cat) throws IOException, ParseException
    {
        if( cat != null )
            cat.info("Delete from index " + relativeName);
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, serverDCname);
        map.put(LuceneProtocol.KEY_NAME, relativeName);
        request(LuceneProtocol.DB_DELETE_FROM_INDEX, map, false);
    }

    @Override
    public DynamicPropertySet[] search(String relativeName, String queryString, Formatter formatter, boolean alternativeView)
            throws IOException, ParseException, IntrospectionException
    {
        return search(relativeName, queryString, null, formatter, alternativeView);
    }

    @Override
    public DynamicPropertySet[] search(String relativeName, String queryString, String[] fields, Formatter formatter,
            boolean alternativeView) throws IOException, ParseException, IntrospectionException
    {
        return search(relativeName, queryString, fields, formatter, alternativeView, 0, MAX_DEFAULT_SEARCH_RESULTS_COUNT);
    }

    @Override
    public DynamicPropertySet[] search(String relativeName, String queryString, String[] fields, Formatter formatter,
            boolean alternativeView, int from, int to) throws IOException, ParseException, IntrospectionException
    {
        Map<String, String> map = new HashMap<>();

        map.put(Connection.KEY_DC, serverDCname);
        map.put(LuceneProtocol.KEY_NAME, relativeName);
        map.put(LuceneProtocol.KEY_QUERY, queryString);
        map.put(LuceneProtocol.KEY_VIEW, alternativeView ? "true" : "false");
        map.put(LuceneProtocol.KEY_FROM, String.valueOf(from));
        map.put(LuceneProtocol.KEY_TO, String.valueOf(to));

        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();

        if( fields != null && fields.length > 0 )
        {
            ObjectOutputStream oos = new ObjectOutputStream(baos1);
            for( String field : fields )
                oos.writeObject(field);
            oos.flush();
            map.put(LuceneProtocol.KEY_FIELDS, baos1.toString());
        }

        if( formatter != null )
        {
            map.put(LuceneProtocol.KEY_FORMATTER_PREFIX, formatter.getPrefix());
            map.put(LuceneProtocol.KEY_FORMATTER_POSTFIX, formatter.getPostfix());
        }

        byte[] result = request(LuceneProtocol.DB_LUCENE_SEARCH, map, true);

        if( result != null && result.length > 0 )
        {
            String str = new String(result, "UTF-16BE");
            if( !"null".equals(str) )
            {
                DynamicPropertySet[] dps = DPSUtils.loadDPSArray(new ByteArrayInputStream(result));
                if( dps != null && dps.length > 0 )
                    return dps;

                if( result.length > 1 )
                    throw new ParseException(str);
            }
        }

        return null;
    }

    @Override
    public DynamicPropertySet[] searchRecursive(String relativeName, String queryString, Formatter formatter, int from, int to)
            throws IOException, ParseException, IntrospectionException
    {
        Map<String, String> map = new HashMap<>();

        map.put(Connection.KEY_DC, serverDCname);
        map.put(LuceneProtocol.KEY_NAME, relativeName);
        map.put(LuceneProtocol.KEY_QUERY, queryString);
        map.put(LuceneProtocol.KEY_FROM, String.valueOf(from));
        map.put(LuceneProtocol.KEY_TO, String.valueOf(to));

        if( formatter != null )
        {
            map.put(LuceneProtocol.KEY_FORMATTER_PREFIX, formatter.getPrefix());
            map.put(LuceneProtocol.KEY_FORMATTER_POSTFIX, formatter.getPostfix());
        }

        byte[] result = request(LuceneProtocol.DB_LUCENE_SEARCH_RECURSIVE, map, true);

        if( result != null && result.length > 0 )
        {
            String str = new String(result, "UTF-16BE");
            if( !"null".equals(str) )
            {
                DynamicPropertySet[] dps = DPSUtils.loadDPSArray(new ByteArrayInputStream(result));
                if( dps != null && dps.length > 0 )
                    return dps;

                if( result.length > 1 )
                    throw new ParseException(str);
            }
        }

        return null;
    }

    @Override
    public Vector<String> getCollectionsNamesWithIndexes()
    {
        Vector<String> v = new Vector<>();
        String result = "";

        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(Connection.KEY_DC, serverDCname);
            map.put(LuceneProtocol.KEY_PROPERTY, LUCENE_INDEXES);
            byte[] b = request(LuceneProtocol.DB_LIST_WITH_PROPERTY, map, true);
            if( b != null )
                result = new String(b, "UTF-16BE");
        }
        catch( IOException e )
        {
            return v;
        }

        StringTokenizer tokens = new StringTokenizer(result, "\r\n");
        while( tokens.hasMoreTokens() )
        {
            v.add(tokens.nextToken());
        }

        return v;
    }

    @Override
    public DataElement getDataElement(String relativeName)
    {
        if( module != null )
            return CollectionFactory.getDataElement(relativeName, module);
        return null;
    }

    @Override
    public @Nonnull Vector<String> getPropertiesNames(String relativeName)
    {
        DataElement de = getDataElement(relativeName);
        if( de instanceof DataCollection )
            return LuceneUtils.getPropertiesNames((DataCollection<?>)de);
        return new Vector<>();
    }

    @Override
    public void showRebuildIndexesUI(JFrame parent) throws Exception
    {
    }

    @Override
    public Collection<String> getSuggestions(String term, String relativeName)
    {
        // TODO: implement
        return Collections.emptyList();
    }
}
