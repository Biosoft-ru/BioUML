package ru.biosoft.server.servlets.webservices;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.tasks.StubJobControl;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.util.LimitedTextBuffer;
import ru.biosoft.util.TextUtil2;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

/**
 * Class representing job launched from the web
 * @author lan
 */
public class WebJob
{
    public static final String JOB_DATA_PREFIX = "jobData/";
    private static final int MAX_MESSAGE_ROWS = 1000;
    private static AtomicInteger jobNumber = new AtomicInteger();

    private final boolean valid;

    private final Set<ru.biosoft.access.core.DataElementPath> jobResults = Collections.synchronizedSet(new LinkedHashSet<ru.biosoft.access.core.DataElementPath>());
    private JobControl jobControl;
    private final LimitedTextBuffer message = new LimitedTextBuffer(MAX_MESSAGE_ROWS);
    private final String loggerID;
    private TaskInfo task;

    /**
     * Returns WebJob associated with existing task and assigns jobID to it (so later the job can be obtained with this jobID)
     * Useful to get the job from another session
     * @param jobID
     * @param task
     * @return
     */
    public static WebJob attach(String jobID, TaskInfo task)
    {
        try
        {
            WebJob result = (WebJob)task.getTransient( "webJob" );
            if( result == null )
            {
                result = getWebJob( jobID );
                result.setTask( task );
                result.addJobMessage( task.getLogInfo() );
            }
            WebSession.getCurrentSession().putValue( "webJob/" + jobID, result );
            return result;
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public static WebJob getWebJob(String jobID)
    {
        if(jobID == null || jobID.isEmpty()) return new WebJob(false, null, null);
        WebSession session = WebSession.getCurrentSession();
        synchronized(session)
        {
            WebJob result = null;
            try
            {
                result = (WebJob)session.getValue("webJob/"+jobID);
            }
            catch( Exception e )
            {
            }
            if(result == null)
            {
                JobControl jobControl = null;
                Object job = WebServicesServlet.getSessionCache().getObject("job_" + jobID);
                if(job instanceof JobControl)
                {
                    jobControl = (JobControl)job;
                }
                result = new WebJob(true, jobControl, "WebJob #" + jobNumber.incrementAndGet() + " (" + session.getUserName() + ")");
                session.putValue("webJob/"+jobID, result);
            }
            return result;
        }
    }

    public static Object getJobData(String completePath)
    {
        if(!completePath.startsWith(JOB_DATA_PREFIX)) return null;
        String[] components = TextUtil2.split( completePath, '/' );
        try
        {
            TaskInfo taskInfo = getWebJob(components[1]).getTask();
            Object data = taskInfo.getTransient(components[2]);
            if(data == null)
                data = taskInfo.getSource().optDataElement();
            return data;
        }
        catch(Exception e)
        {
            return null;
        }
    }

    private WebJob(boolean valid, JobControl jobControl, String loggerID)
    {
        this.valid = valid;
        this.loggerID = loggerID;
        setJobControl(jobControl);
    }

    /**
     * Return list of job results or null if no results present/reported by job
     */
    public DataElementPath[] getJobResults()
    {
        if( !valid ) return null;
        return jobResults.toArray(new ru.biosoft.access.core.DataElementPath[jobResults.size()]);
    }

    /**
     * Add list of result paths to this job
     * @param resultPaths
     */
    public void addJobResults(List<ru.biosoft.access.core.DataElementPath> resultPaths)
    {
        if( valid ) jobResults.addAll(resultPaths);
    }

    /**
     * Add single result path to resulting paths of this job
     */
    public void addJobResult(DataElementPath resultPath)
    {
        if( valid ) jobResults.add(resultPath);
    }

    /**
     * TODO Clean finished jobs automatically. Otherwise there can become many of them for long sessions
     */
    public FunctionJobControl createJobControl()
    {
        if( !valid ) return null;
        FunctionJobControl job = new FunctionJobControl( getJobLogger() );
        setJobControl(job);
        job.functionStarted();
        return job;
    }

    /**
     * Get message associated with the job
     * Returns empty String in case of any errors
     */
    public String getJobMessage()
    {
        if( !valid )
            return "";
        return message.toString();
    }

    public void addJobMessage(String message)
    {
        if( !valid ) return;
        this.message.add(message);
    }

    public TaskInfo getTask()
    {
        return task;
    }

    public void setTask(TaskInfo task)
    {
        this.task = task;
        if(task != null)
        {
            task.setTransient("webJob", this);
            setJobControl(task.getJobControl());
        }
    }

    /**
     * Associates supplied jobControl with current WebJob object
     * @param job jobControl to associate
     */
    public void setJobControl(final JobControl job)
    {
        if( !valid || job == null || job == this.jobControl )
            return;
        this.jobControl = job;
        job.addListener(new JobControlListenerAdapter()
        { // Capture error message
            @Override
            public void jobTerminated(JobControlEvent event)
            {
                if(event.getStatus() == JobControl.COMPLETED) job.removeListener(this);
                setJobControl(new StubJobControl(jobControl));
            }
            @Override
            public void resultsReady(JobControlEvent event)
            {
                Object[] results = event.getResults();
                List<ru.biosoft.access.core.DataElementPath> resultPaths = new ArrayList<>();
                if( results != null )
                {
                    for( Object result : results )
                    {
                        if( result instanceof DataElement )
                        {
                            DataElementPath resultPath = null;
                            if( ( (DataElement)result ).getOrigin() != null
                                    && ( (DataElement)result ).getOrigin().contains((DataElement)result) )
                                resultPath = DataElementPath.create((DataElement)result);
                            if( resultPath != null )
                                resultPaths.add(resultPath);
                        }
                        else if( result instanceof DataElementPath )
                        {
                            resultPaths.add((DataElementPath)result);
                        }
                        else if( result instanceof BufferedImage )
                        {
                            String name = UUID.randomUUID().toString();
                            WebSession.getCurrentSession().putImage(name, (BufferedImage)result);
                            resultPaths.add(DataElementPath.create("images/"+name));
                        }
                    }
                }
                addJobResults(resultPaths);
                if( event.getStatus() == JobControl.COMPLETED || event.getStatus() == JobControl.TERMINATED_BY_ERROR
                        || event.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    job.removeListener(this);
            }
        });
    }

    /**
     * Get logger associated with jobID and current session
     * @return Logger object (it will be created if wasn't associated previously)
     * TODO Implement separate logger pool which will remove older loggers
     */
    public Logger getJobLogger()
    {
        return Logger.getLogger(loggerID);
    }

    /**
     * Returns JobControl object associated with specified WebJob
     * @return JobControl object associated with specified WebJob
     */
    public JobControl getJobControl()
    {
        if( !valid )
            return null;
        return jobControl;
    }

    /**
     * @return true if WebJob is valid (i.e. was initialized with non-null and non-empty jobID)
     */
    public boolean isValid()
    {
        return valid;
    }
}
