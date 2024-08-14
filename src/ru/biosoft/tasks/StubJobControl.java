package ru.biosoft.tasks;

import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlListener;

/**
 * JobControl which stores all statuses of original JobControl, but removes any functionality
 * Useful when you want to keep statuses, but discard the original JobControl to spare memory
 * @author lan
 */
public class StubJobControl implements JobControl
{
    private int status;
    private long createdTime;
    private long elapsedTime;
    private long endedTime;
    private long remainedTime;
    private long startedTime;
    private String textStatus;
    private int preparedness;
    
    public StubJobControl(JobControl jobControl)
    {
        this.status = jobControl.getStatus();
        this.textStatus = jobControl.getTextStatus();

        this.createdTime = jobControl.getCreatedTime();
        this.elapsedTime = jobControl.getElapsedTime();
        this.endedTime = jobControl.getEndedTime();
        this.remainedTime = jobControl.getRemainedTime();
        this.startedTime = jobControl.getStartedTime();
        this.preparedness = jobControl.getPreparedness();
    }
    

    
    public StubJobControl(int status, long createdTime, long elapsedTime, long endedTime, long remainedTime, long startedTime,
            String textStatus, int preparedness)
    {
        this.status = status;
        this.createdTime = createdTime;
        this.elapsedTime = elapsedTime;
        this.endedTime = endedTime;
        this.remainedTime = remainedTime;
        this.startedTime = startedTime;
        this.textStatus = textStatus;
        this.preparedness = preparedness;
    }



    @Override
    public void run()
    {
    }

    @Override
    public void pause()
    {
    }

    @Override
    public void resume()
    {
    }

    @Override
    public void terminate()
    {
    }

    @Override
    public int getStatus()
    {
        return status;
    }

    @Override
    public String getTextStatus()
    {
        return textStatus;
    }

    @Override
    public int getPreparedness()
    {
        return preparedness;
    }

    @Override
    public long getCreatedTime()
    {
        return createdTime;
    }

    @Override
    public long getRemainedTime()
    {
        return remainedTime;
    }

    @Override
    public long getElapsedTime()
    {
        return elapsedTime;
    }

    @Override
    public long getStartedTime()
    {
        return startedTime;
    }

    @Override
    public long getEndedTime()
    {
        return endedTime;
    }

    @Override
    public void addListener(JobControlListener listener)
    {
    }

    @Override
    public void removeListener(JobControlListener listener)
    {
    }

    @Override
    public void setPreparedness(int percent)
    {
    }
}