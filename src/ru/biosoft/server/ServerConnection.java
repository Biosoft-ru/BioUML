package ru.biosoft.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides functionality of the server.
 *
 * @see Connection
 */
public class ServerConnection extends Connection implements Runnable
{

    protected static final Logger log = Logger.getLogger( ServerConnection.class.getName() );

    /** Date of connection starting. */
    protected Date d = new Date ( );

    /** Time of connection starting. */
    protected long tFrom;

    /** Total size of transmitted data. */
    protected long lengthOfTransmittedData = 0;

    protected InputStream is;

    protected OutputStream os;

    protected boolean isConnection = false;

    protected Socket socket;

    /**
     * Constructs the ServerConnection
     *
     * @param socket
     *            socket for data transfer
     */
    public ServerConnection ( Socket socket ) throws IOException
    {
        this.socket = socket;

        is = socket.getInputStream ( );
        os = socket.getOutputStream ( );

        isConnection = true;
    }

    // ////////////////////////////////////////////
    // Common supported client commands
    //

    /**
     * Closes the connection.
     */
    public synchronized void disconnect ( )
    {
        String s = new SimpleDateFormat().format ( d ) + '\t' + lengthOfTransmittedData + '\t'
                + ( System.currentTimeMillis ( ) - tFrom ) + '\t'
                + ( System.currentTimeMillis ( ) - d.getTime ( ) ) + '\n';
        log.info ( s );

        isConnection = false;
        try
        {
            is.close ( );
            os.close ( );

            if ( socket != null )
                socket.close ( );
        }
        catch ( Exception e )
        {
            log.log( Level.SEVERE, "Disconnection error: " + e, e );
        }
    }

    ////////////////////////////////////////
    // Functions
    //

    /**
     * Reads the command and call corresponding function. If command is
     * DISCONNECT, then closes the connection.
     *
     * @see Connection
     */
    @Override
    public void run ( )
    {
        tFrom = System.currentTimeMillis ( );

        String addressee = socket.getInetAddress ( ).getHostAddress ( ) + ":" + socket.getPort ( );
        Response response = new Response ( os, addressee )
        {
            @Override
            public void disconnect ( )
            {
                ServerConnection.this.isConnection = false;
            }
        };

        try
        {
            while ( isConnection )
            {
                Map arguments = Request.getArguments ( is );

                String serviceName = Request.getService ( arguments );
                Service service = ServiceRegistry.getService ( serviceName );

                if ( service != null )
                    service.processRequest ( Request.getCommand ( arguments ), arguments, response );
                else
                {
                    log.log( Level.SEVERE, "Unknown service: " + serviceName + "." );
                    response.error ( "unknown service: " + serviceName );
                }

                Thread.yield ( );
            }
        }
        catch ( EOFException eof )
        {
        }
        catch ( SocketException e )
        {
            log.info ( "Connection " + addressee + " was closed." );
        }
        catch ( Exception e1 )
        {
            try
            {
                response.error ( "exception: " + e1 );
                disconnect ( );
                log.log( Level.SEVERE, "Server connection error", e1 );
            }
            catch ( Exception e2 )
            {
                disconnect ( );
                log.log( Level.SEVERE, "Server connection error", e2 );
            }
        }
        catch ( Throwable t )
        {
            log.log( Level.SEVERE, "Server connection error", t );
        }
    }

}
