package biouml.plugins.simulation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;
import ru.biosoft.gui.PluggedEditorsTabbedPane;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.PropertiesDialog;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.EModelRoleSupport;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.plot.PlotDialog;
import biouml.plugins.simulation.resources.MessageBundle;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import ru.biosoft.jobcontrol.ClassJobControl;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

@SuppressWarnings ( "serial" )
public class SimulationEnginePane extends EditorPartSupport implements ItemListener, PropertyChangeListener
{
    private JTabbedPane tabbedPane;
    protected EModelRoleSupport emodel;

    public static final String GENERATE_CODE_ACTION = "generate";
    public static final String SIMULATE_ACTION = "simulate";
    public static final String STOP_SIMULATION_ACTION = "stop";
    public static final String SAVE_RESULT_ACTION = "save-result";
    public static final String PLOT_ACTION = "plot";
    public static final String CLEAR_LOG_ACTION = "clear log";

    protected Action[] actions;
    private EngineTab engineTab;
    private PlotTab plotTab;

    public SimulationEnginePane()
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

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        emodel = ( (Diagram)model ).getRole( EModelRoleSupport.class );
        emodel.addPropertyChangeListener( this );

        engineTab = new EngineTab( model, document );
        plotTab = emodel instanceof EModel ? new PlotTab( model ) : null;
        initTabbedPane( model, document );
    }

    private void initTabbedPane(Object model, Document document)
    {
        tabbedPane.removeAll();

        tabbedPane.addTab( "Engine", engineTab.getView() );

        if( plotTab != null )
            tabbedPane.addTab( "Plots", plotTab.getView() );

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
    public Action[] getActions()
    {
        if( actions == null )
        {
            Component c = tabbedPane.getSelectedComponent();

            if( c instanceof EngineTab )
            {
                return ( (EngineTab)c ).getActions();
            }
        }
        return new Action[0];
    }

    public static class PlotTab extends EditorPartSupport
    {
        private final PropertyInspectorEx inspector = new PropertyInspectorEx();
        protected EModel emodel;
        public static final String PLOTS = "Plots";
        PlotsInfo plot;

        public JComponent getView()
        {
            return inspector;
        }

        public PlotTab(Object model)
        {
            if( model instanceof Diagram )
                this.emodel = ( (Diagram)model ).getRole( EModel.class );
            {
                try
                {
                    Object plotsObj = emodel.getParent().getAttributes().getValue( PLOTS );

                    if( ! ( plotsObj instanceof PlotsInfo ) )
                    {
                        plot = new PlotsInfo( emodel );
                        emodel.getParent().getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( PLOTS, PlotsInfo.class, plot ) );
                    }
                    else
                        plot = (PlotsInfo)plotsObj;

                    inspector.explore( plot );

                }
                catch( Exception e )
                {
                    Logger.getLogger( getClass().getName() ).log( Level.SEVERE, "Can not explore plots for diagram " + model, e );
                }
            }
        }

        @Override
        public void onClose()
        {
            try
            {
                if( emodel != null ) //it can be null in some cases - e.g. when we apply antimony, we recreate document but this editor was not even opened (and initialized) before that
                    emodel.getParent().getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( PLOTS, PlotsInfo.class, plot ) );
            }
            catch( Exception ex )
            {
                Logger.getLogger( getClass().getName() ).log( Level.SEVERE, "Error with plot pane.", ex );
            }
        }
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && ( (Diagram)model ).getRole() instanceof EModelRoleSupport;
    }

    public static class EngineTab extends EditorPartSupport implements ItemListener, PropertyChangeListener
    {
        protected Action[] actions;
        protected Logger log;
        protected String[] categoryList = {"biouml.plugins.simulation"};
        protected TextPaneAppender appender;
        protected ResultWriter currentResults;
        private FunctionJobControl jobControl;

        protected JComboBox<String> engineBox = new JComboBox<>();
        private final JLabel engineLabel = new JLabel( MessageBundle.getMessage( "SIMULATION_ENGINE" ) );
        private JSplitPane splitPane = new JSplitPane();
        private final SimulationStatusListener listener = new SimulationStatusListener();
        protected EModelRoleSupport executableModel;
        protected SimulationEngine simulationEngine;
        boolean doSimulate;
        protected PropertyInspector inspector = new PropertyInspectorEx();
        protected DiagramElementsListener diagramListener = new DiagramElementsListener();
        protected JComponent view;

        protected Action generateCodeAction = new GenerateCodeAction( GENERATE_CODE_ACTION );
        protected Action simulateAction = new SimulateAction( SIMULATE_ACTION );
        protected Action stopSimulationAction = new StopSimulationAction( STOP_SIMULATION_ACTION );
        protected Action saveResultAction = new SaveResultAction( SAVE_RESULT_ACTION );
        protected Action plotAction = new PlotAction( PLOT_ACTION );
        protected Action clearLogAction = new ClearLogAction( CLEAR_LOG_ACTION );

        public EngineTab(Object model, Document document)
        {
            this.explore( model, document );
            log = Logger.getLogger( getClass().getName() );
            jobControl = new FunctionJobControl( log );
        }

        public SimulationEngine getSimulationEngine()
        {
            return simulationEngine;
        }
        public void setSimulationEngine(SimulationEngine simulationEngine)
        {
            simulationEngine.setJobControl( new FunctionJobControl( log ) );
            if( this.simulationEngine != null )
            {
                this.simulationEngine.releaseDiagram();
                this.simulationEngine.removePropertyChangeListener( this );
            }

            this.simulationEngine = simulationEngine;
            if( model instanceof Diagram )
            {
                this.simulationEngine.setDiagram( (Diagram)model );
                ( (Diagram)model ).getAttributes()
                        .add( DPSUtils.createHiddenReadOnlyTransient( "simulationOptions", SimulationEngine.class, simulationEngine ) );
            }

            simulationEngine.addPropertyChangeListener( this );
            exploreSimulationEngine();
        }

        public void exploreSimulationEngine()
        {
            inspector.explore( simulationEngine );
            inspector.setComponentModel( ComponentFactory.getModel( simulationEngine, Policy.UI, true ) );
        }

        public EModelRoleSupport getExecutableModel()
        {
            return executableModel;
        }

        @Override
        public void itemStateChanged(ItemEvent e)
        {
            if( e.getStateChange() == ItemEvent.SELECTED )
                setSimulationEngine( SimulationEngineRegistry.getSimulationEngine( e.getItem().toString() ) );
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if( evt.getPropertyName().equals( "solver" ) )
                inspector.setComponentModel( ComponentFactory.getModel( simulationEngine, Policy.UI, true ) );
        }

        ////////////////////////////////////////////////////////////////////////////
        // EditorPart issues
        //
        private void initEngineBox(String[] simulationEngineNames, String selected)
        {
            engineBox.removeItemListener( this );
            engineBox.removeAllItems();
            for( String engineName : simulationEngineNames )
                engineBox.addItem( engineName );
            engineBox.setVisible( simulationEngineNames.length > 1 );
            engineLabel.setVisible( simulationEngineNames.length > 1 );
            engineBox.setSelectedItem( selected );
            engineBox.addItemListener( this );
        }

        private JSplitPane initSplitPane()
        {
            appender = new TextPaneAppender( new PatternFormatter( "%4$s :  %5$s%n" ), "Application Log" );
            appender.setLevel( Level.INFO );
            appender.addToCategories( categoryList );
            splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, false, inspector, appender.getLogTextPanel() );
            splitPane.setDividerLocation( 0.5 );
            return splitPane;
        }

        public void generateView()
        {
            GridBagLayout gbag = new GridBagLayout();
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets( 5, 5, 5, 5 );

            gbag.setConstraints( engineLabel, gbc );

            gbc.gridx = 1;
            gbag.setConstraints( engineBox, gbc );

            gbc.weightx = 1;
            gbc.weighty = 2;
            gbc.gridwidth = 4;
            gbc.gridy = 1;
            gbc.gridx = 0;
            initSplitPane();
            gbag.setConstraints( splitPane, gbc );
            setLayout( gbag );

            add( engineLabel );
            add( engineBox );
            add( splitPane );
        }

        @Override
        public JComponent getView()
        {
            return this;
        }

        @Override
        public Object getModel()
        {
            return model;
        }

        @Override
        public boolean canExplore(Object model)
        {
            return model instanceof Diagram && ( (Diagram)model ).getRole() instanceof EModelRoleSupport;
        }

        @Override
        public void explore(Object model, Document document)
        {
            if( this.model != null )
                ( (Diagram)this.model ).removeDataCollectionListener( diagramListener );
            this.model = model;
            this.document = document;
            ( (Diagram)model ).addDataCollectionListener( diagramListener );
            try
            {
                if( executableModel != null )
                    executableModel.removePropertyChangeListener( inspector );

                executableModel = ( (Diagram)model ).getRole( EModelRoleSupport.class );
                executableModel.addPropertyChangeListener( inspector );
                inspector.setDefaultNumberFormat( null );

                String[] simulationEngineNames = SimulationEngineRegistry.getSimulationEngineNames( executableModel );
                if( simulationEngineNames.length == 0 )
                {
                    log.log( Level.SEVERE, "No simulation engines found for current diagram" );
                    return;
                }
                String selected;

                SimulationEngine engine = DiagramUtility.getPreferredEngine( (Diagram)model );
                if( engine != null )
                {
                    setSimulationEngine( engine );
                    selected = SimulationEngineRegistry.getSimulationEngineName( engine );
                }
                else
                {
                    setSimulationEngine( SimulationEngineRegistry.getSimulationEngine( simulationEngineNames[0] ) );
                    selected = simulationEngineNames[0];
                }

                initEngineBox( simulationEngineNames, selected );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Can not explore model for simulation.", e );
                inspector.explore( null );
            }
            resetActions();
            generateView();
        }


        private void resetActions()
        {
            simulateAction.setEnabled( true );
            generateCodeAction.setEnabled( true );
            stopSimulationAction.setEnabled( false );
            saveResultAction.setEnabled( false );
            plotAction.setEnabled( false );
        }


        ////////////////////////////////////////////////////////////////////////////
        // Actions
        //


        class GenerateCodeAction extends AbstractAction
        {
            public GenerateCodeAction(String name)
            {
                super( name );
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {
                SimulationEngineUtils.generateCode( executableModel, simulationEngine );
            }
        }

        class SimulateAction extends AbstractAction
        {
            public SimulateAction(String name)
            {
                super( name );
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {
                simulate();
            }
        }

        class ClearLogAction extends AbstractAction
        {
            public ClearLogAction(String name)
            {
                super( name );
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {
                appender.getLogTextPanel().setText( "" );
            }
        }

        class StopSimulationAction extends AbstractAction
        {
            public StopSimulationAction(String name)
            {
                super( name );
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {
                stopSimulation();
            }
        }

        class SaveResultAction extends AbstractAction
        {
            public SaveResultAction(String name)
            {
                super( name );
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {
                saveSimulationResult();
            }
        }

        class PlotAction extends AbstractAction
        {
            public PlotAction(String name)
            {
                super( name );
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {
                plot();
            }
        }

        @Override
        public Action[] getActions()
        {
            if( actions == null )
            {
                ActionInitializer initializer = new ActionInitializer( MessageBundle.class );
                initializer.initAction( generateCodeAction, GENERATE_CODE_ACTION );
                initializer.initAction( simulateAction, SIMULATE_ACTION );
                initializer.initAction( stopSimulationAction, STOP_SIMULATION_ACTION );
                initializer.initAction( saveResultAction, SAVE_RESULT_ACTION );
                initializer.initAction( plotAction, PLOT_ACTION );
                initializer.initAction( clearLogAction, CLEAR_LOG_ACTION );

                saveResultAction.setEnabled( false );
                plotAction.setEnabled( false );

                actions = new Action[] {generateCodeAction, simulateAction, stopSimulationAction, saveResultAction, plotAction,
                        clearLogAction};
            }

            return actions;
        }

        /////////////////////////////////////////////////////////////////////////////
        // Utility methods for actions handling
        //

        protected ResultListener[] getResultListeners(SimulationEngine simulationEngine) throws Exception
        {
            SimulationResult res = simulationEngine.generateSimulationResult();
            if (res == null)
                return new ResultListener[0];
            
            currentResults = new ResultWriter( res );
            res.setDiagramPath( executableModel.getParent().getCompletePath() );
            //        if( document instanceof DiagramDocument )
            //            res.setDiagramPath( ( (DiagramDocument)document ).getDiagram().getCompletePath());

            if( !simulationEngine.needToShowPlot )
                return new ResultListener[] {currentResults};
            else
            {
                PlotInfo[] plotInfos = simulationEngine.getPlots();
                ResultListener[] result = new ResultListener[plotInfos.length + 1];
                for( int i = 0; i < plotInfos.length; i++ )
                    result[i] = simulationEngine.generateResultPlot( jobControl, plotInfos[i] );
                result[plotInfos.length] = currentResults;
                return result;
            }
        }

        protected void doSaveSimulationResult(SimulationResult simulationResult) throws Exception
        {
            if( simulationResult != null )
            {
                SimulationResult tmpResult = currentResults.getResults().clone( simulationResult.getOrigin(), simulationResult.getName() );
                simulationResult.getOrigin().put( tmpResult );
            }
        }

        protected void stopSimulation()
        {
            simulationEngine.stopSimulation();
            simulateAction.setEnabled( true );
            generateCodeAction.setEnabled( true );
        }


        /**
         * pending start in separate thread and disable other actions
         * until simulation will be completed.
         */
        protected void simulate()
        {
            doSimulate = true;
            // check whether there are variables to be plotted
            if( simulationEngine.getNeedToShowPlot() && !simulationEngine.hasVariablesToPlot() )
            {
                showNoVariablesDialog();
            }
            else
            {
                List<String> incorrect = new ArrayList<>();
                try
                {
                    incorrect = simulationEngine.getIncorrectPlotVariables();
                }
                catch( Exception ex )
                {
                    JOptionPane.showMessageDialog( null, ex.getMessage(), MessageBundle.getMessage( "WARN_WRONG_VARIABLES_TO_PLOT_TITLE" ),
                            JOptionPane.ERROR_MESSAGE );
                    return;
                }

                if( incorrect.size() > 0 )
                {
                    String errorMsg = MessageFormat.format( MessageBundle.getMessage( "WARN_WRONG_VARIABLES_TO_PLOT" ),
                            String.join( ", ", incorrect ) );
                    JOptionPane.showMessageDialog( null, errorMsg, MessageBundle.getMessage( "WARN_WRONG_VARIABLES_TO_PLOT_TITLE" ),
                            JOptionPane.ERROR_MESSAGE );
                    return;
                }
            }

            if( doSimulate )
            {
                SimulationAnalysis method = new SimulationAnalysis( simulationEngine.getDiagram(), "Simulation analysis" );
                SimulationAnalysisParameters simulationParameters = method.getParameters();
                try
                {
                    for( ResultListener listener : getResultListeners( simulationEngine ) )
                        method.addResultListenerList( listener );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }

                simulationParameters.setSimulationEngine( simulationEngine );
                ClassJobControl jobControl = method.getJobControl();
                jobControl.addListener( listener );
                TaskManager.getInstance().addAnalysisTask( method, jobControl, true,
                        TaskInfo.SIMULATE + " \"" + simulationEngine.getDiagram().getName() + "\"" );
            }
        }

        private void showNoVariablesDialog()
        {
            JFrame frame = new JFrame( "" );
            doSimulate = false;
            JPanel panel = new JPanel();
            panel.setLayout( new BorderLayout() );
            panel.add( new JLabel( MessageBundle.getMessage( "WARN_NO_VARIABLES_TO_PLOT" ) ), BorderLayout.CENTER );
            panel.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );
            OkCancelDialog dialog = new OkCancelDialog( frame, "Warning", panel )
            {
                @Override
                public void okPressed()
                {
                    doSimulate = true;
                    super.okPressed();
                }
                @Override
                public void cancelPressed()
                {
                    doSimulate = false;
                    super.cancelPressed();
                }
            };
            dialog.doModal();
        }

        protected DataCollection<?> getSimulationResultDC()
        {
            Module module = Module.getModule( executableModel.getDiagramElement() );
            return SimulationEngineUtils.getSimulationResultDC( module, simulationEngine );
        }

        protected void saveSimulationResult()
        {
            try
            {
                DynamicPropertySet dps = new DynamicPropertySetAsMap();
                dps.add( new DynamicProperty( "path", DataElementPath.class, "" ) );
                PropertiesDialog dialog = new PropertiesDialog( Application.getApplicationFrame(), "New element", dps );

                if( dialog.doModal() )
                {
                    DataElementPath dep = (DataElementPath)dps.getProperty( "path" ).getValue();

                    simulationResult = new SimulationResult( dep.getParentCollection(), dep.getName() );
                    doSaveSimulationResult( simulationResult );
                    plotAction.setEnabled( true );
                }
            }
            catch( Throwable t )
            {
                simulationEngine.getLogger().error( "ERROR_SAVE_RESULT",
                        new String[] {simulationEngine.getDiagram().getName(), t.toString()}, t );
            }
        }

        protected SimulationResult simulationResult = null;
        protected void plot()
        {
            if( simulationResult == null )
            {
                int type = JOptionPane.showConfirmDialog( this,
                        simulationEngine.getLogger().getResourceString( "SAVE_RESULT_CONFIRM_MESSAGE" ),
                        simulationEngine.getLogger().getResourceString( "SAVE_RESULT_DIALOG_TITLE" ), JOptionPane.OK_CANCEL_OPTION );
                if( type == JOptionPane.OK_OPTION )
                    saveSimulationResult();
                else
                    return;
            }

            try
            {
                new PlotDialog( simulationResult ).doModal();
            }
            catch( Exception ex )
            {
                log.log( Level.SEVERE, "Error occured when creating plot for simulation result "
                        + ( simulationResult != null ? simulationResult.getName() + "." : "." ) + " : " + ex );
            }
        }

        public class DiagramElementsListener implements DataCollectionListener
        {
            protected boolean refresh = false;

            @Override
            public void elementAdded(DataCollectionEvent e) throws Exception
            {
                DataElement de = e.getDataElement();
                if( de instanceof SubDiagram )
                {
                    simulationEngine.propertyChange( null );
                    exploreSimulationEngine();
                }
            }

            @Override
            public void elementChanged(DataCollectionEvent e) throws Exception
            {
            }

            @Override
            public void elementRemoved(DataCollectionEvent e) throws Exception
            {
                if( refresh )
                {
                    simulationEngine.propertyChange( null );
                    exploreSimulationEngine();
                    refresh = false;
                }
            }

            @Override
            public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
            {
            }

            @Override
            public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
            {
            }

            @Override
            public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
            {
                refresh = e.getDataElement() instanceof SubDiagram;
            }
        }

        class SimulationStatusListener extends JobControlListenerAdapter
        {
            @Override
            public void jobStarted(JobControlEvent event)
            {
                generateCodeAction.setEnabled( false );
                simulateAction.setEnabled( false );
                saveResultAction.setEnabled( false );
                plotAction.setEnabled( false );
                stopSimulationAction.setEnabled( true );
            }

            @Override
            public void resultsReady(JobControlEvent event)
            {
                generateCodeAction.setEnabled( true );
                simulateAction.setEnabled( true );
                saveResultAction.setEnabled( true );
                plotAction.setEnabled( true );
                stopSimulationAction.setEnabled( false );
            }

            @Override
            public void jobTerminated(JobControlEvent event)
            {
                generateCodeAction.setEnabled( true );
                simulateAction.setEnabled( true );
                saveResultAction.setEnabled( false );
                plotAction.setEnabled( false );
                stopSimulationAction.setEnabled( false );
            }
        }
    }


    @Override
    public void itemStateChanged(ItemEvent e)
    {
        Component c = tabbedPane.getSelectedComponent();
        if( c instanceof ItemListener )
            ( (ItemListener)c ).itemStateChanged( e );
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        Component c = tabbedPane.getSelectedComponent();
        if( c instanceof PropertyChangeListener )
            ( (PropertyChangeListener)c ).propertyChange( evt );
    }

}
