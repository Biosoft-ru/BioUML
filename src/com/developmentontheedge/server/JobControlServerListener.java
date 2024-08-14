package com.developmentontheedge.server;

import java.io.Serializable;
import java.util.HashMap;

import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

/**
 *
 */
public class JobControlServerListener implements JobControlListener
{

    private static final HashMap<String, JobControlServerListener> listeners = new HashMap<>();
        
    public static JobControlServerListener getListener(String id)
    {
        return listeners.get(id);
    }
    
    public static final String VALUE_CHANGED     = "value changed";
    
    public static final String JOB_STARTED         = "job started";
    
    public static final String JOB_TERMINATED     = "job terminated";
    
    public static final String JOB_PAUSED         = "job paused";
    
    public static final String JOB_RESUMED         = "job resumed";
    
    public static final String RESULTS_READY     = "results ready";
    
        
    /**
     * Create new server listener with unique identificator
     * and you can access to this control on the client through
     * this id
     * 
     * @param id
     */
    protected JobControl jobControl;
    protected String id;
    public JobControlServerListener(JobControl jobControl, String id)
    {
        this.jobControl = jobControl;
        this.id = id;
        listeners.put(id, this);
    }
    
    public JobControl getJobControl()
    {
        return jobControl;
    }
    
    protected int preparedness = 0;
    public int getPreparedness()
    {
        return preparedness;
    }
    
    protected String message = "";
    public String getMessage()
    {
        return message;
    }
    
    protected int status = JobControl.CREATED;
    public int getStatus()
    {
        return status;
    }
    
    public static class Event implements Serializable
    {
        public long from;
        public String what;
        public int preparedness;
        public String message;
        public int status;
    }
    
    protected Event event = null;
    protected void addEvent(JobControlEvent event, String what)
    {
        Event e = new Event();
        e.from = System.currentTimeMillis();
        e.what = what;
        e.preparedness = event.getPreparedness();
    //    if (preparedness != e.preparedness)
    //    {
    //        System.out.println("Job Control with id \"" + id + "\" set preperedness = " + e.preparedness);
    //    }
    //    System.out.println("Preparedness is: " + e.preparegness);
        e.message = event.getMessage();
        e.status = event.getStatus();
        this.event = e;
    }
    /*
    public Event getEvent()
    {
    //    System.out.println("Get events from = " + from);
    //    for (int i = 0; i < events.size(); i++)
    //    {
    //        System.out.println("Event " + i + " has from = " + ((Event)events.get(i)).from);
    //        if (((Event)events.get(i)).from >= from)
    //            return events.subList(i, events.size());
    //    }
    //    return new Vector();
    }
    */
    
    public Event getLastEvent()
    {
        return event;
    //    if (events != null && events.size() > 0)
    //        return (Event)events.lastElement();
    //    return null;
    }
    
    @Override
    public void valueChanged(JobControlEvent event)
    {
    //    System.out.println("Value changed message");
        addEvent(event, VALUE_CHANGED);
        preparedness = event.getPreparedness();
        message = event.getMessage();
        status = event.getStatus();
    }

    @Override
    public void jobStarted(JobControlEvent event)
    {
    //    System.out.println("Job started message");
        addEvent(event, JOB_STARTED);
        preparedness = event.getPreparedness();
        message = event.getMessage();
        status = event.getStatus();
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
    //    System.out.println("Job terminated message");
        addEvent(event, JOB_TERMINATED);
        preparedness = event.getPreparedness();
        message = event.getMessage();
        status = event.getStatus();
    }

    @Override
    public void jobPaused(JobControlEvent event)
    {
    //    System.out.println("Job paused message");
        addEvent(event, JOB_PAUSED);
        preparedness = event.getPreparedness();
        message = event.getMessage();
        status = event.getStatus();
    }

    @Override
    public void jobResumed(JobControlEvent event)
    {
    //    System.out.println("Job resumed message");
        addEvent(event, JOB_RESUMED);
        preparedness = event.getPreparedness();
        message = event.getMessage();
        status = event.getStatus();
    }

    @Override
    public void resultsReady(JobControlEvent event)
    {
    //    System.out.println("Result ready message");
        addEvent(event, RESULTS_READY);
        preparedness = event.getPreparedness();
        message = event.getMessage();
        status = event.getStatus();
    }
    
}
