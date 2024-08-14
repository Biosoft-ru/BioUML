package ru.biosoft.server;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import ru.biosoft.exception.InternalException;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.security.SecurityManager;

public class Request
{
    protected Logger log;

    protected ClientConnection conn;

    /**
     * Number of topic read in current session.
     */
    protected int topicNumber = 0;

    protected String connError;

    /**
     * @param conn
     * @param log
     */
    public Request(ClientConnection conn, Logger log)
    {
        this.conn = conn;
        this.log = log;
        synchronized( conn )
        {
            conn.usageCount++;
        }
    }

    public boolean isMutable()
    {
        synchronized( conn )
        {
            return conn.isMutable();
        }
    }

    public String getConnectionInfo()
    {
        return conn.getHost() + ":" + conn.getPort();
    }

    public ClientConnection getConnection()
    {
        return conn;
    }
    
    /**
     * Opens the connection with the server, sends request, reads the answer,
     * check it, close the connection and return the result as byte array.
     * 
     * @param service
     * @param command
     * @param arguments
     * @param readAnswer
     * @return
     * @throws BiosoftNetworkException
     */
    public byte[] request(String service, int command, Map<String, String> arguments, boolean readAnswer) throws BiosoftNetworkException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        request(service, command, arguments, readAnswer, baos);
        byte[] result = baos.toByteArray();
        if( result.length > 0 )
        {
            return result;
        }
        return null;
    }
    
    public @Nonnull String requestString(String service, int command, Map<String, String> arguments) throws BiosoftNetworkException
    {
        byte[] result = request(service, command, arguments, true);
        if(result == null)
            return "";
        try
        {
            return new String(result, "UTF-16BE");
        }
        catch( UnsupportedEncodingException e )
        {
            throw new InternalException(e);
        }
    }

    /**
     * Opens the connection with the server, sends request, reads the answer to output stream,
     * check it, and close the connection.
     * 
     * @param service
     * @param command
     * @param arguments
     * @param readAnswer
     * @param out
     * @throws IOException
     */
    public void request(String service, int command, Map<String, String> arguments, boolean readAnswer, OutputStream out) throws BiosoftNetworkException
    {
        int state;
        if( conn == null )
            return;

        synchronized( conn )
        {
            try
            {
                conn.connect();
                
                if( arguments != null )
                {
                    Map<String, String> fullArguments = new HashMap<>(arguments);
                    // send service command
                    if(conn.getSessionId() != null)
                    {
                        fullArguments.put(SecurityManager.SESSION_ID, conn.getSessionId());
                    }
                    fullArguments.put(Connection.KEY_SERVICE, service);
                    fullArguments.put(Connection.KEY_COMMAND, String.valueOf(command));

                    if( !conn.sendArguments(fullArguments) )
                        return;

                    // reads answer and check it
                    topicNumber++;
                    log.log(Level.FINE, "Reads the data from the server (topic " + topicNumber + ").");
                }

                // read status
                InputStream is = conn.getIs();
                if( is == null )
                {
                    throw new IOException("Cannot read answer for request " + service + "->" + command);
                }
                DataInputStream dis = new DataInputStream(is);
                state = dis.readInt();
                if( state == Connection.ERROR )
                {
                    throw new BiosoftNetworkException(conn.getHost(), dis.readUTF());
                }
                else if( state != Connection.OK )
                {
                    throw new IOException("Connection state is invalid: 0x" + Integer.toHexString(state)+" for request " + service + "->" + command);
                }
                if( readAnswer )
                {
                    BufferedOutputStream bos = new BufferedOutputStream(out);
                    int format = dis.readInt();
                    long length = dis.readInt();
                    if( length == -1 )
                    {
                        length = dis.readLong();
                    }
                    if( length > 0 )
                    {
                        if( format == Connection.FORMAT_SIMPLE )
                        {
                            int bufferSize = (int)Math.min(length, 1048576);
                            byte[] buffer = new byte[bufferSize];
                            long len = 0;
                            while( len < length )
                            {
                                int curread = dis.read(buffer);
                                len += curread;
                                bos.write(buffer, 0, curread);
                            }
                            bos.close();
                        }
                        else if( format == Connection.FORMAT_GZIP )
                        {
                            //TODO: optimize for work with long data
                            byte[] b = new byte[(int)length];
                            dis.readFully(b);

                            try(InputStream zis = new GZIPInputStream(new ByteArrayInputStream(b)))
                            {
                                length = dis.readInt();

                                b = new byte[(int)length];
                                int len = 0;
                                while( len < length )
                                    len += zis.read(b, len, (int)length - len);
                            }
                            bos.write(b);
                            bos.close();
                        }
                    }
                }
            }
            catch( IOException ex )
            {
                throw new BiosoftNetworkException(ex, conn.getHost());
            }
            finally
            {
                try
                {
                    conn.setLastUsage();
                    conn.disconnect();
                }
                catch( IOException e )
                {
                }
            }
        }
    }

    /**
     * Special function for reading of request arguments which was send by this
     * request - Symple wrapper over <code>ClientConnection.getArguments</code>
     * 
     * @param is
     * @return
     * @throws IOException
     */
    public static Map getArguments(InputStream is) throws IOException
    {
        return ClientConnection.getArguments(is);
    }

    /**
     * Get service name or <B>null</B> from reading arguments
     * 
     * @param arguments
     * @return
     */
    public static String getService(Map arguments)
    {
        Object service = arguments.get(Connection.KEY_SERVICE);
        if( service == null )
            return null;
        return service.toString();
    }

    /**
     * Get command id from reading arguments
     * 
     * @param arguments
     * @return
     */
    public static Integer getCommand(Map arguments)
    {
        Object command = arguments.get(Connection.KEY_COMMAND);
        if( command == null )
            return Connection.DISCONNECT;
        try
        {
            return Integer.parseInt(command.toString());
        }
        catch( NumberFormatException e )
        {
        }
        return Connection.DISCONNECT;
    }

    /**
     * close connection
     */
    public synchronized void close()
    {
        if( conn == null )
            return;
        synchronized( conn )
        {
            conn.usageCount--;
        }
        conn = null;
    }

    @Override
    protected void finalize() throws Throwable
    {
        close();
    }

    public void setSessionId(String sessionId)
    {
        conn.setSessionId(sessionId);
    }
}
