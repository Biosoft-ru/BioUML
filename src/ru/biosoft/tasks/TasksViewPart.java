package ru.biosoft.tasks;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JScrollPane;

import java.util.logging.Logger;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.swing.table.DefaultRowModel;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import ru.biosoft.jobcontrol.JobControl;
import com.developmentontheedge.log.LogTextPanel;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.util.OkCancelDialog;

public class TasksViewPart extends ViewPartSupport implements TaskManagerListener
{
    protected Logger log = Logger.getLogger(TasksViewPart.class.getName());

    protected TabularPropertyInspector taskTable;
    protected TaskManager taskManager;

    public static final String STOP_ACTION = "task-stop";
    public static final String PAUSE_ACTION = "task-pause";
    public static final String RESUME_ACTION = "task-resume";
    public static final String REMOVE_ACTION = "task-remove";
    public static final String LOGINFO_ACTION = "task-loginfo";

    protected Action[] actions;
    protected Action stopAction = new StopAction();
    protected Action pauseAction = new PauseAction();
    protected Action resumeAction = new ResumeAction();
    protected Action removeAction = new RemoveAction();
    protected Action logInfoAction = new LogInfoAction();

    public TasksViewPart()
    {
        taskTable = new TabularPropertyInspector();
        JScrollPane scrollPane = new JScrollPane(taskTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        this.taskManager = TaskManager.getInstance();
        this.taskManager.addListener(this);

        taskTable.addListSelectionListener(e -> {
            Object model = taskTable.getModelOfSelectedRow();
            if( model != null )
            {
                enableActions( ( (TaskInfoWrapper)model ).getTask());
            }
        });
    }

    @Override
    public void explore(Object model, Document document)
    {
        update();
    }

    @Override
    public boolean canExplore(Object model)
    {
        return true;
    }

    @Override
    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(stopAction, STOP_ACTION);
            initializer.initAction(pauseAction, PAUSE_ACTION);
            initializer.initAction(resumeAction, RESUME_ACTION);
            initializer.initAction(removeAction, REMOVE_ACTION);
            initializer.initAction(logInfoAction, LOGINFO_ACTION);

            enableActions(null);
            //Resume and pause actions are not supported
            actions = new Action[] {/*resumeAction, pauseAction,*/ stopAction, removeAction, logInfoAction};
        }
        return actions;
    }

    protected void enableActions(TaskInfo selectedTask)
    {
        boolean running = ( selectedTask != null ) && ( selectedTask.getJobControl().getStatus() == JobControl.RUNNING );
        stopAction.setEnabled(running);
        pauseAction.setEnabled(running);

        boolean paused = ( selectedTask != null ) && ( selectedTask.getJobControl().getStatus() == JobControl.PAUSED );
        resumeAction.setEnabled(paused);

        removeAction.setEnabled(selectedTask != null);
    }

    protected void update()
    {
        DataCollection<?> tasks = taskManager.getTasksInfo();
        if( tasks.getSize() > 0 )
        {
            taskTable.explore(tasks.iterator());
        }
        else
        {
            taskTable.explore(new DefaultRowModel(), new TaskInfoWrapper(new TaskInfo(null, null, null, null, null)),
                    PropertyInspector.SHOW_USUAL);
        }
    }

    //
    //TaskManagerListener functions
    //

    @Override
    public void taskAdded(TaskInfo ti)
    {
        update();
    }

    @Override
    public void taskRemoved(TaskInfo ti)
    {
        update();
    }

    @Override
    public void taskChanged(TaskInfo ti)
    {
        taskTable.repaint();
    }

    //
    //Actions
    //
    private abstract class TaskAction extends AbstractAction
    {
        public TaskAction(String name)
        {
            super(name);
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            Object model = taskTable.getModelOfSelectedRow();
            if( model != null )
            {
                doAction(((TaskInfoWrapper)model).getTask());
            }
        }

        abstract protected void doAction(TaskInfo task);
    }

    private class StopAction extends TaskAction
    {
        public StopAction()
        {
            super(STOP_ACTION);
        }

        @Override
        public void doAction(TaskInfo task)
        {
            taskManager.stopTask(task);
        }
    }

    private class PauseAction extends TaskAction
    {
        public PauseAction()
        {
            super(PAUSE_ACTION);
        }

        @Override
        public void doAction(TaskInfo task)
        {
            task.getJobControl().pause();
        }
    }

    private class ResumeAction extends TaskAction
    {
        public ResumeAction()
        {
            super(RESUME_ACTION);
        }

        @Override
        public void doAction(TaskInfo task)
        {
            task.getJobControl().resume();
        }
    }

    private class RemoveAction extends TaskAction
    {
        public RemoveAction()
        {
            super(REMOVE_ACTION);
        }

        @Override
        public void doAction(TaskInfo task)
        {
            taskManager.stopTask(task);
            taskManager.removeTask(task);
        }
    }

    private class LogInfoAction extends TaskAction
    {
        public LogInfoAction()
        {
            super(LOGINFO_ACTION);
        }

        @Override
        public void doAction(TaskInfo ti)
        {
            MessageBundle resources = new MessageBundle();
            LogTextPanel logPanel = new LogTextPanel();
            logPanel.setText(ti.getLogInfo());
            OkCancelDialog dialog = new OkCancelDialog(Application.getApplicationFrame(), resources.getString("LOG_PANE_TITLE"),
                    logPanel, null, resources.getString("LOG_PANE_CLOSE"));
            dialog.setSize(500, 400);
            dialog.setLocationRelativeTo(Application.getApplicationFrame());
            dialog.setVisible(true);
        }
    }
}
