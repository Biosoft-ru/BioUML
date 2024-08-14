package biouml.plugins.research.workflow.engine;

import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.task.AbstractTask;

/**
 * @author lan
 *
 */
public class WorkflowElementTask extends AbstractTask
{
    private WorkflowElement element;
    private JobControlListener listener;

    public WorkflowElementTask(String name, WorkflowElement element, JobControlListener listener)
    {
        super("Workflow: "+name+" (user: "+SecurityManager.getSessionUser()+")");
        this.element = element;
        this.listener = listener;
        if(element.getJobControl() != null)
        {
            element.getJobControl().addListener(new JobControlListenerAdapter()
            {
                @Override
                public void jobTerminated(JobControlEvent event)
                {
                    if(event.getStatus() == JobControl.TERMINATED_BY_REQUEST)
                    {
                        stop();
                    }
                }
            });
        }
    }
    
    @Override
    protected void doRun()
    {
        element.startElementExecution(listener);
    }

    @Override
    public double estimateWeight()
    {
        return element.getWeight();
    }
}
