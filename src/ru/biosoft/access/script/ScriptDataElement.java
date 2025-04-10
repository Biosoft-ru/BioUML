package ru.biosoft.access.script;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.exception.ProductNotAvailableException;
import ru.biosoft.access.log.BiosoftLogger;
import ru.biosoft.access.log.DefaultBiosoftLogger;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.task.JobControlTask;
import ru.biosoft.access.task.Task;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.beans.DynamicPropertySetSupport;
import ru.biosoft.jobcontrol.JobControl;

/**
 * @author lan
 *
 */
@PropertyName("script")
public abstract class ScriptDataElement extends TextDataElement
{
    public ScriptDataElement(String name, DataCollection<?> origin, String content)
    {
        super(name, origin, content);
    }

    public ScriptDataElement(String name, DataCollection<?> origin)
    {
        super(name, origin);
    }

    /**
     * Executes the script with given content and given environment
     * @param content
     * @param env
     * @param sessionContext if true then special one-per-session context will be used, otherwise new context will be created
     * @return result of script execution if available
     */
    public String execute(String content, ScriptEnvironment env, boolean sessionContext)
    {
        Map<String, Object> scope = Collections.emptyMap();
        return execute( content, env, scope, scope, sessionContext );
    }

    public TaskInfo createTask(String content, ScriptEnvironment env, boolean sessionContext)
    {
        try
        {
            ScriptTypeRegistry.checkProduct(this);
        }
        catch( ProductNotAvailableException e )
        {
            env.error(e.getMessage());
            return null;
        }
        Map<String, Object> scope = Collections.emptyMap();
        TaskInfo task = createTask(content, env, scope, scope, sessionContext, false);
        return task;
    }

    protected TaskInfo createTask(String content, ScriptEnvironment env, Map<String, Object> scope,
            Map<String, Object> outVars, boolean sessionContext, boolean willJoin)
    {
        BiosoftLogger log = new DefaultBiosoftLogger();
        LogScriptEnvironment logEnv = new LogScriptEnvironment( log );
        JobControl jc = createJobControl( content, new ComplexScriptEnvironment( env, logEnv ), scope, outVars, sessionContext );
        TaskManager taskManager = TaskManager.getInstance();
        String title = ScriptTypeRegistry.getScriptType(this).getTitle();
        if( !sessionContext )
            title += " " + getName();
        title += " (" + SecurityManager.getSessionUser() + ")";
        TaskInfo taskInfo;
        JobControlTask task = new JobControlTask( title, jc ) {
            @Override
            public double estimateWeight()
            {
                // Mark task as fast if it's going to be joined in the current thread
                return willJoin ? 0 : 1;
            }
        };
        if( sessionContext )
        {
            taskInfo = taskManager.addTask( TaskInfo.SCRIPT, null, jc, log, JournalRegistry.getCurrentJournal(),
                    new DynamicPropertySetSupport(), false, task );
            taskInfo.setData(content);
        }
        else
        {
            taskInfo = taskManager.addTask( TaskInfo.SCRIPT_DOCUMENT, DataElementPath.create( this ), jc, log,
                    JournalRegistry.getCurrentJournal(), new DynamicPropertySetSupport(), false, task );
        }
        return taskInfo;
    }

    protected String doExecute(String content, ScriptEnvironment env, Map<String, Object> scope, Map<String, Object> outVars,
            boolean sessionContext)
    {
        try
        {
            TaskInfo taskInfo = createTask(content, env, scope, outVars, sessionContext, true);
            ScriptJobControl jc = (ScriptJobControl)taskInfo.getJobControl();
            TaskManager.getInstance().runTask(taskInfo);
            Task task = taskInfo.getTask();
            if(task != null)//taskInfo.task can be freed by taskManager
                task.join();
            return jc.getResult();
        }
        catch( CancellationException e )
        {
            env.error("Cancelled by user request");
        }
        catch( ExecutionException e )
        {
            Throwable cause = e.getCause();
            handleException( env, cause );
        }
        catch( Throwable t )
        {
            env.error(ExceptionRegistry.log(t));
        }
        return null;
    }

    public String execute(String content, ScriptEnvironment env, Map<String, Object> scope, Map<String, Object> outVars, boolean sessionContext)
    {
        try
        {
            ScriptTypeRegistry.checkProduct(this);
        }
        catch(ProductNotAvailableException e)
        {
            env.error(e.getMessage());
            return null;
        }
        return doExecute(content, env, scope, outVars, sessionContext);
    }

    protected void handleException(ScriptEnvironment env, Throwable ex)
    {
        env.error(ExceptionRegistry.log(ex));
    }

    abstract protected ScriptJobControl createJobControl(String content, ScriptEnvironment env, Map<String, Object> scope, Map<String, Object> outVars,
            boolean sessionContext);
}
