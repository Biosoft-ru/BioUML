package biouml.plugins.server.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import one.util.streamex.StreamEx;

import biouml.model.Module;
import biouml.plugins.server.access.QuerySystemService.Pair;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Request;

/**
 *
 */
public class ClientQuerySystemSupport extends DefaultQuerySystem implements ClientQuerySystem
{

    protected Request connection;

    private final String serverDCname;
    private boolean indexesWasLoaded = false;

    /**
     * Internal index map - using for all operation
     * with indexes
     */
    protected Map<String, Index> indexesMap = new HashMap<>();
    /**
     * Primary data collection
     */
    protected DataCollection dc;

    /**
     * Implementation of ClientQuerySystem interface
     *
     * @param dc
     * @throws Exception
     */
    public ClientQuerySystemSupport(DataCollection dc) throws Exception
    {
        super(dc);

        this.dc = dc;

        Properties properties = dc.getInfo().getProperties();
        serverDCname = properties.getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME);
        if( serverDCname == null )
            throw new DataElementReadException(dc, ClientDataCollection.SERVER_DATA_COLLECTION_NAME);
        ClientConnection conn = Module.getModule(dc).cast( ClientModule.class ).getClientConnection();

        connection = new Request(conn, log);
    }

    ////////////////////////////////////////
    // Request functions
    //

    /**
     * Opens the connection with the server,
     * sends request, reads the answer,
     * check it, and close the connection.
     *
     * @param command  request command (cod)
     * @param argument request argument
     *
     * @see Connection
     */
    public byte[] request(int command, Map<String, String> arguments, boolean readAnswer) throws BiosoftNetworkException
    {
        if( connection != null )
        {
            return connection.request(ClientQuerySystem.QUERY_SERVICE, command, arguments, readAnswer);
        }
        return null;
    }

    /**
     * Base function for all obtaining operations
     */
    @Override
    public Index[] getIndexes()
    {
        try
        {
            if( !indexesWasLoaded )
            {
                List<String> names = new ArrayList<>();
                byte[] data = request(DB_GET_INDEXES, Collections.singletonMap(KEY_DC, serverDCname), true);
                String result = "";
                if( data != null )
                    result = new String(data, "UTF-16BE");
                //System.out.println("Indexes is: " + result);
                StringTokenizer tokenizer = new StringTokenizer(result, "\r\n");
                if( tokenizer.hasMoreTokens() )
                {
                    String name = tokenizer.nextToken();
                    if( super.getIndex(name) == null )
                        names.add(name);
                }

                StreamEx.of( names ).mapToEntry( name -> new ClientIndex( dc, name ) ).forKeyValue( indexesMap::put );
                indexesWasLoaded = true;
            }
        }
        catch( IOException e1 )
        {
            log.log(Level.SEVERE, "Cannot get indexes ", e1);
        }

        return indexesMap.values().toArray(new Index[indexesMap.size()]);
    }

    /**
     * This function first of all call <code>getIndexes()</code>
     * id then return requested index
     */
    @Override
    public Index getIndex(String name)
    {
        if( indexes == null )
            getIndexes();
        return indexesMap.get(name);
    }

    @Override
    public void close()
    {
        try
        {
            request(DB_CLOSE, Collections.singletonMap(KEY_DC, serverDCname), false);
            connection.close();
            super.close();
        }
        catch( BiosoftNetworkException e )
        {
        }
    }

    @Override
    public void closeIndex(String name) throws IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_DC, serverDCname);
        map.put(KEY_INDEX, name);
        request(DB_INDEX_CLOSE, map, false);
    }

    @Override
    public boolean checkValidIndex(String name) throws IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_DC, serverDCname);
        map.put(KEY_INDEX, name);
        byte[] result = request(DB_INDEX_CHECK_VALID, map, true);
        if( result != null )
            if( "true".equals(new String(result, "UTF-16BE")) )
                return true;
        return false;
    }

    @Override
    public int getIndexSize(String name) throws IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_DC, serverDCname);
        map.put(KEY_INDEX, name);
        try
        {
            byte[] result = request(DB_INDEX_GET_SIZE, map, true);
            if( result != null )
                return Integer.parseInt(new String(result, "UTF-16BE"));
        }
        catch( NumberFormatException e )
        {
        }
        return 0;
    }

    @Override
    public Map indexGet(String name) throws IOException, ClassNotFoundException
    {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_DC, serverDCname);
        map.put(KEY_INDEX, name);
        byte[] result = request(DB_INDEX_GET_ALL, map, true);
        //return (Map)ois.readObject();
        Map data = new HashMap();
        try
        {
            if( result != null )
            {
                ByteArrayInputStream bais = new ByteArrayInputStream(result);
                ObjectInputStream ois = new ObjectInputStream(bais);
                while( true )
                {
                    QuerySystemService.Pair pair = (Pair)ois.readObject();
                    data.put(pair.key, pair.value);
                }
            }
        }
        catch( Throwable t )
        {
        }
        return data;
    }

    @Override
    public Object indexPut(String name, Object key, Object value) throws IOException, ClassNotFoundException
    {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_DC, serverDCname);
        map.put(KEY_INDEX, name);
        map.put(KEY_KEY, key.toString());
        map.put(KEY_VALUE, value.toString());
        byte[] result = request(DB_INDEX_PUT, map, true);
        if( result != null && !"null".equals(new String(result, "UTF-16BE")) )
        {
            ByteArrayInputStream bais = new ByteArrayInputStream(result);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        }
        return null;
    }

    @Override
    public Object indexRemove(String name, Object key) throws IOException, ClassNotFoundException
    {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_DC, serverDCname);
        map.put(KEY_INDEX, name);
        map.put(KEY_KEY, key.toString());
        byte[] result = request(DB_INDEX_REMOVE, map, true);
        if( result != null && !"null".equals(new String(result, "UTF-16BE")) )
        {
            ByteArrayInputStream bais = new ByteArrayInputStream(result);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        }
        return null;
    }

    @Override
    public void indexPutAll(String name, Map put) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(new HashMap(put));
        oos.flush();
        Map<String, String> map = new HashMap<>();
        map.put(KEY_DC, serverDCname);
        map.put(KEY_INDEX, name);
        map.put(KEY_DATA, baos.toString());
        request(DB_INDEX_PUT_ALL, map, false);
    }

    @Override
    public void indexClear(String name) throws IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_DC, serverDCname);
        map.put(KEY_INDEX, name);
        request(DB_INDEX_CLEAR, map, false);
    }

}
