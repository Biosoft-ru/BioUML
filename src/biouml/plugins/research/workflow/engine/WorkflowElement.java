package biouml.plugins.research.workflow.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlListener;

/**
 * Base class for workflow elements
 */
public abstract class WorkflowElement
{
    //TODO: jobcontrol check
    protected static final Logger log = Logger.getLogger( WorkflowElement.class.getName() );
    protected List<WorkflowElement> dependencies;
    protected List<WorkflowElement> listeners;
    private boolean started = false;
    protected DynamicProperty statusProperty;
    protected int preparedness = 0;
    protected boolean ignoreFail;

    public WorkflowElement(DynamicProperty statusProperty)
    {
        this.dependencies = new ArrayList<>();
        this.statusProperty = statusProperty;
    }

    /**
     * Add element dependence
     */
    public void addDependence(WorkflowElement element)
    {
        dependencies.add(element);
    }

    /**
     * Relative time cost of the element (used to display progress)
     */
    public double getWeight()
    {
        return 0;
    }

    /**
     * Try to terminate element execution by user request
     */
    public void terminate()
    {
    }

    /**
     * Check if execution complete
     */
    public abstract boolean isComplete();

    /**
     * Start asynchronous execution
     */
    public abstract void startElementExecution(JobControlListener listener);

    /**
     * Check if all dependences are complete
     */
    public boolean readyToExecute()
    {
        for( WorkflowElement ae : dependencies )
        {
            if( !ae.isComplete() )
            {
                return false;
            }
        }
        return true;
    }

    public boolean isStarted()
    {
        return started;
    }

    public void setStarted(boolean started)
    {
        this.started = started;
    }

    protected void setPreparedness(int preparedness)
    {
        if(statusProperty != null)
            statusProperty.setValue(preparedness);
        this.preparedness = preparedness;
    }

    public int getPreparedness()
    {
        return isComplete()?100:preparedness;
    }

    public JobControl getJobControl()
    {
        return null;
    }

    public void addListener( WorkflowElement element )
    {
        if(listeners == null)
            listeners = new ArrayList<>();
        listeners.add(element);
    }

    public boolean isIgnoreFail()
    {
        return ignoreFail;
    }
    public void setIgnoreFail(boolean ignoreFail)
    {
        this.ignoreFail = ignoreFail;
    }
}
