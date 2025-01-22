package biouml.plugins.physicell;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.beans.swing.table.RowModel;

import biouml.model.Diagram;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.PluggedEditorsTabbedPane;
import ru.biosoft.gui.ViewPartSupport;

public class PhysicellModelViewPart extends ViewPartSupport implements PropertyChangeListener, DataCollectionListener
{
    private JTabbedPane tabbedPane;
    private MulticellEModel emodel;

    private Action addVisualizerAction;
    private Action removeVisualizerAction;

    private Action addColorSchemeAction;
    private Action removeColorSchemeAction;

    private PropertyInspector domainInspector = new PropertyInspector();
    private PropertyInspector parametersInspector = new PropertyInspector();
    private PropertyInspector initialConditionInspector = new PropertyInspector();
    private PropertyInspector reportInspector = new PropertyInspectorEx();
    private PropertyInspector optionsInspector = new PropertyInspector();
    private VisualizerTab visualizerTab = new VisualizerTab();
    private ColorSchemesTab colorSchemeTab = new ColorSchemesTab();

    private Action[] actions;

    public PhysicellModelViewPart()
    {
        tabbedPane = new JTabbedPane( SwingConstants.LEFT );
        add( BorderLayout.CENTER, tabbedPane );
    }

    private void update()
    {
        Object parent = getParent().getParent();
        if( parent instanceof PluggedEditorsTabbedPane )
        {
            PluggedEditorsTabbedPane pane = (PluggedEditorsTabbedPane)parent;
            pane.updateActions();
        }
    }

    private void initTabbedPane(MulticellEModel emodel)
    {
        tabbedPane.removeAll();
        tabbedPane.addTab( "Domain", domainInspector );
        tabbedPane.addTab( "Substrates", new SubstrateViewPart( emodel ) );
        tabbedPane.addTab( "Cell types", new CellDefinitionsViewPart( emodel ) );
        tabbedPane.addTab( "Events", new EventsViewPart( emodel ) );
        tabbedPane.addTab( "User Parameters", parametersInspector );
        tabbedPane.addTab( "Initial Condition", initialConditionInspector );
        tabbedPane.addTab( "Model Report", reportInspector );
        tabbedPane.addTab( "Visualizer", visualizerTab );
        tabbedPane.addTab( "Color schemes", colorSchemeTab );
        tabbedPane.addTab( "Model Options", optionsInspector );
        tabbedPane.addChangeListener( new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                update();
            }
        } );
        update();
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( emodel != null )
            emodel.removePropertyChangeListener( this );

        if( model != null )
            ( (Diagram)model ).removeDataCollectionListener( this );

        emodel = ( (Diagram)model ).getRole( MulticellEModel.class );
        emodel.addPropertyChangeListener( this );
        ( (Diagram)model ).addDataCollectionListener( this );
        domainInspector.explore( emodel.getDomain() );
        parametersInspector.explore( emodel.getUserParmeters() );
        initialConditionInspector.explore( emodel.getInitialCondition() );
        reportInspector.explore( emodel.getReportProperties() );
        optionsInspector.explore( emodel.getOptions() );
        visualizerTab.explore( emodel.getVisualizerProperties() );
        colorSchemeTab.explore( emodel );
        initTabbedPane( emodel );
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && ( (Diagram)model ).getRole() instanceof MulticellEModel;
    }

    @Override
    public Action[] getActions()
    {
        Component c = tabbedPane.getSelectedComponent();
        if( c instanceof VisualizerTab )
        {
            ActionManager actionManager = Application.getActionManager();
            ActionInitializer initializer = new ActionInitializer( MessageBundle.class );
            addVisualizerAction = new AddVisualizerAction();
            actionManager.addAction( AddVisualizerAction.KEY, addVisualizerAction );
            initializer.initAction( addVisualizerAction, AddVisualizerAction.KEY );

            removeVisualizerAction = new RemoveVisualizerAction();
            actionManager.addAction( RemoveVisualizerAction.KEY, removeVisualizerAction );
            initializer.initAction( removeVisualizerAction, RemoveVisualizerAction.KEY );

            actions = new Action[] {addVisualizerAction, removeVisualizerAction};
            return actions;
        }
        else if( c instanceof ColorSchemesTab )
        {
            ActionManager actionManager = Application.getActionManager();
            ActionInitializer initializer = new ActionInitializer( MessageBundle.class );
            addColorSchemeAction = new AddColorSchemeAction();
            actionManager.addAction( AddColorSchemeAction.KEY, addColorSchemeAction );
            initializer.initAction( addColorSchemeAction, AddColorSchemeAction.KEY );

            removeColorSchemeAction = new RemoveColorSchemeAction();
            actionManager.addAction( RemoveColorSchemeAction.KEY, removeColorSchemeAction );
            initializer.initAction( removeColorSchemeAction, RemoveColorSchemeAction.KEY );

            actions = new Action[] {addColorSchemeAction, removeColorSchemeAction};
            return actions;
        }
        return new Action[0];
    }


    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        Component component = tabbedPane.getSelectedComponent();
        if( component instanceof SubstrateViewPart )
            ( (SubstrateViewPart)component ).update();
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        Component component = tabbedPane.getSelectedComponent();
        if( component instanceof SubstrateViewPart )
            ( (SubstrateViewPart)component ).update();
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        Component component = tabbedPane.getSelectedComponent();
        if( component instanceof SubstrateViewPart )
            ( (SubstrateViewPart)component ).update();
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0)
    {
        Component component = tabbedPane.getSelectedComponent();
        if( component instanceof SubstrateViewPart )
            ( (SubstrateViewPart)component ).update();
    }

    public static class SubstrateViewPart extends PhysicellTab
    {
        public SubstrateViewPart(MulticellEModel emodel)
        {
            super( emodel );
        }

        @Override
        protected RowModel getRowModel()
        {
            return new ListRowModel( emodel.getSubstrates(), SubstrateProperties.class );
        }

        @Override
        protected Object createTemplate()
        {
            return new SubstrateProperties( "" );
        }
    }

    public class CellDefinitionsViewPart extends PhysicellTab
    {
        public CellDefinitionsViewPart(MulticellEModel emodel)
        {
            super( emodel );
        }

        @Override
        protected RowModel getRowModel()
        {
            return new ListRowModel( emodel.getCellDefinitions(), CellDefinitionProperties.class );
        }

        @Override
        protected Object createTemplate()
        {
            return new CellDefinitionProperties( "" );
        }
    }

    public static class EventsViewPart extends PhysicellTab
    {
        public EventsViewPart(MulticellEModel emodel)
        {
            super( emodel );
        }

        @Override
        protected RowModel getRowModel()
        {
            return new ListRowModel( emodel.getEvents(), EventProperties.class );
        }

        @Override
        protected Object createTemplate()
        {
            return new EventProperties( "" );
        }
    }


    /**
     * Adds new empty visualizer to the model
     */
    public class AddVisualizerAction extends AbstractAction
    {
        public static final String KEY = "Add visualizer";

        public AddVisualizerAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {

            visualizerTab.addVisualizer();
            update();
        }
    }

    /**
     * Removes selected visualizer from the model
     */
    public class RemoveVisualizerAction extends AbstractAction
    {
        public static final String KEY = "Remove visualizer";

        public RemoveVisualizerAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            visualizerTab.removeSelectedVisualizer();
            update();
        }
    }

    /**
     * Adds new empty visualizer to the model
     */
    public class AddColorSchemeAction extends AbstractAction
    {
        public static final String KEY = "Add color scheme";

        public AddColorSchemeAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {

            colorSchemeTab.addColorScheme();
            update();
        }
    }

    /**
     * Removes selected visualizer from the model
     */
    public class RemoveColorSchemeAction extends AbstractAction
    {
        public static final String KEY = "Remove color scheme";

        public RemoveColorSchemeAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            colorSchemeTab.removeSelectedVisualizer();
            update();
        }
    }

}