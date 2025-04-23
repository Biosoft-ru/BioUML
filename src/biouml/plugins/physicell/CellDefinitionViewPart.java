package biouml.plugins.physicell;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;

import biouml.model.Diagram;
import biouml.model.Node;
import ru.biosoft.graphics.editor.SelectionManager;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.PluggedEditorsTabbedPane;
import ru.biosoft.gui.ViewPartSupport;

public class CellDefinitionViewPart extends ViewPartSupport implements ViewPaneListener
{
    private ViewPane viewPane;
    private JTabbedPane tabbedPane;
    private MulticellEModel emodel;
    private Node selectedNode;

    private AddRuleAction addRuleAction;
    private RemoveRuleAction removeRuleAction;

    private Action[] actions;

    private PropertyInspectorEx cycleInspector = new PropertyInspectorEx();
    private PropertyInspectorEx divisionInspector = new PropertyInspectorEx();
    private PropertyInspectorEx deathInspector = new PropertyInspectorEx();
    private PropertyInspectorEx motilityInspector = new PropertyInspectorEx();
    private PropertyInspectorEx mechanicsInspector = new PropertyInspectorEx();
    private PropertyInspectorEx volumeInspector = new PropertyInspectorEx();
    private PropertyInspectorEx geometryInspector = new PropertyInspectorEx();
    private PropertyInspectorEx secretionInspector = new PropertyInspectorEx();
    private PropertyInspectorEx functionsInspector = new PropertyInspectorEx();
    private PropertyInspectorEx interactionsInspector = new PropertyInspectorEx();
    private PropertyInspectorEx transformationsInspector = new PropertyInspectorEx();
    private PropertyInspectorEx customDataInspector = new PropertyInspectorEx();
    private PropertyInspectorEx intracellularInspector = new PropertyInspectorEx();
    private PropertyInspectorEx integrityInspector = new PropertyInspectorEx();
    
    private RulesTab ruleTab = new RulesTab();

    public CellDefinitionViewPart()
    {
        tabbedPane = new JTabbedPane( SwingConstants.LEFT );
        add( BorderLayout.CENTER, tabbedPane );
        cycleInspector.setDefaultNumberFormat( null );
        divisionInspector.setDefaultNumberFormat( null );
        deathInspector.setDefaultNumberFormat( null );
        motilityInspector.setDefaultNumberFormat( null );
        mechanicsInspector.setDefaultNumberFormat( null );
        volumeInspector.setDefaultNumberFormat( null );
        geometryInspector.setDefaultNumberFormat( null );
        secretionInspector.setDefaultNumberFormat( null );
        functionsInspector.setDefaultNumberFormat( null );
        interactionsInspector.setDefaultNumberFormat( null );
        transformationsInspector.setDefaultNumberFormat( null );
        customDataInspector.setDefaultNumberFormat( null );
        intracellularInspector.setDefaultNumberFormat( null );
        integrityInspector.setDefaultNumberFormat( null );
        ruleTab.setDefaultNumberFormat( null );
    }

    protected void showStub()
    {
        removeAll();
        JLabel text = new JLabel( "Select Cell Definition element on the diagram to display properties." );
        add( text, BorderLayout.CENTER );
        repaint();
        validate();
    }

    protected void showTabbedPane()
    {
        removeAll();
        add( tabbedPane, BorderLayout.CENTER );
        repaint();
        validate();
    }

    public void updatePanel()
    {
        SelectionManager sm = viewPane.getSelectionManager();
        if( sm.getSelectedModels() != null && sm.getSelectedModels().length > 0 && sm.getSelectedModels()[0] instanceof Node )
        {
            selectedNode = (Node)sm.getSelectedModels()[0];
            if( selectedNode.getRole() instanceof CellDefinitionProperties )
            {
                this.explore( selectedNode.getRole( CellDefinitionProperties.class ) );
                showTabbedPane();
                return;
            }
        }
        this.showStub();
    }

    private void explore(CellDefinitionProperties cdp)
    {
        motilityInspector.explore( cdp.getMotilityProperties() );
        mechanicsInspector.explore( cdp.getMechanicsProperties() );
        volumeInspector.explore( cdp.getVolumeProperties() );
        geometryInspector.explore( cdp.getGeometryProperties() );
        deathInspector.explore( cdp.getDeathProperties() );
        cycleInspector.explore( cdp.getCycleProperties() );
        divisionInspector.explore( cdp.getDivisionProperties() );
        functionsInspector.explore( cdp.getFunctionsProperties() );
        secretionInspector.explore( cdp.getSecretionsProperties() );
        interactionsInspector.explore( cdp.getInteractionsProperties() );
        transformationsInspector.explore( cdp.getTransformationsProperties() );
        customDataInspector.explore( cdp.getCustomDataProperties() );
        intracellularInspector.explore( cdp.getIntracellularProperties() );
        integrityInspector.explore(cdp.getIntegrityProperties());
        ruleTab.explore( cdp.getRulesProperties() );
    }


    public CellDefinitionViewPart(MulticellEModel emodel)
    {
        tabbedPane = new JTabbedPane( SwingConstants.TOP );
        add( BorderLayout.CENTER, tabbedPane );
        this.emodel = emodel;
        initTabbedPane( emodel );
    }

    private void initTabbedPane(MulticellEModel emodel)
    {
        tabbedPane.removeAll();
        tabbedPane.addTab( "Cycle", cycleInspector );
        tabbedPane.addTab( "Division", divisionInspector );
        tabbedPane.addTab( "Death", deathInspector );
        tabbedPane.addTab( "Volume", volumeInspector );
        tabbedPane.addTab( "Mechanics", mechanicsInspector );
        tabbedPane.addTab( "Integrity", integrityInspector );
        tabbedPane.addTab( "Motility", motilityInspector );
        tabbedPane.addTab( "Secretion", secretionInspector );
        tabbedPane.addTab( "Interactions", interactionsInspector );
        tabbedPane.addTab( "Transformations", transformationsInspector );
        tabbedPane.addTab( "Custom Data", customDataInspector );
        tabbedPane.addTab( "Functions", functionsInspector );
        tabbedPane.addTab( "Intracellular", intracellularInspector );
        tabbedPane.addTab( "Rules", ruleTab );
        tabbedPane.addChangeListener( new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                update();
            }
        } );
    }

    private void update()
    {
        Container parent = getParent();
        if( parent != null )
            parent = parent.getParent();
        if( parent instanceof PluggedEditorsTabbedPane )
        {
            PluggedEditorsTabbedPane pane = (PluggedEditorsTabbedPane)parent;
            pane.updateActions();
        }
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        emodel = ( (Diagram)model ).getRole( MulticellEModel.class );
        ViewPane viewPane = document.getViewPane();
        if( this.viewPane != null )
            this.viewPane.removeViewPaneListener( this );
        viewPane.addViewPaneListener( this );
        this.viewPane = viewPane;
        updatePanel();
        initTabbedPane( emodel );
    }

    @Override
    public Action[] getActions()
    {
        Component c = tabbedPane.getSelectedComponent();
        if( c instanceof RulesTab )
        {
            ActionManager actionManager = Application.getActionManager();
            if( actions == null )
            {
                ActionInitializer initializer = new ActionInitializer( MessageBundle.class );
                addRuleAction = new AddRuleAction();
                actionManager.addAction( AddRuleAction.KEY, addRuleAction );
                initializer.initAction( addRuleAction, AddRuleAction.KEY );

                removeRuleAction = new RemoveRuleAction();
                actionManager.addAction( RemoveRuleAction.KEY, removeRuleAction );
                initializer.initAction( removeRuleAction, RemoveRuleAction.KEY );

                actions = new Action[] {addRuleAction, removeRuleAction};
            }
            return actions;
        }
        return new Action[0];
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && ( (Diagram)model ).getRole() instanceof MulticellEModel;
    }

    @Override
    public void mouseClicked(ViewPaneEvent e)
    {
        updatePanel();
    }

    @Override
    public void mousePressed(ViewPaneEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(ViewPaneEvent e)
    {
        updatePanel();
    }

    @Override
    public void mouseEntered(ViewPaneEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(ViewPaneEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseDragged(ViewPaneEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseMoved(ViewPaneEvent e)
    {
        // TODO Auto-generated method stub

    }

    /**
     * Adds new empty rule to selected cell definition
     */
    public class AddRuleAction extends AbstractAction
    {
        public static final String KEY = "Add rule";

        public AddRuleAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {

            ruleTab.addRule();
            updatePanel();
        }
    }

    /**
     * Removes selected rule from selected Cell Definition
     */
    public class RemoveRuleAction extends AbstractAction
    {
        public static final String KEY = "Remove rule";

        public RemoveRuleAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ruleTab.removeSelectedRule();
            updatePanel();
        }
    }

}