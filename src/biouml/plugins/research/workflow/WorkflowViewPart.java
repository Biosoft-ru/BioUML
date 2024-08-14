package biouml.plugins.research.workflow;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.Collection;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.ViewPartSupport;
import biouml.model.Diagram;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.engine.WorkflowEngineListener;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;

/**
 * Workflow control view part
 */
public class WorkflowViewPart extends ViewPartSupport implements WorkflowEngineListener
{
    protected Logger log = Logger.getLogger(WorkflowViewPart.class.getName());

    protected WorkflowPanel workflowControlViewPane;
    protected WorkflowEngine workflowEngine;

    public static final String START_ACTION = "workflow-start";
    public static final String STOP_ACTION = "workflow-stop";

    protected Action[] actions;
    protected Action startAction = new StartAction();
    protected Action stopAction = new StopAction();


    public WorkflowViewPart()
    {
        workflowControlViewPane = new WorkflowPanel();
        add(workflowControlViewPane, BorderLayout.CENTER);

        startAction.setEnabled(true);
        stopAction.setEnabled(false);

    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        if( document != null )
        {
            workflowControlViewPane.explore((Diagram)model, document);
        }
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( ( model instanceof Diagram ) && ( ( (Diagram)model ).getType() instanceof WorkflowDiagramType ) )
            return true;
        return false;
    }

    @Override
    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(startAction, START_ACTION);
            initializer.initAction(stopAction, STOP_ACTION);
            Action[] wpActions = workflowControlViewPane.getActions();

            actions = StreamEx.of(startAction, stopAction).append( wpActions ).toArray( Action[]::new );
        }

        return actions;
    }

    //Engine listener support

    @Override
    public void started()
    {
    }

    @Override
    public void finished()
    {
        if( document != null )
        {
            document.update();
        }
        startAction.setEnabled(true);
        stopAction.setEnabled(false);
        workflowEngine = null;
    }

    @Override
    public void stateChanged()
    {
        if( document != null )
        {
            document.update();
        }
    }

    @Override
    public void errorDetected(String error)
    {
        JOptionPane.showMessageDialog(Application.getApplicationFrame(), error, "Workflow error", JOptionPane.ERROR_MESSAGE);
        workflowEngine = null;
    }

    //Actions
    private class StartAction extends AbstractAction
    {
        public StartAction()
        {
            super(START_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                WorkflowPropertiesDialog dialog = new WorkflowPropertiesDialog(Application.getApplicationFrame(), (Diagram)model);
                if(!dialog.doModal()) return;
                workflowEngine = new WorkflowEngine();
                workflowEngine.setWorkflow((Diagram)model);
                workflowEngine.setParameters((DynamicPropertySet)dialog.getProperties());
                workflowEngine.addEngineListener(WorkflowViewPart.this);
                if(dialog.getResearchDiagramPath() != null)
                {
                    workflowEngine.createResearchDiagram(dialog.getResearchDiagramPath());
                }
            }
            catch( Exception e1 )
            {
                errorDetected(e1.getMessage());
                return;
            }
            try
            {
                workflowEngine.initWorkflow();
            }
            catch( Exception e1 )
            {
                return;
            }
            startAction.setEnabled(false);
            stopAction.setEnabled(true);
            workflowEngine.start();
        }
    }

    private class StopAction extends AbstractAction
    {
        public StopAction()
        {
            super(STOP_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            startAction.setEnabled(true);
            stopAction.setEnabled(false);
            if(workflowEngine != null)
                workflowEngine.stop();
            workflowEngine = null;
        }
    }

    @Override
    public void parameterErrorDetected(String error)
    {
        errorDetected(error);
        workflowEngine = null;
    }

    @Override
    public void resultsReady(Object[] results)
    {
        Collection<ru.biosoft.access.core.DataElementPath> paths = workflowEngine == null ? Collections.<ru.biosoft.access.core.DataElementPath> emptyList() : workflowEngine
                .getAutoOpenPaths();
        if( results == null )
            return;
        DocumentManager documentManager = DocumentManager.getDocumentManager();
        for( Object result : results )
        {
            if( ! ( result instanceof DataElement ) )
                continue;
            DataElementPath path = DataElementPath.create((DataElement)result);
            if(!paths.contains(path))
                continue;
            if( documentManager.openDocument((DataElement)result) == null )
            {
                log.log(Level.SEVERE, "Can not open document: " + path);
            }
        }
    }

}
