package com.developmentontheedge.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;

import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.Connection;
import ru.biosoft.server.ConnectionPool;
import ru.biosoft.server.Request;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

public class JobControlClient implements JobControl
{
    protected String id;

    protected Request connection;

    protected Logger log;

    protected List<JobControlListener> listeners;

    protected ClientConnection conn;

    public JobControlClient(Logger cat, Class<? extends ClientConnection> connectionClass, String id, String url) throws Exception
    {
        this(cat, null, connectionClass, id, url);
    }

    public JobControlClient(Logger cat, JobControlListener listener, Class<? extends ClientConnection> connectionClass, String id, String url) throws Exception
    {
        listeners = new ArrayList<>();
        if( listener != null )
            listeners.add(listener);
        this.log = cat;
        if( cat == null )
            cat = Logger.getLogger(JobControlClient.class.getName());
        this.id = id;

        conn = ConnectionPool.getConnection(connectionClass, url, true);

        connection = new Request(conn, cat);
    }

    /**
     * Opens the connection with the server, sends request, reads the answer,
     * check it, and close the connection.
     * 
     * @param command
     *            request command (cod)
     * @param argument
     *            request argument
     * 
     * @see Connection
     */
    public byte[] request(int command, Map<String, String> data, boolean readAnswer) throws BiosoftNetworkException
    {
        if( connection != null )
            return connection.request(DoteProtocol.DOTE_SERVICE, command, data, readAnswer);
        return null;
    }

    // /////////////////////////////////////////////////////////////////
    // implementation
    //

    @Override
    public void pause()
    {
        Map<String, String> map = new HashMap<>();
        map.put(DoteProtocol.KEY_LISTENER, id);
        try
        {
            request(DoteProtocol.DB_JOBCONTROL_STOP, map, false);
        }
        catch( BiosoftNetworkException e )
        {
            log.log(Level.SEVERE, "Connection error", e);
        }
    }

    @Override
    public void resume()
    {
        Map<String, String> map = new HashMap<>();
        map.put(DoteProtocol.KEY_LISTENER, id);
        try
        {
            request(DoteProtocol.DB_JOBCONTROL_RESUME, map, false);
        }
        catch( BiosoftNetworkException e )
        {
            log.log(Level.SEVERE, "Connection error", e);
        }
    }

    @Override
    public void terminate()
    {
        if( eventsObtainer != null )
        {
            eventsObtainer.setQuit();
        }
        Map<String, String> map = new HashMap<>();
        map.put(DoteProtocol.KEY_LISTENER, id);
        try
        {
            request(DoteProtocol.DB_JOBCONTROL_CANCEL, map, false);
        }
        catch( BiosoftNetworkException e )
        {
            log.log(Level.SEVERE, "Connection error", e);
        }
        connection.close();
        conn.close();
        if( log != null )
            log.info("Close connection: " + conn);
    }

    @Override
    public void run()
    {
        Map<String, String> map = new HashMap<>();
        map.put(DoteProtocol.KEY_LISTENER, id);
        try
        {
            request(DoteProtocol.DB_JOBCONTROL_START, map, false);
        }
        catch( BiosoftNetworkException e )
        {
            log.log(Level.SEVERE, "Connection error", e);
        }
    }

    @Override
    public int getStatus()
    {
        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(DoteProtocol.KEY_LISTENER, id);
            byte[] result = request(DoteProtocol.DB_JOBCONTROL_GET_STATUS, map, true);
            if( result != null )
                return Integer.parseInt(new String(result, "UTF-16BE"));
        }
        catch( NumberFormatException e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Wrong status format");
        }
        catch( IOException e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Cannot get status");
        }
        return JobControl.TERMINATED_BY_ERROR;
    }

    @Override
    public int getPreparedness()
    {
        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(DoteProtocol.KEY_LISTENER, id);
            byte[] result = request(DoteProtocol.DB_JOBCONTROL_GET_PREPAREDNESS, map, true);
            if( result != null )
                return Integer.parseInt(new String(result, "UTF-16BE"));
        }
        catch( NumberFormatException e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Wrong preparedness");
        }
        catch( IOException e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Cannot get preparedness");
        }
        return 0;
    }

    @Override
    public String getTextStatus()
    {
        return AbstractJobControl.getTextStatus(getStatus());
    }

    @Override
    public long getCreatedTime()
    {
        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(DoteProtocol.KEY_LISTENER, id);
            byte[] result = request(DoteProtocol.DB_JOBCONTROL_GET_CREATEDTIME, map, true);
            if( result != null )
                return Long.parseLong(new String(result, "UTF-16BE"));
        }
        catch( IOException e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Cannot get time");
        }
        return System.currentTimeMillis();
    }

    @Override
    public long getRemainedTime()
    {
        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(DoteProtocol.KEY_LISTENER, id);
            byte[] result = request(DoteProtocol.DB_JOBCONTROL_GET_REMAINEDTIME, map, true);
            if( result != null )
                return Long.parseLong(new String(result, "UTF-16BE"));
        }
        catch( IOException e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Cannot get time");
        }
        return System.currentTimeMillis();
    }

    @Override
    public long getElapsedTime()
    {
        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(DoteProtocol.KEY_LISTENER, id);
            byte[] result = request(DoteProtocol.DB_JOBCONTROL_GET_ELAPSEDTIME, map, true);
            if( result != null )
                return Long.parseLong(new String(result, "UTF-16BE"));
        }
        catch( IOException e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Cannot get time");
        }
        return System.currentTimeMillis();
    }

    @Override
    public long getStartedTime()
    {
        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(DoteProtocol.KEY_LISTENER, id);
            byte[] result = request(DoteProtocol.DB_JOBCONTROL_GET_STARTEDTIME, map, true);
            if( result != null )
                return Long.parseLong(new String(result, "UTF-16BE"));
        }
        catch( IOException e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Cannot get time");
        }
        return System.currentTimeMillis();
    }

    @Override
    public long getEndedTime()
    {
        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(DoteProtocol.KEY_LISTENER, id);
            byte[] result = request(DoteProtocol.DB_JOBCONTROL_GET_ENDEDTIME, map, true);
            if( result != null )
                return Long.parseLong(new String(result, "UTF-16BE"));
        }
        catch( IOException e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Cannot get time");
        }
        return System.currentTimeMillis();
    }

    @Override
    public void setPreparedness(int percent)
    {
        Map<String, String> map = new HashMap<>();
        map.put(DoteProtocol.KEY_LISTENER, id);
        map.put(DoteProtocol.KEY_PREPEREDNESS, String.valueOf(percent));
        try
        {
            request(DoteProtocol.DB_JOBCONTROL_SET_PREPAREDBESS, map, false);
        }
        catch( BiosoftNetworkException e )
        {
            log.log(Level.SEVERE, "Connection error", e);
        }
    }

    @Override
    public void addListener(JobControlListener listener)
    {
        if( listener != null )
            listeners.add(listener);
        if( listeners.size() > 0 )
            startWatchDog();
    }

    @Override
    public void removeListener(JobControlListener listener)
    {
        if( listener != null )
        {
            listeners.remove(listener);
            // System.out.println("Job control with id \"" + id + "\" remove
            // listener");
        }
    }

    protected void parseEvent(byte[] e) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(e);
        ObjectInputStream ois = new ObjectInputStream(bais);
        JobControlServerListener.Event event = (JobControlServerListener.Event)ois.readObject();

        if( event.status == JobControl.TERMINATED_BY_ERROR || event.status == JobControl.TERMINATED_BY_REQUEST
                || event.status == JobControl.COMPLETED )
        {
            if( eventsObtainer != null )
                eventsObtainer.setQuit();
        }

        for( JobControlListener listener : listeners )
        {
            JobControlEvent jobEvent = new JobControlEvent(this);
            if( event.what.equals(JobControlServerListener.VALUE_CHANGED) )
                listener.valueChanged(jobEvent);
            else if( event.what.equals(JobControlServerListener.JOB_STARTED) )
                listener.jobStarted(jobEvent);
            else if( event.what.equals(JobControlServerListener.JOB_TERMINATED) )
                listener.jobTerminated(jobEvent);
            else if( event.what.equals(JobControlServerListener.JOB_PAUSED) )
                listener.jobPaused(jobEvent);
            else if( event.what.equals(JobControlServerListener.JOB_RESUMED) )
                listener.jobResumed(jobEvent);
            else if( event.what.equals(JobControlServerListener.RESULTS_READY) )
                listener.resultsReady(jobEvent);
        }
    }

    protected EventObtainer eventsObtainer = null;

    protected void startWatchDog()
    {
        if( eventsObtainer == null )
        {
            eventsObtainer = new EventObtainer();
            eventsObtainer.start();
        }
    }

    protected class EventObtainer extends Thread
    {
        volatile boolean quit = false;

        public void setQuit()
        {
            quit = true;
        }

        @Override
        public void run()
        {
            while( !quit )
            {
                try
                {
                    Thread.sleep(5000);
                    Map<String, String> map = new HashMap<>();
                    map.put(DoteProtocol.KEY_LISTENER, id);
                    byte[] event = request(DoteProtocol.DB_JOBCONTROL_GET_EVENTS, map, true);
                    if( event != null )
                    {
                        String str = new String(event, "UTF-16BE");
                        if( !"null".equals(str) )
                            parseEvent(event);
                    }
                }
                catch( Throwable t )
                {
                    if( log != null )
                        log.log(Level.SEVERE, "Watch dog error", t);
                    break;
                }
            }
        }
    }

}
