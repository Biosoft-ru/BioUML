package ru.biosoft.tasks;

import java.beans.PropertyDescriptor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.MutableDataElementSupport;
import ru.biosoft.access.log.BiosoftLogger;
import ru.biosoft.access.log.StringBufferListener;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.task.RunnableTask;
import ru.biosoft.access.task.Task;
import ru.biosoft.journal.Journal;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Task information class. Contains {@link JobControl} and any additional properties
 */
public class TaskInfo extends MutableDataElementSupport
{
    //types
    public static final String ANALYSIS = "analysis";
    public static final String SIMULATE = "simulate";
    public static final String SQL = "SQL";
    public static final String SCRIPT = "script";
    public static final String SCRIPT_DOCUMENT = "scriptDocument";
    public static final String IMPORT = "import";
    public static final String WORKFLOW = "workflow";
    public static final String EXPORT = "export";
    public static final String REMOVE = "remove";
    public static final String PERSPECTIVE = "perspective";
    
    public static final String IMPORT_FILE_PROPERTY = "importFile";
    public static final String IMPORT_FORMAT_PROPERTY = "importFormat";
    public static final String IMPORT_OUTPUT_PROPERTY = "outputPath";
    public static final String IMPORT_FILESIZE_PROPERTY = "fileSize";
    
    public static final PropertyDescriptor IMPORT_FORMAT_PROPERTY_DESCRIPTOR = StaticDescriptor.create(IMPORT_FORMAT_PROPERTY);
    public static final PropertyDescriptor IMPORT_OUTPUT_PROPERTY_DESCRIPTOR = StaticDescriptor.create(IMPORT_OUTPUT_PROPERTY);
    public static final PropertyDescriptor IMPORT_FILESIZE_PROPERTY_DESCRIPTOR = StaticDescriptor.create( IMPORT_FILESIZE_PROPERTY );
    
    public static final String EXPORT_OUTPUT_PROPERTY = "exportPath";
    public static final String EXPORT_FORMAT_PROPERTY = "exportFormat";
    public static final PropertyDescriptor EXPORT_OUTPUT_PROPERTY_DESCRIPTOR = StaticDescriptor.create( EXPORT_OUTPUT_PROPERTY );
    public static final PropertyDescriptor EXPORT_FORMAT_PROPERTY_DESCRIPTOR = StaticDescriptor.create( EXPORT_FORMAT_PROPERTY );

    protected String type;
    protected DataElementPath source;
    protected String data;
    protected JobControl jobControl;
    protected String user;
    protected long startTime;
    protected long endTime;
    protected StringBuffer logInfo;
    protected DynamicPropertySet attributes = null;
    protected Journal journal = null;
    protected Task task;

    /**
     * Properties which will not be serialized
     */
    protected Map<String, Object> transientProperties = new HashMap<>();

    public TaskInfo(DataCollection origin, String name)
    {
        this(origin, name, null, null, null, null);
    }

    public TaskInfo(DataCollection origin, String name, String type, DataElementPath source, JobControl jobControl, Task task)
    {
        super(origin, name);
        this.type = type;
        this.source = source;
        this.jobControl = jobControl;
        this.task = task;
        this.startTime = System.currentTimeMillis();
        this.endTime = -1;
    }

    public TaskInfo(DataCollection origin, String type, DataElementPath source, JobControl jobControl, Task task)
    {
        this(origin, generateName(origin), type, source, jobControl, task == null ? generateTask(type, source, jobControl) : task);
    }

    /**
     * Make clone of {@link TaskInfo}
     */
    public TaskInfo clone(DataCollection origin, String name)
    {
        TaskInfo result = new TaskInfo(origin, name);
        result.setType(type);
        result.setSource(source);
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setLogInfo(logInfo.toString());
        result.setData(data);
        Iterator<DynamicProperty> iter = attributes.iterator();
        while( iter.hasNext() )
        {
            result.getAttributes().add(iter.next());
        }
        return result;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public DataElementPath getSource()
    {
        return source;
    }

    public void setSource(DataElementPath source)
    {
        this.source = source;
    }

    public String getData()
    {
        return data;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    public void setEndTime()
    {
        setEndTime(System.currentTimeMillis());
    }

    public String getLogInfo()
    {
        if( logInfo == null )
            return null;
        return logInfo.toString();
    }

    public void setLogInfo(String logInfo)
    {
        this.logInfo = logInfo == null ? null : new StringBuffer(logInfo);
    }
    
    public JobControl getJobControl()
    {
        return jobControl;
    }
    
    public Task getTask()
    {
        return this.task;
    }
    
    public Journal getJournal()
    {
        return journal;
    }

    public void setJournal(Journal journal)
    {
        this.journal = journal;
    }

    public DynamicPropertySet getAttributes()
    {
        if( attributes == null )
        {
            attributes = new DynamicPropertySetAsMap();
        }
        return attributes;
    }
    
    public void setAttributes(DynamicPropertySet attributes)
    {
        this.attributes = attributes;
    }

    protected StringBufferListener logAppender = null;
    public void addAsAppender(BiosoftLogger log)
    {
        if( logInfo == null )
        {
            logInfo = new StringBuffer();
        }
        logAppender = new StringBufferListener(logInfo, log);
    }
    
    public void setTaskComplete()
    {
        if( logAppender != null )
        {
            //remove current appender from log listeners
            logAppender.close();
        }
    }

    /**
     * Frees task resources
     */
    protected void freeTask()
    {
        task = null;
        if(!(jobControl instanceof StubJobControl))
        {
            jobControl = new StubJobControl(jobControl);
        }
    }

    private static AtomicLong lastTaskId = new AtomicLong(1);
    /**
     * Generate unique task name
     */
    public static String generateName(DataCollection origin)
    {
        long taskId = lastTaskId.getAndIncrement();
        return String.format("%s %d", new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()), taskId);
    }
    
    private static RunnableTask generateTask(String type, DataElementPath source, JobControl jobControl)
    {
        return jobControl == null ? null : new RunnableTask(type + ":" + ( source == null ? "null" : source.getName() ) + "(user: "
                + SecurityManager.getSessionUser() + ")", jobControl);
    }

    /**
     * Set transient property (replacing old one wit the same key if any)
     * @param key
     * @param value
     */
    public void setTransient(String key, Object value)
    {
        transientProperties.put(key, value);
    }
    
    /**
     * Retrieve transient property by key
     * @param key
     * @return
     */
    public Object getTransient(String key)
    {
        return transientProperties.get(key);
    }

    @Override
    public String toString()
    {
        return getName()+":"+getSource()+" ("+getUser()+")";
    }
}
