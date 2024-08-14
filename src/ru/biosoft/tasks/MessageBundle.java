package ru.biosoft.tasks;

import java.util.ListResourceBundle;

import javax.swing.Action;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            // TaskInfo constants
            {"CN_TASK_INFO"          , "Task element"},
            {"CD_TASK_INFO"          , "Task element"},
        
            {"PN_IDENTIFIER"         , "ID"},
            {"PD_IDENTIFIER"         , "Element ID"},
            {"PN_TYPE"               , "Type"},
            {"PD_TYPE"               , "Element type"},
            {"PN_SOURCE"             , "Source"},
            {"PD_SOURCE"             , "Element source"},
            {"PN_DATA"               , "Data"},
            {"PD_DATA"               , "Element data"},
            {"PN_ATTRIBUTES"         , "Properties"},
            {"PD_ATTRIBUTES"         , "Properties"},
            {"PN_START"              , "Start"},
            {"PD_START"              , "Start time"},
            {"PN_END"                , "End"},
            {"PD_END"                , "End time"},
            {"PN_LOG"                , "Log info"},
            {"PD_LOG"                , "Log info"},
            
            {"PN_STATUS"             , "Status"},
            {"PD_STATUS"             , "Task status"},
            
            // TasksViewPart actions
            { TasksViewPart.STOP_ACTION    + Action.SMALL_ICON           , "stop.gif"},
            { TasksViewPart.STOP_ACTION    + Action.NAME                 , "Stop"},
            { TasksViewPart.STOP_ACTION    + Action.SHORT_DESCRIPTION    , "Stop task execution"},
            { TasksViewPart.STOP_ACTION    + Action.LONG_DESCRIPTION     , "Stop task execution"},
            { TasksViewPart.STOP_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-tasks-stop"},
            
            { TasksViewPart.PAUSE_ACTION   + Action.SMALL_ICON           , "pause.gif"},
            { TasksViewPart.PAUSE_ACTION   + Action.NAME                 , "Pause"},
            { TasksViewPart.PAUSE_ACTION   + Action.SHORT_DESCRIPTION    , "Temporary stop task execution"},
            { TasksViewPart.PAUSE_ACTION   + Action.LONG_DESCRIPTION     , "Temporary stop task execution"},
            { TasksViewPart.PAUSE_ACTION   + Action.ACTION_COMMAND_KEY   , "cmd-tasks-pause"},
            
            { TasksViewPart.RESUME_ACTION  + Action.SMALL_ICON           , "resume.gif"},
            { TasksViewPart.RESUME_ACTION  + Action.NAME                 , "Resume"},
            { TasksViewPart.RESUME_ACTION  + Action.SHORT_DESCRIPTION    , "Resume task execution"},
            { TasksViewPart.RESUME_ACTION  + Action.LONG_DESCRIPTION     , "Resume task execution"},
            { TasksViewPart.RESUME_ACTION  + Action.ACTION_COMMAND_KEY   , "cmd-tasks-resume"},
            
            { TasksViewPart.REMOVE_ACTION  + Action.SMALL_ICON           , "remove.gif"},
            { TasksViewPart.REMOVE_ACTION  + Action.NAME                 , "Remove"},
            { TasksViewPart.REMOVE_ACTION  + Action.SHORT_DESCRIPTION    , "Remove task from list"},
            { TasksViewPart.REMOVE_ACTION  + Action.LONG_DESCRIPTION     , "Remove task from list"},
            { TasksViewPart.REMOVE_ACTION  + Action.ACTION_COMMAND_KEY   , "cmd-tasks-remove"},
            
            { TasksViewPart.LOGINFO_ACTION + Action.SMALL_ICON           , "log.gif"},
            { TasksViewPart.LOGINFO_ACTION + Action.NAME                 , "Log info"},
            { TasksViewPart.LOGINFO_ACTION + Action.SHORT_DESCRIPTION    , "Show log info"},
            { TasksViewPart.LOGINFO_ACTION + Action.LONG_DESCRIPTION     , "Show log info"},
            { TasksViewPart.LOGINFO_ACTION + Action.ACTION_COMMAND_KEY   , "cmd-tasks-log"},
            
            // Log info panel constants
            {"LOG_PANE_TITLE", "Log info"},
            {"LOG_PANE_CLOSE", "Close"},
        };
    }
}
