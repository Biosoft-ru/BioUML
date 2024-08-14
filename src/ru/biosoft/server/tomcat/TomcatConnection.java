package ru.biosoft.server.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import ru.biosoft.server.ClientConnection;

/**
 * This web connection uses POST request
 */
public class TomcatConnection extends ClientConnection
{
    private final static String CONTEXT = "/biouml/";

    public static final String CONNECTION_TIMEOUT_PROPERTY = "biouml.client.timeout";

    private URL url;

    private URLConnection conn;
    
    /**
     * @param host
     * @param port
     * @throws MalformedURLException
     */
    public TomcatConnection(String host, Integer port) throws MalformedURLException
    {
        super(host, port);
        initURL();
    }

    /**
     * @param url
     * @throws MalformedURLException
     */
    public TomcatConnection(String url) throws MalformedURLException
    {
        super(url, 0);
        initURL();
    }

    /**
     * @throws MalformedURLException 
     * 
     */
    private void initURL() throws MalformedURLException
    {
        String urlString = host;
        if(!host.startsWith("http://") && !host.startsWith("https://"))
        {
            if( port == 0 )
            {
                //using URL string
                urlString = "http://" + host;
            }
            else
            {
                //using host and port
                urlString = "http://" + host + ":"+port + CONTEXT;
            }
        }
        if(!urlString.endsWith("/"))
            urlString += "/";
        url = new URL(urlString);
    }

    @Override
    protected synchronized OutputStream getOs() throws IOException
    {
        if( conn == null )
            connect();
        return conn.getOutputStream();
    }

    @Override
    protected synchronized InputStream getIs() throws IOException
    {
        if( conn == null )
            connect();
        return conn.getInputStream();
    }

    /**
     * Opens the connection with the server.
     */
    @Override
    public synchronized void connect() throws IOException
    {
        if( isConnected() )
            return;

        conn = url.openConnection();
        int timeout = Integer.parseInt(System.getProperty(CONNECTION_TIMEOUT_PROPERTY, "10000"));
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        super.connect();
    }

    @Override
    public synchronized String toString()
    {
        return "Web connection: " + url;
    }

    @Override
    public synchronized void disconnect() throws IOException
    {
        conn = null;
        super.disconnect();
    }
}
