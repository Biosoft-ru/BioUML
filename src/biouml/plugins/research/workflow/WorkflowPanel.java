package biouml.plugins.research.workflow;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.graphics.editor.SelectionManager;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.gui.Document;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.items.WorkflowItem;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowVariable;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;

/**
 * Workflow control view pane
 */
@SuppressWarnings ( "serial" )
public class WorkflowPanel extends JPanel implements ViewPaneListener, PropertyChangeListener
{
    protected PropertyInspector propertyInspector;
    protected ViewPane viewPane;

    protected Node selectedNode;
    protected Property selectedProperty;
    protected MouseListener piSelectionListener;
    
    public static final String BIND_PARAMETER_ACTION = "workflow-bind-parameter";
    
    protected Action bindParameterAction = new BindParameterAction();
    
    private Diagram diagram;
    private boolean parameterBindingMode = false;
    
    public WorkflowPanel()
    {
        setLayout(new BorderLayout());

        this.propertyInspector = new PropertyInspectorEx();
        propertyInspector.setPropertyShowMode(PropertyInspector.SHOW_EXPERT);
        selectedProperty = null;
        piSelectionListener = new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
            }
            @Override
            public void mouseEntered(MouseEvent e)
            {
            }
            @Override
            public void mouseExited(MouseEvent e)
            {
            }
            @Override
            public void mousePressed(MouseEvent e)
            {
            }
            @Override
            public void mouseReleased(MouseEvent e)
            {
                Property prop = propertyInspector.getProperty(e.getPoint());
                if( prop == null )
                {
                    selectedProperty = null;
                }
                else if( selectedProperty != null && selectedProperty.equals(prop) )
                {
                    selectedProperty = null;
                }
                else
                {
                    selectedProperty = prop;
                }
            }
        };
        
        //showPortsAction.setEnabled(true);
        bindParameterAction.setEnabled(false);
        
        showStub();
    }

    public void explore(Diagram diagram, Document document)
    {
        this.diagram = diagram;
        ViewPane viewPane = document.getViewPane();
        
        if( this.viewPane != null )
        {
            this.viewPane.removeViewPaneListener(this);
        }
        viewPane.addViewPaneListener(this);
        this.viewPane = viewPane;
        
        updatePanel();
    }

    protected void showStub()
    {
        removeAll();
        JLabel text = new JLabel("Select analysis element on the diagram to display properties.");
        add(text, BorderLayout.CENTER);
        repaint();
        validate();
    }

    protected void showPropertyInspector()
    {
        removeAll();
        add(propertyInspector, BorderLayout.CENTER);
        repaint();
        validate();
    }
    
    private void updatePanel()
    {
        if( ( propertyInspector.getBean() != null ) && ( propertyInspector.getBean() instanceof Option ) )
        {
            ( (Option)propertyInspector.getBean() ).removePropertyChangeListener(this);
        }
        Property propertyToBind = null;
        Node analysisToBind = null;
        if( parameterBindingMode )
        {
            propertyToBind = selectedProperty;
            analysisToBind = selectedNode;
        }
        selectedProperty = null;
        parameterBindingMode = false;
        Application.getApplicationFrame().getStatusBar().setMessage("");
        propertyInspector.removeMouseListener(piSelectionListener);
        bindParameterAction.setEnabled(false);

        SelectionManager sm = viewPane.getSelectionManager();
        if( sm.getSelectedModels() != null && sm.getSelectedModels().length > 0 && sm.getSelectedModels()[0] instanceof Node )
        {
            selectedNode = (Node)sm.getSelectedModels()[0];
            WorkflowItem item = WorkflowItemFactory.getWorkflowItem(selectedNode, (ViewEditorPane)viewPane);
            if(propertyToBind != null && item instanceof WorkflowVariable)
            {
                ( (WorkflowSemanticController)diagram.getType().getSemanticController() ).bindParameter(selectedNode, analysisToBind, propertyToBind, (ViewEditorPane)viewPane);
                return;
            }
            if(item != null)
            {
                propertyInspector.explore(item);
                showPropertyInspector();
                return;
            } else if( selectedNode.getKernel().getType().equals(Type.ANALYSIS_METHOD) )
            {
                bindParameterAction.setEnabled(true);
                AnalysisParameters parameters = WorkflowEngine.getAnalysisParametersByNode(selectedNode);
                propertyInspector.explore(parameters);
                propertyInspector.addMouseListener(piSelectionListener);
                if( parameters instanceof Option )
                {
                    ( (Option)parameters ).addPropertyChangeListener(this);

                }
                showPropertyInspector();
                return;
            } else if( selectedNode.getKernel().getType().equals(Type.ANALYSIS_SCRIPT) )
            {
                propertyInspector.explore(WorkflowEngine.getScriptParameters(selectedNode));
                showPropertyInspector();
                return;
            }
            else if( selectedNode.getKernel().getType().equals(Type.TYPE_PLOT))
            {
                propertyInspector.explore(WorkflowEngine.getPlotParameters(selectedNode));
                showPropertyInspector();
                return;
            }
            else if(selectedNode.getKernel().getType().equals(Type.TYPE_DATA_GENERATOR))
            {
                propertyInspector.explore(WorkflowEngine.getDataGeneratorParameters(selectedNode));
                showPropertyInspector();
                return;
            }
        }
        showStub();
    }

    //PropertyChangeListener implementation
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        //refresh attributes when parameters changed
        if( selectedNode.getKernel().getType().equals(Type.ANALYSIS_METHOD) )
        {
            AnalysisDPSUtils.writeParametersToNodeAttributes(null, (AnalysisParameters)propertyInspector.getBean(), selectedNode
                    .getAttributes());
            if(diagram == null)
                return;
            SemanticController sc = diagram.getType().getSemanticController();
            if(!(sc instanceof WorkflowSemanticController))
                return;
            WorkflowSemanticController wsc = (WorkflowSemanticController)sc;
            wsc.updateAnalysisNode( (Compartment)selectedNode );
        }
    }

    @Override
    public void mouseClicked(ViewPaneEvent e)
    {
    }

    @Override
    public void mousePressed(ViewPaneEvent event)
    {
    }
    
    @Override
    public void mouseReleased(ViewPaneEvent e)
    {
        updatePanel();
    }

    @Override
    public void mouseEntered(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseExited(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseDragged(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseMoved(ViewPaneEvent e)
    {
    }

    public Property getSelectedProperty()
    {
        return selectedProperty;
    }
    
    private class BindParameterAction extends AbstractAction
    {
        public BindParameterAction()
        {
            super(BIND_PARAMETER_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                Node analysisNode = selectedNode;
                Property prop = getSelectedProperty();
                if( analysisNode != null && analysisNode.getKernel().getType().equals(Type.ANALYSIS_METHOD) && prop != null )
                {
                    parameterBindingMode  = true;
                    Application.getApplicationFrame().getStatusBar().setMessage("Click on expression or parameter to bind it with selected analysis property");
                }
            }
            catch( Exception e1 )
            {
            }
        }
    }

    public Action[] getActions()
    {
        new ActionInitializer(MessageBundle.class).initAction(bindParameterAction, BIND_PARAMETER_ACTION);
        return new Action[]{bindParameterAction};
    }
}
