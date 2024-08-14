package com.developmentontheedge.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

import ru.biosoft.server.Connection;
import ru.biosoft.server.Response;
import ru.biosoft.server.SynchronizedServiceSupport;

import com.developmentontheedge.server.JobControlServerListener.Event;

/**
 * Provides functionality of the LuceneService.
 * 
 * @see Connection
 */
public class DoteService
        extends SynchronizedServiceSupport
{

    protected Response connection;

    protected Map arguments;

    protected JobControlServerListener listener = null;
    
    @Override
    public boolean processRequest ( int command )
            throws Exception
    {
        connection = getSessionConnection ( );
        arguments = getSessionArguments ( );

        switch ( command )
        {
        case DoteProtocol.DB_JOBCONTROL_SET_LISTENER:
            setListener ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_START:
            start ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_STOP:
            stop ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_RESUME:
            resume ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_CANCEL:
            cancel ( );
            break;

        case DoteProtocol.DB_JOBCONTROL_GET_PREPAREDNESS:
            sendPreparegness ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_GET_MESSAGE:
            sendMessage ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_GET_STATUS:
            sendStatus ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_GET_REMAINEDTIME:
            sendRemainedTime ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_GET_ELAPSEDTIME:
            sendElapsedTime ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_GET_STARTEDTIME:
            sendStartedTime ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_GET_ENDEDTIME:
            sendEndedTime ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_GET_CREATEDTIME:
            sendCreatedTime ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_SET_PREPAREDBESS:
            setPreparedness ( );
            break;
        case DoteProtocol.DB_JOBCONTROL_GET_EVENTS:
            sendEvents ( );
            break;

        default:
            return false;
        }
        return true;
    }

    // ////////////////////////////////////////////
    // Protocol implementation functions
    //

    protected boolean setListener ( )
            throws IOException
    {
        Object id = arguments.get ( DoteProtocol.KEY_LISTENER );
        if ( id == null )
        {
            connection.error ( "did't send job control listener id." );
            return false;
        }

        listener = JobControlServerListener.getListener ( id.toString ( ) );
        if ( listener == null )
        {
            connection.error ( "cannot set job control listener with id " + id + "." );
            return false;
        }

        return true;
    }

    protected void start ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        listener.getJobControl ( ).run ( );
        connection.send ( "".getBytes(), Connection.FORMAT_SIMPLE );
    }

    protected void stop ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        listener.getJobControl ( ).pause ( );
        connection.send ( "".getBytes(), Connection.FORMAT_SIMPLE );
    }

    protected void resume ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        listener.getJobControl ( ).resume ( );
        connection.send ( "".getBytes(), Connection.FORMAT_SIMPLE );
    }

    protected void cancel ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        listener.getJobControl ( ).terminate ( );
        connection.send ( "".getBytes(), Connection.FORMAT_SIMPLE );
    }

    protected void sendPreparegness ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        connection.send ( ( "" + listener.getPreparedness ( ) ).getBytes ( "UTF-16BE" ), Connection.FORMAT_SIMPLE );
    }

    protected void sendMessage ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        connection.send ( ( "" + listener.getMessage ( ) ).getBytes ( "UTF-16BE" ), Connection.FORMAT_SIMPLE );
    }

    protected void sendStatus ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        connection.send ( ( "" + listener.getStatus ( ) ).getBytes ( "UTF-16BE" ), Connection.FORMAT_SIMPLE );
    }

    protected void sendRemainedTime ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        connection.send ( ( "" + listener.getJobControl ( ).getRemainedTime ( ) ).getBytes ( "UTF-16BE" ), Connection.FORMAT_SIMPLE );
    }

    protected void sendElapsedTime ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        connection.send ( ( "" + listener.getJobControl ( ).getElapsedTime ( ) ).getBytes ( "UTF-16BE" ), Connection.FORMAT_SIMPLE );
    }

    protected void sendStartedTime ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        connection.send ( ( "" + listener.getJobControl ( ).getStartedTime ( ) ).getBytes ( "UTF-16BE" ), Connection.FORMAT_SIMPLE );
    }

    protected void sendEndedTime ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        connection.send ( ( "" + listener.getJobControl ( ).getEndedTime ( ) ).getBytes ( "UTF-16BE" ), Connection.FORMAT_SIMPLE );
    }

    protected void sendCreatedTime ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        connection.send ( ( "" + listener.getJobControl ( ).getCreatedTime ( ) ).getBytes ( "UTF-16BE" ), Connection.FORMAT_SIMPLE );
    }

    protected void setPreparedness ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;
        Object preperedness = arguments.get ( DoteProtocol.KEY_PREPEREDNESS );
        if ( preperedness == null )
        {
            connection.error ( "did't send new job control preperedness." );
            return;
        }
        int percent = 0;
        try
        {
            percent = Integer.parseInt ( preperedness.toString ( ) );
        }
        catch ( NumberFormatException e )
        {
            connection.error ( "invalid new preperedness value = " + preperedness );
            return;
        }
        if ( percent < 0 || percent > 100 )
        {
            connection.error ( "wrond listener id." );
            return;
        }
        listener.getJobControl ( ).setPreparedness ( percent );
        connection.send ( "".getBytes(), Connection.FORMAT_SIMPLE );
    }

    public void sendEvents ( )
            throws IOException
    {
        if ( !setListener ( ) )
            return;

        Event e = listener.getLastEvent ( );
        if ( e != null )
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream ( );
            ObjectOutputStream oos = new ObjectOutputStream ( baos );
            oos.writeObject ( e );
            oos.flush ( );
            connection.send ( baos.toByteArray ( ), Connection.FORMAT_SIMPLE );
            return;
        }
        connection.send ( "null".getBytes ( "UTF-16BE" ), Connection.FORMAT_SIMPLE );
    }
}
