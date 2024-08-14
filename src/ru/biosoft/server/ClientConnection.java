package ru.biosoft.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to the server.
 * 
 * The application should use ConnectionPoll for effective management by open
 * connections.
 * <p>
 * 
 * @see Connection
 */
public abstract class ClientConnection
{
    public static final String URL_PROPERTY = "host";

    public static final String CONNECTION_TYPE = "connection-class";

    protected static final Logger log = Logger.getLogger(ClientConnection.class.getName());

    /** Port number used by connection. By default 10077. */
    protected int port = 10077;

    /** The Internet address of the server. */
    protected String host;

    /**
     * This field use by watch dog
     */
    private long lastUsage = System.currentTimeMillis();

    /**
     * This field use by watch dog
     */
    private volatile boolean isConnection = false;
    
    private String sessionId;

    /**
     * Internal flag
     */
    public int usageCount = 0;

    // //////////////////////////////////////
    // Public section
    //

    /**
     * Constructs the ClientConnection with for transfer data
     * 
     * @param host
     *            the Server address
     * @param port
     *            the Server port
     */
    public ClientConnection(String host, Integer port)
    {
        this.host = host;
        this.port = port.intValue();
    }

    /**
     * ATTENTION - this function should call only once time before sending of
     * request (only for obtaining of stream)
     * 
     * @return
     * @throws IOException
     */
    protected abstract InputStream getIs() throws IOException;

    /**
     * This function should be cold only by <code>sendArguments</code>
     * function
     */
    protected abstract OutputStream getOs() throws IOException;

    /**
     * Return host
     */
    public String getHost()
    {
        return host;
    }

    /**
     * Return port
     */
    public int getPort()
    {
        return port;
    }

    /**
     * By default return true
     */
    public boolean isMutable()
    {
        return true;
    }

    /**
     * This function send arguments to the server as simple serialized Map class
     */
    protected boolean sendArguments(Map arguments) throws IOException
    {
        OutputStream os = getOs();
        if( os != null )
        {
            try( ObjectOutputStream oos = new ObjectOutputStream( os ) )
            {
                oos.writeObject( new HashMap<>( arguments ) );
                oos.flush();
            }
            return true;
        }
        return false;
    }

    /**
     * This is symmetric function for function
     * <code>ClientConnection.sendArguments</code> but this function is static
     * so it can be used only for connection types, which not override
     * <code>sendArguments</code> function
     */
    public static Map getArguments(InputStream is) throws IOException
    {
        Map map = null;
        try
        {
            ObjectInputStream ois = new ObjectInputStream(is);
            map = (Map)ois.readObject();
        }
        catch( ClassNotFoundException e )
        {
            log.log(Level.SEVERE, "Get arguments exception", e);
            throw new IOException("Obtaining arguments exception " + e);
        }
        return map;
    }

    // //////////////////////////////////////
    // The connection open/close functions
    //

    /**
     * Return connection flag.
     */
    public synchronized boolean isConnected()
    {
        return isConnection;
    }

    /**
     * Opens the connection with the server. By default this function only set
     * connection flag.
     */
    public synchronized void connect() throws IOException
    {
        isConnection = true;
    }

    /**
     * Close the connection.
     * 
     * @throws IOException
     */
    public synchronized void disconnect() throws IOException
    {
        isConnection = false;
    }

    /**
     * Closes the connection in any case.
     */
    public synchronized void close()
    {
        try
        {
            disconnect();
        }
        catch( IOException e )
        {
            log.log(Level.INFO, "Disconnect error.", e);
        }
    }

    @Override
    public synchronized String toString()
    {
        return "ClientConnection " + host + ":" + port;
    }

    /**
     * This function set last usage, so if you use this connection - please call
     * this function (otherwise watch dog close this connection)
     */
    public final synchronized void setLastUsage()
    {
        lastUsage = System.currentTimeMillis();
    }

    public final synchronized long getLastUsage()
    {
        return lastUsage;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }
}
