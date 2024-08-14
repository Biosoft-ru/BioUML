package biouml.plugins.server.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Index;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Response;
import ru.biosoft.server.SynchronizedServiceSupport;

public class QuerySystemService extends SynchronizedServiceSupport
{
    protected Response connection;
    protected Map arguments;

    private DataCollection dc;

    private Index index;

    /**
     * Set up the specified data collection.<p>
     *
     * If such data collection is not loaded on the Server
     * the error message will be sent to the client.
     */
    protected boolean setDataCollection() throws IOException
    {
        Object dcName = arguments.get(ClientQuerySystem.KEY_DC);
        if( dcName == null )
        {
            connection.error("didn't send data collection complete name");
            return false;
        }
        DataElementPath path = DataElementPath.create(dcName.toString());

        if( dc == null || !dc.getCompletePath().equals(path) )
        {
            dc = path.optDataCollection();
            if( dc == null )
            {
                connection.error("Can not find data collection, complete name=" + path + ".");
                return false;
            }
        }

        return true;
    }

    protected boolean setIndex() throws IOException
    {
        if( setDataCollection() )
        {
            Object index = arguments.get(ClientQuerySystem.KEY_INDEX);
            if( index == null )
            {
                connection.error("didn't send index name");
                return false;
            }

            if( dc.getInfo().getQuerySystem() == null )
            {
                connection.error("There is no indexes");
                return false;
            }

            this.index = dc.getInfo().getQuerySystem().getIndex(index.toString());
            if( this.index == null )
            {
                connection.error("invalid index name " + index.toString());
                return false;
            }
            return true;
        }
        return false;
    }

    //////////////////////////////////////////////
    // Protocol implementation functions
    //

    @Override
    protected boolean processRequest(int command) throws Exception
    {
        connection = getSessionConnection();
        arguments = getSessionArguments();

        switch( command )
        {
            case ClientQuerySystem.DB_SET_DC:
                setDataCollection();
                break;

            case ClientQuerySystem.DB_GET_INDEXES:
                sendIndexes();
                break;
            case ClientQuerySystem.DB_CLOSE:
                close();
                break;

            case ClientQuerySystem.DB_INDEX_CLOSE:
                sendIndexClose();
                break;
            case ClientQuerySystem.DB_INDEX_CHECK_VALID:
                sendIndexCheck();
                break;
            case ClientQuerySystem.DB_INDEX_GET_SIZE:
                sendIndexSize();
                break;
            case ClientQuerySystem.DB_INDEX_GET_ALL:
                sendIndexData();
                break;
            case ClientQuerySystem.DB_INDEX_PUT:
                doIndexPut();
                break;
            case ClientQuerySystem.DB_INDEX_REMOVE:
                doIndexRemove();
                break;
            case ClientQuerySystem.DB_INDEX_PUT_ALL:
                doIndexPutAll();
                break;
            case ClientQuerySystem.DB_INDEX_CLEAR:
                doIndexClear();
                break;

            default:
                return false;
        }
        return true;
    }

    public void sendIndexes() throws IOException
    {
        if( setDataCollection() )
        {
            if( dc.getInfo().getQuerySystem() != null )
            {
                Index[] indexes = dc.getInfo().getQuerySystem().getIndexes();
                if( indexes != null && indexes.length > 0 )
                {
                    StringBuffer buffer = new StringBuffer(indexes[0].getName());
                    for( int i = 1; i < indexes.length; i++ )
                    {
                        buffer.append("\n");
                        buffer.append(indexes[i].getName());
                    }
                    //System.out.println("Indexes is: " + buffer);
                    connection.send(buffer.toString().getBytes("UTF-16BE"), Connection.FORMAT_GZIP);
                    return;
                }
            }
            //System.out.println("Indexes is empty");
            connection.send("".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    public void close() throws Exception
    {
        if( setDataCollection() )
        {
            // TODO - validate side-effects
            //if (dc.getInfo().getQuerySystem() != null)
            //    dc.getInfo().getQuerySystem().close();
            connection.send("".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    public void sendIndexClose() throws Exception
    {
        if( setIndex() )
        {
            index.close();
            connection.send("".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    public void sendIndexCheck() throws IOException
    {
        if( setIndex() )
        {
            if( index.isValid() )
                connection.send("true".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
            else
                connection.send("false".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    public void sendIndexSize() throws IOException
    {
        if( setIndex() )
        {
            connection.send(Integer.toString(index.size()).getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    public static class Pair implements Serializable
    {
        public String key;
        public String value;
        public Pair(String key, String value)
        {
            this.key = key;
            this.value = value;
        }
    }

    public void sendIndexData() throws IOException
    {
        if( setIndex() )
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            for( Object key : index.keySet() )
            {
                if( key instanceof Serializable )
                {
                    Object value = index.get(key);
                    if( value instanceof Serializable )
                        oos.writeObject(new Pair(key.toString(), value.toString()));
                }
            }
            oos.flush();
            connection.send(baos.toByteArray(), Connection.FORMAT_GZIP);
            return;
        }
    }

    public void doIndexPut() throws IOException
    {
        if( setIndex() )
        {
            Object key = arguments.get(ClientQuerySystem.KEY_KEY);
            if( key == null )
            {
                connection.error("didn't send key");
                return;
            }
            Object value = arguments.get(ClientQuerySystem.KEY_KEY);
            if( value == null )
            {
                connection.error("didn't send value");
                return;
            }
            Object o = index.put(key, value);
            if( o != null )
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(o);
                oos.flush();
                connection.send(baos.toByteArray(), Connection.FORMAT_GZIP);
                return;
            }
            connection.send("null".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    public void doIndexRemove() throws IOException
    {
        if( setIndex() )
        {
            Object key = arguments.get(ClientQuerySystem.KEY_KEY);
            if( key == null )
            {
                connection.error("didn't send key");
                return;
            }
            Object o = index.remove(key);
            if( o != null )
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(o);
                oos.flush();
                connection.send(baos.toByteArray(), Connection.FORMAT_GZIP);
                return;
            }
            connection.send("null".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    public void doIndexPutAll() throws IOException, ClassNotFoundException
    {
        if( setIndex() )
        {
            Object data = arguments.get(ClientQuerySystem.KEY_DATA);
            if( data == null )
            {
                connection.error("didn't send data");
                return;
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(data.toString().getBytes());
            ObjectInputStream ois = new ObjectInputStream(bais);
            index.putAll((Map)ois.readObject());
            connection.send(null, Connection.FORMAT_SIMPLE);
        }
    }

    public void doIndexClear() throws IOException
    {
        if( setIndex() )
        {
            index.clear();
            connection.send(null, Connection.FORMAT_SIMPLE);
        }
    }

}
