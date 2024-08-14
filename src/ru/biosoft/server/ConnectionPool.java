package ru.biosoft.server;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import biouml.plugins.server.access.ClientDataCollection;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.core.DataElementReadException;

/**
 * Simple implementation of connection pool.
 * 
 * Only one connection is used for some host:port.
 * 
 * Background thread close connections by time out.
 */
public class ConnectionPool
{
    protected static final int TIME_OUT = 3000000; // ms

    protected static final Logger log = Logger.getLogger(ConnectionPool.class.getName());

    private static final Map<String, ClientConnection> connections;

    public static synchronized ClientConnection getConnection(Class<? extends ClientConnection> connectionClass, String host, int port) throws Exception
    {
        return getConnection(connectionClass, host, port, false);
    }

    /**
     * Get connections from DataCollection properties:
     * 
     * <ul>
     * <li>ClientConnection.URL_PROPERTY - URL for server, obligatory</li> 
     * <li>ClientConnection.CONNECTION_TYPE - class name for client connection instance,
     * By default - ru.biosoft.server.tomcat.TomcatConnection </li>
     * <li>ClientConnection.PLUGINS_PROPERTY - id of plug-in needed for class name for client connection instance.</li>
     * 
     * @param properties
     * @return
     * @throws Exception
     */
    public static synchronized ClientConnection getConnection(Properties properties) throws Exception
    {
    	ru.biosoft.access.core.DataElementPath path = DataElementPath.create(properties.getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME));
    	
    	String url = properties.getProperty(ClientConnection.URL_PROPERTY);
    	if( url == null )
    		throw new DataElementReadException(path, ClientConnection.URL_PROPERTY);
    
    	String connectionClassName = properties.getProperty(ClientConnection.CONNECTION_TYPE);
    	if( connectionClassName == null )
    		connectionClassName = "ru.biosoft.server.tomcat.TomcatConnection";
    	
    	String pluginNames = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
    	ClientConnection conn;
    	try
    	{
    		Class<? extends ClientConnection> connectionClass;
  
    		if( pluginNames == null )
    			connectionClass = (Class<? extends ClientConnection>) Class.forName(connectionClassName);
    		else 
    			connectionClass = ClassLoading.loadSubClass( connectionClassName, pluginNames, ClientConnection.class );
    		
    		conn = getConnection(connectionClass, url);
    	}
    	catch( Exception e )
    	{
    		throw new BiosoftNetworkException(e, url);
    	}
    
    	return conn;
    }
    
    /**
     * Create new client connection or return existing connection
     * 
     * @param connectionClass
     * @param host
     * @param port
     * @param getExclusive - expert parameter - this connection
     *          wont be managed by watch dog and client must close
     *          this connection
     * @return
     * @throws Exception
     */
    public static synchronized ClientConnection getConnection(Class<? extends ClientConnection> connectionClass, String host, int port, boolean getExclusive)
            throws Exception
    {
        String key = connectionClass.getName() + "-" + host + ":" + port;

        if( !getExclusive )
        {
            if( connections.containsKey(key) )
                return connections.get(key);
        }
        ClientConnection connection = connectionClass.getConstructor(String.class, Integer.class).newInstance(host, port);
        if( !getExclusive )
        {
            connections.put(key, connection);
        }
        return connection;
    }

    public static synchronized @Nonnull ClientConnection getConnection(Class<? extends ClientConnection> connectionClass, String url) throws Exception
    {
        return getConnection(connectionClass, url, false);
    }

    /**
     * Create new client connection or return existing connection
     * 
     * @param connectionClass
     * @param url
     * @param getExclusive - expert parameter - this connection
     *          wont be managed by watch dog and client must close
     *          this connection
     * @return
     * @throws Exception
     */
    public static synchronized @Nonnull ClientConnection getConnection(Class<? extends ClientConnection> connectionClass, String url, boolean getExclusive) throws Exception
    {
        String key = connectionClass.getName() + "-" + url;
        ClientConnection connection;
        if( !getExclusive )
        {
            connection = connections.get(key);
            if( connection != null )
                return connection;
        }
        Constructor<? extends ClientConnection> c = connectionClass.getConstructor(String.class);
        connection = c.newInstance(url);
        if( !getExclusive )
        {
            connections.put(key, connection);
        }
        return connection;
    }
    
    public static @Nonnull ClientConnection getConnection(DataElement element) throws LoggedException
    {
        DataElement connectionElement = element;
        while(connectionElement != null && !(connectionElement instanceof ClientConnectionHolder))
            connectionElement = connectionElement.getOrigin();
        if(connectionElement != null)
            return ((ClientConnectionHolder)connectionElement).getClientConnection();
        throw new DataElementReadException(element, "client connection");
    }

    protected static class WatchDog implements Runnable
    {
        @Override
        public void run()
        {
            while( true )
            {
                try
                {
                    Thread.sleep(TIME_OUT);

                    Iterator<String> i = connections.keySet().iterator();
                    while( i.hasNext() )
                    {
                        String key = i.next();
                        ClientConnection connection = connections.get(key);
                        synchronized( connection )
                        {
                            if( connection.isConnected() && System.currentTimeMillis() - connection.getLastUsage() > TIME_OUT )
                            {
                                log.info("Close connection by time out, connection=" + connection);
                                connection.close();
                                if( connection.usageCount == 0 )
                                {
                                    connections.remove(key);
                                    i = connections.keySet().iterator();
                                }
                            }
                        }
                    }
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Connection pool, watch dog error: " + t, t);
                }
            }
        }
    }

    // static initialisation
    static
    {
        connections = new HashMap<>();
        new Thread(new WatchDog(), "ClientConnectionsWatchDog").start();
    }

}