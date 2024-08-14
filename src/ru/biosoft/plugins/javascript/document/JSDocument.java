package ru.biosoft.plugins.javascript.document;


import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.mozilla.javascript.EvaluatorException;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.SaveDocumentAction;
import ru.biosoft.gui.ViewPartRegistry;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.plugins.javascript.ExecuteAction;
import ru.biosoft.plugins.javascript.ExecutePartAction;
import ru.biosoft.plugins.javascript.JSElement;
import ru.biosoft.plugins.javascript.MessageBundle;
import ru.biosoft.plugins.javascript.StepAction;
import ru.biosoft.plugins.javascript.StopAction;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.workbench.documents.RedoAction;
import ru.biosoft.workbench.documents.TextPaneUndoManager;
import ru.biosoft.workbench.documents.UndoAction;
import ru.biosoft.workbench.script.OutputViewPart;
import ru.biosoft.workbench.script.SwingScriptEnvironment;

public class JSDocument extends Document
{
    protected static final Logger log = Logger.getLogger(JSDocument.class.getName());

    protected JSPanel jsPanel;

    protected int currentLine = 0;

    private TaskInfo task;

    public JSDocument(JSElement jsElement)
    {
        super(jsElement, new TextPaneUndoManager());

        Dim dim = new Dim();
        dim.attachTo(new JSDocumentContextFactory());
        if( jsElement != null )
        {
            try
            {
                dim.compileScript(jsElement.getName(), jsElement.getContent());
            }
            catch(EvaluatorException ignore)
            {
                
            }

            jsPanel = new JSPanel(dim.sourceInfo(jsElement.getName()), dim, jsElement.getContent());

            viewPane = new ViewPane();
            viewPane.add(jsPanel);

        }
        else
        {
            jsPanel = new JSPanel(null, dim, null);
        }
        
        jsPanel.addUndoableEditListener(getUndoManager());
    }
    @Override
    public boolean isChanged()
    {
        return getUndoManager().canUndo();
    }

    // //////////////////////////////////////////////////////////////////////////
    // Properties
    //

    public int getCurrentLine()
    {
        return currentLine;
    }

    public JSPanel getJSPanel()
    {
        return jsPanel;
    }

    public void setCurrentLine(int currentLine)
    {
        this.currentLine = currentLine;
    }

    public JSElement getJSElement()
    {
        applyEditorChanges();
        return (JSElement)getModel();
    }

    @Override
    public String getDisplayName()
    {
        JSElement valueList = getJSElement();
        return valueList.getOrigin().getName() + " : " + valueList.getName();
    }

    private static boolean actionInitialized = false;
    @Override
    public Action[] getActions(ActionType actionType)
    {
        ActionManager actionManager = Application.getActionManager();
        if( !actionInitialized )
        {
            actionInitialized = true;

            ActionInitializer initializer = new ActionInitializer(MessageBundle.class, ru.biosoft.workbench.MessageBundle.class);

            //toolbar actions
            Action action = new UndoAction();
            actionManager.addAction(UndoAction.KEY, action);
            initializer.initAction(action, UndoAction.KEY);

            action = new RedoAction();
            actionManager.addAction(RedoAction.KEY, action);
            initializer.initAction(action, RedoAction.KEY);

            action = new ExecuteAction();
            actionManager.addAction(ExecuteAction.KEY, action);
            initializer.initAction(action, ExecuteAction.KEY);

            action = new StepAction();
            actionManager.addAction(StepAction.KEY, action);
            initializer.initAction(action, StepAction.KEY);

            action = new StopAction();
            actionManager.addAction(StopAction.KEY, action);
            initializer.initAction(action, StopAction.KEY);

            action = new ExecutePartAction();
            actionManager.addAction(ExecutePartAction.KEY, action);
            initializer.initAction(action, ExecutePartAction.KEY);

            updateActionsState();
        }
        if( actionType == ActionType.TOOLBAR_ACTION )
        {
            Action undoAction = actionManager.getAction(UndoAction.KEY);
            Action redoAction = actionManager.getAction(RedoAction.KEY);
            Action executeAction = actionManager.getAction(ExecuteAction.KEY);
            executeAction.putValue(ExecuteAction.DOCUMENT_ELEMENT, this);
            Action stepAction = actionManager.getAction(StepAction.KEY);
            stepAction.putValue(ExecuteAction.DOCUMENT_ELEMENT, this);
            Action stopAction = actionManager.getAction(StopAction.KEY);
            stopAction.putValue(ExecuteAction.DOCUMENT_ELEMENT, this);
            Action executePartAction = actionManager.getAction(ExecutePartAction.KEY);
            executePartAction.putValue(ExecutePartAction.DOCUMENT_ELEMENT, this);
            return new Action[] {undoAction, redoAction, stopAction, stepAction, executePartAction, executeAction};
        }
        return null;
    }

    @Override
    public void updateActionsState()
    {
        ActionManager actionManager = Application.getActionManager();
        actionManager.enableActions( getUndoManager().canUndo(), UndoAction.KEY, SaveDocumentAction.KEY );
        actionManager.enableActions( getUndoManager().canRedo(), RedoAction.KEY );
        boolean started = executionStarted();
        actionManager.enableActions( started, StopAction.KEY );
        actionManager.enableActions( !started, ExecuteAction.KEY, ExecutePartAction.KEY, StepAction.KEY );
    }

    // //////////////////////////////////////////////////////////////////////////
    // Update issues
    //

    @Override
    protected void doUpdate()
    {
    }

    @Override
    public boolean isMutable()
    {
        return getJSElement().getOrigin().isMutable();
    }

    @Override
    public void save()
    {
        JSElement jsElement = getJSElement();
        String newData = jsPanel.getText(false);
        jsElement.setContent(newData);
        try
        {
            CollectionFactoryUtils.save( jsElement );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Saving error", e);
        }
    }
    @Override
    public void close()
    {
        if( jsPanel != null )
        {
            jsPanel.removeUndoableEditListener(getUndoManager());
        }
        super.close();
    }

    public String getText(boolean onlySelected)
    {
        return jsPanel.getText(onlySelected);
    }

    public ScriptEnvironment getEnvironment()
    {
        OutputViewPart outputPane = (OutputViewPart)ViewPartRegistry.getViewPart("script.output");
        GUI.getManager().showViewPart( outputPane );
        return new SwingScriptEnvironment(outputPane.getTextPane());
    }

    public boolean executionStarted()
    {
        return ( jsPanel != null && jsPanel.executionStarted() )
                || ( task != null && task.getTask() != null && !task.getTask().isDone() && !task.getTask().isStopped() );
    }

    public void startExecution(boolean onlySelected)
    {
        String text = getText(onlySelected);
        if( onlySelected && text == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Please select a part of document for execution.");
        }
        else
        {
            SwingScriptEnvironment env = (SwingScriptEnvironment)getEnvironment();
            task = getJSElement().createTask(text, env, getJSElement().getOrigin() == null);
            final JobControl jobControl = task.getJobControl();
            env.setJobControl(jobControl);
            final JobControlListenerAdapter listener = new JobControlListenerAdapter()
            {
                @Override
                public void jobTerminated(JobControlEvent event)
                {
                    updateActionsState();
                    jobControl.removeListener(this);
                }
            };
            jobControl.addListener(listener);
            TaskManager.getInstance().runTask(task);
            updateActionsState();
        }
    }

    public void stopExecution()
    {
        if( jsPanel != null )
        {
            jsPanel.setPosition( -1);
        }
        if( task != null && !task.getTask().isDone() )
        {
            task.getJobControl().terminate();
        }
        updateActionsState();
    }
}
