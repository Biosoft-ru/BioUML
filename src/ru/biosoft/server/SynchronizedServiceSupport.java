package ru.biosoft.server;

import java.io.EOFException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;

/**
 * Base implementation which contain all necessary functions
 * for implementation of new service
 *
 * TODO: move all services to async mode and remove this class
 */
public abstract class SynchronizedServiceSupport implements Service
{
    protected static final Logger log = Logger.getLogger(SynchronizedServiceSupport.class.getName());

    ///////////////////////////////////////////////////////////////////
    // Properties
    //

    /** Session connection */
    private Response connection;
    /** Get session connection. */
    public synchronized Response getSessionConnection()
    {
        return connection;
    }

    /** Session arguments */
    private Map<String, Object> arguments;

    /** Get session arguments. */
    public synchronized Map<String, Object> getSessionArguments()
    {
        return arguments;
    }

    ///////////////////////////////////////////////////////////////////
    // Utility functions
    //

    /**
     * Set up the specified data collection.<p>
     *
     * If such data collection is not loaded on the Server
     * the error message will be sent to the client.
     */
    public DataCollection getDataCollection() throws IOException
    {
        Object dcName = arguments.get(Connection.KEY_DC);
        if( dcName == null )
        {
            connection.error("didn't send data collection complete name");
            return null;
        }

        ////////////////////////////////////////////////

        DataCollection dc = CollectionFactory.getDataCollection(dcName.toString());
        if( dc == null )
        {
            connection.error("cannot find data collection, complete name=" + dcName.toString());
            return null;
        }

        return dc;
    }

    ///////////////////////////////////////////////////////////////////
    // Protocol implementation functions
    //

    @Override
    public synchronized void processRequest(Integer command, Map data, Response out)
    {
        connection = out;
        arguments = data;

        String sessionId = (String)data.get(SecurityManager.SESSION_ID);
        if( sessionId == null )
        {
            sessionId = SecurityManager.generateSessionId();
        }
        SecurityManager.addThreadToSessionRecord(Thread.currentThread(), sessionId);

        try
        {
            int comm = command.intValue();
            if( comm == Connection.DISCONNECT )
                connection.disconnect();
            else if( !processRequest(comm) )
                connection.error("unknown command: " + command + '.');
        }
        catch( EOFException eof )
        {
            log.log(Level.SEVERE, "Error processing request: EOF exception", eof);
        }
        catch( Throwable e )
        {
            try
            {
                connection.error(ExceptionRegistry.log(e));
                connection.disconnect();
                log.log(Level.SEVERE, "Error processing request (1)", e);
            }
            catch( IOException io )
            {
                log.log(Level.SEVERE, "IOException while processing request "+getClass().getName()+":"+command+": "+io.toString());
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Exception while processing request "+getClass().getName()+":"+command, t);
            }
        }
        connection = null;
        arguments = null;
    }

    protected abstract boolean processRequest(int command) throws Exception;
}
