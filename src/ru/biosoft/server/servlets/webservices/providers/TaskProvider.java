package ru.biosoft.server.servlets.webservices.providers;

import java.util.Objects;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.server.Connection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
public class TaskProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String command = arguments.getAction();

        String rows = arguments.get("rows");
        if( rows != null )
        {
            String selectedNames[] = TextUtil2.split( rows, ',' );

            TaskManager taskManager = TaskManager.getInstance();
            String user = SecurityManager.getSessionUser();
            if( command.equals("stop_rows") )
            {
                for( String name : selectedNames )
                {
                    TaskInfo taskInfo = taskManager.getTask(name);
                    if( taskInfo != null && isSameUser( taskInfo.getUser(), user ) )
                    {
                        taskManager.stopTask(taskInfo);
                    }
                }
            }
            else if( command.equals("remove_rows") )
            {
                for( String name : selectedNames )
                {
                    TaskInfo taskInfo = taskManager.getTask(name);
                    if( taskInfo != null && isSameUser( taskInfo.getUser(), user ) )
                    {
                        taskManager.stopTask(taskInfo);
                        taskManager.removeTask(taskInfo);
                    }
                }
            }
        }
        else if( command.equals( "status" ) )
        {
            String taskID = arguments.get( "taskID" );
            if( taskID != null )
            {
                TaskManager taskManager = TaskManager.getInstance();
                String user = SecurityManager.getSessionUser();
                TaskInfo taskInfo = taskManager.getTask( taskID );
                if( taskInfo != null && isSameUser( taskInfo.getUser(), user ) )
                {
                    JobControl jobControl = taskInfo.getJobControl();
                    if( jobControl != null )
                    {
                        response.sendStatus( jobControl.getStatus(), jobControl.getPreparedness(), jobControl.getTextStatus() );
                        return;
                    }
                }
            }
        }
        else if( command.equals( "log_info" ) )
        {
            String taskID = arguments.get( "taskID" );
            if( taskID != null )
            {
                TaskManager taskManager = TaskManager.getInstance();
                String user = SecurityManager.getSessionUser();
                TaskInfo taskInfo = taskManager.getTask( taskID );
                if( taskInfo != null && Objects.equals( taskInfo.getUser(), user ) )
                {
                    response.sendString( taskInfo.getLogInfo() );
                    return;
                }
            }
        }
        else
        {
            String taskID = arguments.get( "taskID" );
            if( taskID != null )
            {
                TaskManager taskManager = TaskManager.getInstance();
                String user = SecurityManager.getSessionUser();
                TaskInfo taskInfo = taskManager.getTask( taskID );
                if( taskInfo != null && isSameUser( taskInfo.getUser(), user ) )
                {
                    if( command.equals( "hide_task" ) )
                    {
                        taskInfo.getAttributes().add( new DynamicProperty( "isTaskHidden", Boolean.class, true ) );
                        taskManager.updateTask( taskInfo );
                    }
                    else if( command.equals( "pause_task" ) )
                    {
                        taskManager.pauseTask( taskInfo );
                    }
                    else if( command.equals( "resume_task" ) )
                    {
                        Object interrupted = taskInfo.getTransient( "interrupted" );
                        Object paused = taskInfo.getTransient( "paused" );
                        if( interrupted instanceof Boolean && (Boolean)interrupted && paused instanceof Boolean && (Boolean)paused )
                        {
                            taskInfo.setTransient( "paused", false );
                            taskInfo.setTransient( "interrupted", false );
                            taskManager.recoverAnalysisTask( taskInfo );
                        }
                        else
                            taskManager.resumeTask( taskInfo );
                    }
                }
            }
        }

        response.send(new byte[0], Connection.FORMAT_SIMPLE);
    }

    private boolean isSameUser(String taskUser, String sessionUser)
    {
        if( Objects.equals( taskUser, sessionUser ) )
            return true;
        if( taskUser != null && sessionUser != null && taskUser.equalsIgnoreCase( sessionUser ) )
            return true;
        return false;
    }
}
