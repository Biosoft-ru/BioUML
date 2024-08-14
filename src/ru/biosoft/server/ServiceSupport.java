package ru.biosoft.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.Map;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.MissingParameterException;
import ru.biosoft.access.security.SecurityManager;

/**
 * @author lan
 *
 */
public abstract class ServiceSupport implements Service
{
    public static class ServiceRequest
    {
        /**
         * @param connection
         * @param arguments
         */
        public ServiceRequest(Response connection, Map arguments)
        {
            super();
            this.connection = connection;
            this.arguments = arguments;
        }

        /** Session connection */
        private Response connection;
        /** Get session connection. */
        public Response getSessionConnection()
        {
            return connection;
        }

        public void disconnect()
        {
            connection.disconnect();
        }

        public void send(String message) throws UnsupportedEncodingException, IOException
        {
            if(message == null)
                connection.send(null, Connection.FORMAT_SIMPLE);
            else
                connection.send(message.getBytes("UTF-16BE"), message.length() > 1000 ? Connection.FORMAT_GZIP : Connection.FORMAT_SIMPLE);
        }

        public void sendBytes(byte[] data) throws UnsupportedEncodingException, IOException
        {
            connection.send(data, data.length > 1000 ? Connection.FORMAT_GZIP : Connection.FORMAT_SIMPLE);
        }

        public void error(String message) throws IOException
        {
            connection.error(message);
        }

        /** Session arguments */
        private Map arguments;

        public String get(String name)
        {
            Object value = arguments.get(name);
            return value == null ? null : value.toString();
        }

        public int getInt(String key, int defaultValue)
        {
            Object parameter = arguments.get(key);
            try
            {
                return parameter == null ? defaultValue : Integer.parseInt(parameter.toString());
            }
            catch( NumberFormatException e )
            {
                return defaultValue;
            }
        }

        public float getFloat(String key, float defaultValue)
        {
            Object parameter = arguments.get(key);
            try
            {
                return parameter == null ? defaultValue : Float.parseFloat(parameter.toString());
            }
            catch( NumberFormatException e )
            {
                return defaultValue;
            }
        }

        public DataElementPath getDataElementPath() throws IOException
        {
            String dcName = get(Connection.KEY_DC);
            if( dcName == null )
            {
                error( new MissingParameterException( Connection.KEY_DC ).getMessage() );
                return null;
            }
            String deName = get(Connection.KEY_DE);
            if( deName == null )
            {
                error( new MissingParameterException( Connection.KEY_DE ).getMessage() );
                return null;
            }
            return DataElementPath.create(dcName, deName);
        }

        /**
         * Set up the specified data collection.<p>
         *
         * If such data collection is not loaded on the Server
         * the error message will be sent to the client.
         */
        public DataCollection getDataCollection() throws IOException
        {
            DataElementPath dcName = DataElementPath.create(get(Connection.KEY_DC));
            if( dcName == null )
            {
                error( new MissingParameterException( Connection.KEY_DC ).getMessage() );
                return null;
            }
            ////////////////////////////////////////////////

            try
            {
                return dcName.getDataElement(DataCollection.class);
            }
            catch(Throwable t)
            {
                error(ExceptionRegistry.log(t));
                return null;
            }
        }
    }

    protected static final Logger log = Logger.getLogger(ServiceSupport.class.getName());

    ///////////////////////////////////////////////////////////////////
    // Protocol implementation functions
    //

    @Override
    public void processRequest(Integer command, Map data, Response out)
    {
        ServiceRequest request = new ServiceRequest(out, data);

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
                request.disconnect();
            else if( !processRequest(request, comm) )
                request.error("unknown command: " + command + '.');
        }
        catch( EOFException eof )
        {
            log.log(Level.SEVERE, "Error processing request: EOF exception", eof);
        }
        catch( Throwable e )
        {
            try
            {
                request.error(ExceptionRegistry.log(e));
                request.disconnect();
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
    }

    protected abstract boolean processRequest(ServiceRequest request, int command) throws Exception;
}
