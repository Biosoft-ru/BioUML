package ru.biosoft.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.log.BiosoftLogger;
import ru.biosoft.access.log.JULLoggerAdapter;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.task.Task;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisTask;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.util.LazyValue;
import ru.biosoft.util.Pair;

/**
 * Base class to access task functionality
 */
public class TaskManager implements JobControlListener
{
    protected static final Logger log = Logger.getLogger(TaskManager.class.getName());

    private static final LazyValue<TaskManager> instance = new LazyValue<>("Task manager", TaskManager::new);

    public static TaskManager getInstance()
    {
        return instance.get();
    }

    protected DataCollection<TaskInfo> tasks;
    protected Map<String, TaskInfo> currentTasks = new ConcurrentHashMap<>();
    
    public static final String EXECUTORS_PROPERTY = "Executors";
    private List<TaskExecutor> executors = new ArrayList<>();

    public TaskManager()
    {
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "Tasks");
        properties.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, TaskInfo.class.getName());
        tasks = new VectorDataCollection<>(null, properties);
        
        initExecutors();
    }

    public void initExecutors()
    {
        boolean hasJVMExecutor = false;
        Preferences preferences = Application.getPreferences();
        Preferences executorPreferences = preferences==null?null:preferences.getPreferencesValue(EXECUTORS_PROPERTY);
        if(executorPreferences != null)
        {
            for(DynamicProperty dp : executorPreferences)
            {
                Object value = dp.getValue();
                if(value instanceof DynamicPropertySet)
                {
                    DynamicPropertySet dps = (DynamicPropertySet)value;
                    DynamicProperty clDP = dps.getProperty( "class" );
                    if(clDP == null)
                        throw new RuntimeException("Missing class property in executor config: " + dp.getName());
                    String clStr = (String)clDP.getValue();
                    Class<? extends TaskExecutor> cl = ClassLoading.loadSubClass( clStr, TaskExecutor.class);
                    if(cl.equals( JVMExecutor.class ))
                        hasJVMExecutor = true;
                    TaskExecutor executor;
                    try
                    {
                        executor = cl.newInstance();
                    }
                    catch( InstantiationException|IllegalAccessException e )
                    {
                        throw new RuntimeException("Invalid executor: " + dp.getName(), e);
                    }
                    executor.init( dps );
                    executors.add( executor );
                }else
                    throw new RuntimeException("Invalid executors configuration");
            }
        }
        if(!hasJVMExecutor)
            executors.add( new JVMExecutor() );
    }

    public void init(final Properties properties) throws Exception
    {
        SecurityManager.runPrivileged(() -> {
            Properties collectionProperties = new Properties(properties);
            collectionProperties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "Tasks");
            collectionProperties.setProperty(SqlDataCollection.SQL_TRANSFORMER_CLASS, TasksSqlTransformer.class.getName());
            try
            {
                tasks = new SqlDataCollection<>(null, collectionProperties);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Unable to initialize tasks collection", e);
            }
            return null;
        });
    }

    public Map<String, List<TaskInfo>> getAllInterruptedAnalysesByUser()
    {
        Map<String, List<TaskInfo>> result = new HashMap<>();
        for(TaskInfo ti : tasks)
        {
            if(!ti.getType().equals( TaskInfo.ANALYSIS ))
                continue;
            Object interrupted = ti.getTransient( "interrupted" );
            if(interrupted instanceof Boolean && (Boolean)interrupted)
                result
                    .computeIfAbsent( ti.getUser(), k->new ArrayList<>() )
                    .add( ti );
        }
        return result;
    }

    private Map<String, List<TaskInfo>> getInterruptedTasks(Set<String> taskNames, List<TaskInfo> err)
    {
        Map<String, List<TaskInfo>> interruptedTasksByUser = new HashMap<>();
        for(String taskName : taskNames)
        {
            TaskInfo ti;
            try
            {
                ti = tasks.get( taskName );
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not get task " + taskName, e);
                err.add( new TaskInfo( null, taskName ) );
                continue;
            }
            if(ti == null || !ti.getType().equals( TaskInfo.ANALYSIS ) )
            {
                err.add( ti );
                continue;
            }
            Object interrupted = ti.getTransient( "interrupted" );
            if(!(interrupted instanceof Boolean) || !( (Boolean)interrupted))
            {
                err.add( ti );
                continue;
            }
            interruptedTasksByUser
                .computeIfAbsent( ti.getUser(), k->new ArrayList<>() )
                .add( ti );
        }
        return interruptedTasksByUser;
    }

    /**
     * @param adminUser
     * @param adminPass
     * @return pair of <success,failure> restarted tasks
     */
    public Pair<List<TaskInfo>, List<TaskInfo>> restartAllInterruptedTasks(String adminUser, String adminPass)
    {
        Map<String, List<TaskInfo>> interruptedTasksByUser = getAllInterruptedAnalysesByUser();

        List<TaskInfo> suc = new ArrayList<>();
        List<TaskInfo> err = new ArrayList<>();
        for( Map.Entry<String, List<TaskInfo>> entry : interruptedTasksByUser.entrySet() )
        {
            String user = entry.getKey();
            List<TaskInfo> tasks = entry.getValue();
            restartTasksForUser( user, tasks, adminUser, adminPass, suc, err);
        }
        return  new Pair<>( suc, err );
    }

    public Pair<List<TaskInfo>, List<TaskInfo>> restartInterruptedTasks(String adminUser, String adminPass, Set<String> taskNames)
    {
        List<TaskInfo> suc = new ArrayList<>();
        List<TaskInfo> err = new ArrayList<>();

        Map<String, List<TaskInfo>> interruptedTasksByUser = getInterruptedTasks( taskNames, err );

        for( Map.Entry<String, List<TaskInfo>> entry : interruptedTasksByUser.entrySet() )
        {
            String user = entry.getKey();
            List<TaskInfo> tasks = entry.getValue();
            restartTasksForUser( user, tasks, adminUser, adminPass, suc, err);
        }
        return  new Pair<>( suc, err );
    }

    public void restartTasksForUser(String user, List<TaskInfo> tasks, String adminUser, String adminPass, List<TaskInfo> suc, List<TaskInfo> err)
    {
        try
        {
            String sessionId = SecurityManager.generateSessionId();
            SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
            SecurityManager.commonLogin( adminUser + "$" + user , adminPass, null, null );
        }
        catch(Throwable t)
        {
            err.addAll( tasks );
            log.log( Level.SEVERE, "Can not restart interrupted tasks for user=" + user, t );
            return;
        }
        try {
            for(TaskInfo ti : tasks)
            {
                try {
                    ti = recoverAnalysisTask( ti );
                    suc.add( ti );
                } catch(Throwable t)
                {
                    log.log( Level.SEVERE, "Can not restart task " + ti.getName(), t );
                    err.add( ti );
                }
            }
        } finally {
            SecurityManager.commonLogout();
        }
    }

    public TaskInfo recoverAnalysisTask(TaskInfo ti)
    {
        ti = renewAnalysisTaskInfo(ti);

        tasks.put(ti);
        currentTasks.put(ti.getName(), ti);

        AnalysisJobControl ajc = (AnalysisJobControl)ti.getJobControl();
        ajc.setRecoverMode(true);
        ajc.addListener(this);

        runTask(ti);
        return ti;
    }

    private TaskInfo renewAnalysisTaskInfo(TaskInfo ti)
    {
        DynamicPropertySet attrs = ti.getAttributes();
        AnalysisMethod method = AnalysisDPSUtils.getAnalysisMethodByNode( attrs );
        AnalysisParameters params = AnalysisDPSUtils.readParametersFromAttributes( attrs );
        method.setParameters( params );
        AnalysisTask task = new AnalysisTask( method );

        AnalysisJobControl jobControl = method.getJobControl();

        Object paused = ti.getTransient( "paused" );
        if(paused instanceof Boolean && (Boolean)paused)
            jobControl.setPauseOnStart( true );

        TaskInfo result = new TaskInfo( null, ti.getName(), ti.getType(), ti.getSource(), jobControl, task );
        result.setStartTime( ti.getStartTime() );
        result.setJournal( JournalRegistry.getCurrentJournal() );
        result.setLogInfo( ti.getLogInfo() );
        result.setUser( ti.getUser() );
        result.setAttributes( ti.getAttributes() );
        result.addAsAppender( new JULLoggerAdapter( method.getLogger() ) );

        return result;
    }

    public TaskInfo getTask(String name)
    {
        try
        {
            return tasks.get(name);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not get task", e);
        }
        return null;
    }

    /**
     * Add new task to task manager
     */
    public TaskInfo addTask(String type, DataElementPath source, JobControl jobControl, BiosoftLogger logger, Journal journal,
            DynamicPropertySet attributes, boolean autorun, Task task)
    {
        try
        {
            TaskInfo taskInfo = new TaskInfo(null, type, source, jobControl, task);
            if( attributes != null )
            {
                for( DynamicProperty attribute : attributes )
                {
                    taskInfo.getAttributes().add(attribute);
                }
            }
            taskInfo.setJournal(journal);
            taskInfo.setUser(SecurityManager.getSessionUser());

            tasks.put(taskInfo);
            currentTasks.put(taskInfo.getName(), taskInfo);
            if( logger != null )
            {
                taskInfo.addAsAppender(logger);
            }
            fireTaskAdded(taskInfo);
            jobControl.addListener(this);

            if( autorun )
            {
                //start task execution
                runTask(taskInfo);
            }

            return taskInfo;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not add task", e);
        }
        return null;
    }

    public TaskInfo addAnalysisTask(AnalysisMethod analysis, JobControl jobControl, boolean autorun, String type)
    {
        DynamicPropertySet dpsParameters = new DynamicPropertySetSupport();
        AnalysisDPSUtils.writeParametersToNodeAttributes(analysis.getName(), analysis.getParameters(), dpsParameters);
        return addTask( type, DataElementPath.create( AnalysisMethodRegistry.getMethodInfo( analysis.getName() ) ), jobControl,
                new JULLoggerAdapter( analysis.getLogger() ), JournalRegistry.getCurrentJournal(), dpsParameters, autorun,
                new AnalysisTask( analysis ) );
    }

    public TaskInfo addAnalysisTask(AnalysisMethod analysis, JobControl jobControl, boolean autorun)
    {
        return addAnalysisTask(analysis, jobControl, autorun, TaskInfo.ANALYSIS);
    }

    public TaskInfo addAnalysisTask(AnalysisMethod analysis, boolean autorun)
    {
        return addAnalysisTask(analysis, analysis.getJobControl(), autorun);
    }

    /**
     * Run task
     */
    public void runTask(TaskInfo taskInfo)
    {
        TaskExecutor bestTaskExecutor = executors.get(0);
        int bestScore = bestTaskExecutor.canExecute(taskInfo);

        for(int i = 1; i < executors.size(); i++)
        {
            TaskExecutor exec = executors.get( i );
             int score = exec.canExecute(taskInfo);
             if( score > bestScore )
             {
                 bestScore = score;
                 bestTaskExecutor = exec;
             }
        }
        
        bestTaskExecutor.submit(taskInfo);
    }

    /**
     * Break task execution
     */
    public void stopTask(TaskInfo taskInfo)
    {
        JobControl jobControl = taskInfo.getJobControl();
        if( jobControl != null )
        {
            jobControl.terminate();
        }
        fireTaskChanged(taskInfo);
    }

    /**
     * Pause task execution
     */
    public void pauseTask(TaskInfo taskInfo)
    {
        JobControl jobControl = taskInfo.getJobControl();
        if( jobControl != null )
        {
            jobControl.pause();
        }
        fireTaskChanged( taskInfo );
    }

    /**
     * Resume task execution
     */
    public void resumeTask(TaskInfo taskInfo)
    {
        JobControl jobControl = taskInfo.getJobControl();
        if( jobControl != null )
        {
            jobControl.resume();
        }
        fireTaskChanged( taskInfo );
    }

    /**
     * Remove task from task manager
     */
    public void removeTask(TaskInfo taskInfo)
    {
        try
        {
            JobControl jobControl = taskInfo.getJobControl();
            if( jobControl != null )
            {
                taskInfo.getJobControl().removeListener(this);
            }
            tasks.remove(taskInfo.getName());
            currentTasks.remove(taskInfo.getName());
            fireTaskRemoved(taskInfo);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not remove task", e);
        }
    }

    /**
     * Save changed task
     */
    public void updateTask(TaskInfo taskInfo)
    {
        fireTaskChanged( taskInfo );
    }

    /**
     * Get set of task descriptions for current user
     */
    public DataCollection<?> getTasksInfo()
    {
        try
        {
            if( tasks instanceof VectorDataCollection )
                return new TaskWrapperCollection(tasks);
            Properties userTasksProperties = new Properties(tasks.getInfo().getProperties());
            userTasksProperties.setProperty(TasksSqlTransformer.USER_MODE_PROPERTY, "true");
            SqlDataCollection<TaskInfo> dc = new SqlDataCollection<TaskInfo>(null, userTasksProperties)
            {
                @Override
                protected TaskInfo doGet(String name) throws Exception
                {
                    TaskInfo taskInfo = currentTasks.get(name);
                    if( taskInfo != null )
                    {
                        String user = taskInfo.getUser();
                        return user.equals( "*" ) || user.equals(SecurityManager.getSessionUser()) ? taskInfo : null;
                    }
                    return super.doGet(name);
                }
            };
            return new TaskWrapperCollection(dc);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to initialize user tasks: " + ExceptionRegistry.log(e));
        }
        return null;
    }

    public DataCollection<?> getTasksInfo(Class<? extends TasksSqlTransformer> transformerClass, Properties additionalProps,
            Predicate<TaskInfo> filter)
    {
        try
        {
            if( tasks instanceof VectorDataCollection )
                return new TaskWrapperCollection( tasks );
            Properties userTasksProperties = new Properties( tasks.getInfo().getProperties() );
            userTasksProperties.setProperty( SqlDataCollection.SQL_TRANSFORMER_CLASS, transformerClass.getName() );
            userTasksProperties.putAll( additionalProps );
            SqlDataCollection<TaskInfo> dc = new SqlDataCollection<TaskInfo>( null, userTasksProperties )
            {
                @Override
                protected TaskInfo doGet(String name) throws Exception
                {
                    if( filter == null )
                        return super.doGet( name );
                    TaskInfo taskInfo = currentTasks.get( name );
                    if( taskInfo != null )
                    {
                        if( filter.test( taskInfo ) )
                            return taskInfo;
                        return null;
                    }
                    return super.doGet( name );
                }
            };
            return new TaskWrapperCollection( dc );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Unable to initialize user tasks: " + ExceptionRegistry.log( e ) );
        }
        return null;
    }

    public List<TaskInfo> getAllRunningTasks()
    {
        if( !SecurityManager.isAdmin() )
            throw new SecurityException();
        List<TaskInfo> result = new ArrayList<>();
        for( TaskInfo ti : currentTasks.values() )
        {
            if( ! ( ti.getJobControl() instanceof StubJobControl ) && ti.getJobControl().getStatus() == JobControl.RUNNING )
                result.add(ti);
        }
        return result;
    }

    /**
     * Indicates if there are incomplete tasks
     */
    public boolean hasIncompleteTasks()
    {
        for( TaskInfo ti : currentTasks.values() )
        {
            if( ti.getJobControl().getStatus() == JobControl.RUNNING || ti.getJobControl().getStatus() == JobControl.PAUSED )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Stop all executable tasks
     */
    public void stopAllTasks()
    {
        for( TaskInfo ti : currentTasks.values() )
        {
            if( ti.getJobControl().getStatus() == JobControl.RUNNING || ti.getJobControl().getStatus() == JobControl.PAUSED )
            {
                ti.getJobControl().terminate();
            }
        }
    }

    //
    // Listener support
    //
    protected Set<TaskManagerListener> listeners = null;

    /**
     * Add task manager listener
     */
    public void addListener(TaskManagerListener listener)
    {
        if( listeners == null )
        {
            listeners = new HashSet<>();
        }
        listeners.add(listener);
    }

    /**
     * Remove task manager listener
     */
    public void removeListener(TaskManagerListener listener)
    {
        if( listeners != null )
        {
            listeners.remove(listener);
        }
    }

    /**
     * Fire task element addition
     */
    protected void fireTaskAdded(TaskInfo taskInfo)
    {
        if( listeners != null )
        {
            for( TaskManagerListener listener : listeners )
            {
                listener.taskAdded(taskInfo);
            }
        }
    }

    /**
     * Fire task element deletion
     */
    protected void fireTaskRemoved(TaskInfo taskInfo)
    {
        if( listeners != null )
        {
            for( TaskManagerListener listener : listeners )
            {
                listener.taskRemoved(taskInfo);
            }
        }
    }

    /**
     * Fire task element change
     */
    protected void fireTaskChanged(TaskInfo taskInfo)
    {
        if( taskInfo == null )
            return;
        try
        {
            tasks.put(taskInfo);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to update task " + taskInfo, e);
        }
        if( listeners != null )
        {
            for( TaskManagerListener listener : listeners )
            {
                listener.taskChanged(taskInfo);
            }
        }
    }

    //
    // JobControlListener implementation
    //

    @Override
    public void valueChanged(JobControlEvent event)
    {
        fireTaskChanged(getTask(event));
    }

    @Override
    public void jobStarted(JobControlEvent event)
    {
        fireTaskChanged(getTask(event));
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
        TaskInfo taskInfo = getTask(event);
        taskInfo.setTaskComplete();

        if( event.getStatus() == JobControl.COMPLETED )
        {
            //write to journal if no errors
            //TODO: write to journal also error tasks
            taskInfo.setEndTime();
            Journal journal = taskInfo.getJournal();
            if( journal != null )
            {
                journal.addAction(taskInfo);
            }
        }

        fireTaskChanged(taskInfo);
        JobControl jobControl = taskInfo.getJobControl();
        if( jobControl != null )
        {
            jobControl.removeListener(this);
        }
        taskInfo.freeTask();
    }

    @Override
    public void jobPaused(JobControlEvent event)
    {
        fireTaskChanged(getTask(event));
    }

    @Override
    public void jobResumed(JobControlEvent event)
    {
        fireTaskChanged(getTask(event));
    }

    @Override
    public void resultsReady(JobControlEvent event)
    {
        fireTaskChanged(getTask(event));
    }

    protected TaskInfo getTask(JobControlEvent event)
    {
        return getTask(event.getJobControl());
    }

    public TaskInfo getTask(JobControl jobControl)
    {
        for( TaskInfo ti : currentTasks.values() )
        {
            if( ti.getJobControl() == jobControl )
            {
                return ti;
            }
        }
        return null;
    }
}
