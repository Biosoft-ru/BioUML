package biouml.plugins.research.workflow.engine;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.JobControlListener;

/**
 * {@link WorkflowElement} implementation for simple data element
 */
public class SimpleNodeElement extends WorkflowElement
{
    protected boolean complete = false;

    public SimpleNodeElement(DynamicProperty statusProperty)
    {
        super(statusProperty);
    }

    @Override
    public boolean isComplete()
    {
        return complete;
    }

    @Override
    public void startElementExecution(JobControlListener listener)
    {
        complete = true;
        setPreparedness(100);
        listener.jobTerminated(null);
    }
}
