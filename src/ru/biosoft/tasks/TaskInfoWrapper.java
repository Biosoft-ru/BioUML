package ru.biosoft.tasks;

import java.text.SimpleDateFormat;
import java.util.Date;

import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.application.Application;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Wrapper for task representation
 */
public class TaskInfoWrapper extends DataElementSupport
{
    protected TaskInfo taskInfo;

    public TaskInfoWrapper()
    {
        //special for ComponentFactory getModel( Class c ) method
        super("", null);
    }

    public TaskInfoWrapper(TaskInfo taskInfo)
    {
        super(taskInfo.getName(), null);
        this.taskInfo = taskInfo;
    }

    public TaskInfo getTask()
    {
        return taskInfo;
    }

    public String getType()
    {
        if( taskInfo == null )
            return null;
        return taskInfo.getType();
    }

    public String getSource()
    {
        if( taskInfo == null || taskInfo.getSource() == null )
            return null;
        if(Application.getApplicationFrame() == null)
        {   // web edition - return link
            return "<a href=\"#de=" + TextUtil2.encodeURL(taskInfo.getSource().toString()).replace("+", "%20") + "&taskID="
                    + TextUtil2.encodeURL(taskInfo.getName()).replace("+", "%20") + "\">" + taskInfo.getSource().getName() + "</a>";
        }
        return taskInfo.getSource().getName();
    }

    public String getStartTimeStr()
    {
        if( taskInfo == null )
            return null;
        long startTime = taskInfo.getStartTime();
        if( startTime > 0 )
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            String endTimeStr = sdf.format(new Date(startTime));
            return endTimeStr;
        }
        return "-";
    }

    public String getEndTimeStr()
    {
        if( taskInfo == null )
            return null;
        long endTime = taskInfo.getEndTime();
        if( endTime > 0 )
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            String endTimeStr = sdf.format(new Date(endTime));
            return endTimeStr;
        }
        return "-";
    }

    public String getStatus()
    {
        if( taskInfo == null )
            return null;
        String result = taskInfo.getJobControl().getTextStatus();
        if( taskInfo.getJobControl().getStatus() == JobControl.RUNNING )
        {
            result += ": " + taskInfo.getJobControl().getPreparedness() + "%";
        }
        return result;
    }

    public String getLogInfo()
    {
        if( taskInfo == null )
            return null;
        return taskInfo.getLogInfo();
    }
}