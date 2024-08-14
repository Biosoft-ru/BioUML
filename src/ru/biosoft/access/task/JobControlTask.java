package ru.biosoft.access.task;

import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

/**
 * @author lan
 *
 */
public class JobControlTask extends AbstractTask
{
    private JobControl jobControl;

    public JobControlTask(String name, JobControl jobControl)
    {
        super(name);
        this.jobControl = jobControl;
        jobControl.addListener(new JobControlListenerAdapter()
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

    @Override
    protected void doRun()
    {
        jobControl.run();
    }
}
