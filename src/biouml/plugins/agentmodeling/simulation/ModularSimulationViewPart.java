package biouml.plugins.agentmodeling.simulation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotVariable;
import biouml.plugins.agentmodeling.resources.MessageBundle;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.SimulationAnalysis;
import biouml.plugins.simulation.SimulationAnalysisParameters;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.jobcontrol.ClassJobControl;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.PropertiesDialog;
import ru.biosoft.util.bean.BeanInfoEx2;

@SuppressWarnings ( "serial" )
public class ModularSimulationViewPart extends ViewPartSupport
{
    protected static final Logger log = Logger.getLogger( ModularSimulationViewPart.class.getName() );

    public static final String CLEAR_LOG_ACTION = "Clear log";

    protected Action addAction = new AddAction( ADD_ACTION );
    protected Action removeAction = new RemoveAction( REMOVE_ACTION );
    protected Action simulateAction = new SimulateAction( SIMULATE_ACTION );
    protected Action stopSimulateAction = new StopSimulateAction( STOP_SIMULATE_ACTION );
    protected Action clearLogAction = new ClearLogAction( CLEAR_LOG_ACTION );

    private SimulationOptions options;
    private PlotsInfo plots;

    public static final String ADD_ACTION = "add modules group";
    public static final String REMOVE_ACTION = "remove modules group";
    public static final String SIMULATE_ACTION = "simulate";
    public static final String STOP_SIMULATE_ACTION = "stop simulation";
    public static final String SIMULATION_OPTIONS = "simulationOptions";

    private static final String PLOTS = "Plots";

    private JTabbedPane tabbedPane;
    private TextPaneAppender appender;
    private SimulationStatusListener simulationListener;
    private ClassJobControl jobControl;

    public ModularSimulationViewPart()
    {
        stopSimulateAction.setEnabled( false );
        simulationListener = new SimulationStatusListener();
        tabbedPane = new JTabbedPane( SwingConstants.LEFT );
        add( BorderLayout.CENTER, tabbedPane );
        appender = new TextPaneAppender( new PatternFormatter( "%4$s :  %5$s%n" ), "Simulation Log" );
        appender.setLevel( Level.INFO );
        appender.addToCategories( new String[] {"biouml.plugins.simulation"} );
    }

    private void initTabbedPane(Diagram diagram)
    {
        tabbedPane.removeAll();
        options = null;

        if( diagram != null )
        {
            DynamicProperty dp = diagram.getAttributes().getProperty( SIMULATION_OPTIONS );
            if( dp == null || ! ( dp.getValue() instanceof SimulationOptions ) )
            {
                options = new SimulationOptions();
                options.setGroups( ModuleGroup.generateModules( diagram ) );
                dp = new DynamicProperty( SIMULATION_OPTIONS, SimulationOptions.class, options );
                diagram.getAttributes().add( dp );
            }

            Object plotsObj = diagram.getAttributes().getValue( "Plots" );

            if( ! ( plotsObj instanceof PlotsInfo ) )
            {
                plots = new PlotsInfo( diagram.getRole( EModel.class ) );
                diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( PLOTS, PlotsInfo.class, plots ) );
            }
            else
                plots = (PlotsInfo)plotsObj;

            options = (SimulationOptions)dp.getValue();
            options.initDiagram( diagram );

            for( ModuleGroup moduleGroup : options.getGroups() )
            {
                ModuleGroupTab moduleGroupTab = new ModuleGroupTab( moduleGroup );
                tabbedPane.addTab( moduleGroup.getName(), moduleGroupTab );
            }
            tabbedPane.addTab( "Output Options", new OutputOptionsTab( options.getOutputOptions() ) );
            tabbedPane.addTab( "Plot Options", new PlotsTab( plots ) );
        }
        tabbedPane.addTab( "Simulation Log", appender.getLogTextPanel() );
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        if( model instanceof Diagram )
            initTabbedPane( (Diagram)model );
    }

    @Override
    public boolean canExplore(Object model)
    {
        return ( model instanceof Diagram );
    }

    @Override
    public Action[] getActions()
    {
        ActionInitializer initializer = new ActionInitializer( MessageBundle.class );
        initializer.initAction( simulateAction, SIMULATE_ACTION );
        initializer.initAction( stopSimulateAction, STOP_SIMULATE_ACTION );
        initializer.initAction( clearLogAction, CLEAR_LOG_ACTION );
        initializer.initAction( addAction, ADD_ACTION );
        initializer.initAction( removeAction, REMOVE_ACTION );
        Action[] action = new Action[] {simulateAction, stopSimulateAction, clearLogAction, addAction, removeAction};
        return action;
    }

    class AddAction extends AbstractAction
    {
        public AddAction(String name)
        {
            super( name );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            Set<String> alreadyAssigned = StreamEx.of( options.getGroups() ).flatMap( g -> StreamEx.of( g.getSubdiagrams() ) ).toSet();
            String[] available = StreamEx.of( Util.getSubDiagrams( (Diagram)model ) ).map( s -> s.getName() )
                    .prepend( ModuleGroup.TOP_LEVEL ).remove( s -> alreadyAssigned.contains( s ) ).toArray( String[]::new );

            ModuleGroupOptions moduleGroupOptions = new ModuleGroupOptions( generateUniqueName( "Simulation Options" ), available );
            new PropertiesDialog( Application.getApplicationFrame(), "Module group", moduleGroupOptions ).doModal();

            if( moduleGroupOptions.getSubdiagrams().length > 0 )
            {
                ModuleGroup group = new ModuleGroup( moduleGroupOptions.getName(), moduleGroupOptions.getSubdiagrams() );
                options.addGroup( group );
                tabbedPane.addTab( group.getName(), new ModuleGroupTab( group ) );
            }
        }
    }

    class RemoveAction extends AbstractAction
    {
        public RemoveAction(String name)
        {
            super( name );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            int index = tabbedPane.getSelectedIndex();
            Component component = tabbedPane.getComponent( index );
            if( component instanceof ModuleGroupTab )
            {
                tabbedPane.remove( index );
                options.removeGroup( ( (ModuleGroupTab)component ).getModuleGroup() );
            }
        }
    }

    class StopSimulateAction extends AbstractAction
    {
        public StopSimulateAction(String name)
        {
            super( name );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( jobControl != null )
                jobControl.terminate();
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
            Diagram diagram = (Diagram)model;
            boolean runSimulation = checkSubmodules();
            if( runSimulation )
                runSimulation = checkNoResult( diagram );
            if( runSimulation )
                runSimulation = checkIncorrectVariables( diagram );

            if( !runSimulation )
                return;

            SimulationEngine engine = null;
            try
            {
                ModularPreprocessor preprocessor = new ModularPreprocessor();
                preprocessor.flatSimilarSubdiagrams( (Diagram)model, options.getGroups() );
                engine = preprocessor.getSimulationEngine();
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
                return;
            }

            FunctionJobControl funJobControl = new FunctionJobControl( log );
            engine.setJobControl( funJobControl );

            SimulationAnalysis method = new SimulationAnalysis( engine.getDiagram(), "Simulation analysis" );
            SimulationAnalysisParameters simulationParameters = method.getParameters();
            try
            {
                for( ResultListener listener : getResultListeners( engine, funJobControl ) )
                    method.addResultListenerList( listener );
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }

            simulationParameters.setSimulationEngine( engine );
            jobControl = method.getJobControl();
            jobControl.addListener( simulationListener );
            TaskManager.getInstance().addAnalysisTask( method, jobControl, true,
                    TaskInfo.SIMULATE + " \"" + engine.getDiagram().getName() + "\"" );
        }

        protected List<ResultListener> getResultListeners(SimulationEngine simulationEngine, FunctionJobControl jobControl) throws Exception
        {
            List<ResultListener> listeners = new ArrayList<ResultListener>();

            if( options.getOutputOptions().isPlotResults() )
            {
                PlotInfo[] plotInfos = simulationEngine.getPlots();
                for( int i = 0; i < plotInfos.length; i++ )
                    listeners.add( new ResultPlotPane( simulationEngine, jobControl, plotInfos[i] ) );
            }

            if( options.getOutputOptions().isSaveResults() )
            {
                SimulationResult res = new SimulationResult( null, "tmp" );
                simulationEngine.initSimulationResult( res );
                listeners.add( new ResultWriter( res ) );
            }
            return listeners;
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

    public static class ModuleGroupTab extends ViewPartSupport
    {
        private ModuleGroup group;

        public ModuleGroupTab(ModuleGroup moduleGroup)
        {
            group = moduleGroup;
            PropertyInspectorEx editor = new PropertyInspectorEx();
            editor.setDefaultNumberFormat( null );
            editor.explore( moduleGroup );
            add( BorderLayout.CENTER, editor );
        }

        public ModuleGroup getModuleGroup()
        {
            return group;
        }
    }

    private static class OutputOptionsTab extends ViewPartSupport
    {
        public OutputOptionsTab(OutputOptions options)
        {
            PropertyInspectorEx editor = new PropertyInspectorEx();
            editor.setDefaultNumberFormat( null );
            editor.explore( options );
            add( BorderLayout.CENTER, editor );
        }
    }

    private class PlotsTab extends ViewPartSupport
    {
        public PlotsTab(PlotsInfo plotsInfo)
        {
            PropertyInspectorEx editor = new PropertyInspectorEx();
            editor.setDefaultNumberFormat( null );
            editor.explore( plotsInfo );
            add( BorderLayout.CENTER, editor );
        }
    }

    @Override
    public void onClose()
    {
        try
        {
            if (model != null)//it can be null in some cases - e.g. when we apply antimony, we recreate document but this editor was not even opened (and initialized) before that
            {
                DynamicPropertySet attributes = ( (Diagram)model ).getAttributes();
                attributes.add( DPSUtils.createHiddenReadOnlyTransient( SIMULATION_OPTIONS, SimulationOptions.class, options ) );
                attributes.add( DPSUtils.createHiddenReadOnlyTransient( PLOTS, PlotsInfo.class, plots ) );
            }
        }
        catch( Exception ex )
        {
            Logger.getLogger( getClass().getName() ).log( Level.SEVERE, "Error with simulation2 pane.", ex );
        }
    }

    protected boolean checkSubmodules()
    {
        StreamEx<String> availableSubDiagram = getAvailableSubDiagrams();
        if( availableSubDiagram.count() != 0 )
        {
            String error = "Please assign all subdiagrams to simulation groups: " + availableSubDiagram.joining( " ," );
            JOptionPane.showMessageDialog( null, error, "Simulation can not be started", JOptionPane.ERROR_MESSAGE );
            return false;
        }
        return true;
    }

    protected boolean checkNoResult(Diagram diagram)
    {
        if( !options.getOutputOptions().isSaveResults()
                && ( !options.getOutputOptions().isPlotResults() || !hasVariablesToPlot( diagram ) ) )
        {
            String errorMsg = "Please select variables to plot in \"Plots tab\" or simulation result to save in \"Output options\".\n Hint: plots with \"active\" property set to false will not be shown.";
            JOptionPane.showMessageDialog( null, errorMsg, "Simulation can not be started", JOptionPane.ERROR_MESSAGE );
            return false;
        }
        else if( options.getOutputOptions().isSaveResults() && options.getOutputOptions().getResultPath() == null )
        {
            String errorMsg = "Please specify result path or disable saving of simulation result.";
            JOptionPane.showMessageDialog( null, errorMsg, "Simulation can not be started", JOptionPane.ERROR_MESSAGE );
            return false;
        }
        return true;
    }

    protected boolean checkIncorrectVariables(Diagram diagram)
    {
        List<String> incorrect = getIncorrectPlotVariables( diagram );
        if( incorrect.size() > 0 )
        {
            String errorMsg = "There are incorrect variables to plot: "
                    + String.join( ", ", incorrect + ".\nPlease change the in the \"Plots\" tab below." );
            JOptionPane.showMessageDialog( null, errorMsg, "Simulation can not be started", JOptionPane.ERROR_MESSAGE );
            return false;
        }
        return true;
    }

    public StreamEx<String> getAvailableSubDiagrams()
    {
        Set<String> alreadyAssigned = StreamEx.of( options.getGroups() ).flatMap( g -> StreamEx.of( g.getSubdiagrams() ) ).toSet();
        return StreamEx.of( Util.getSubDiagrams( (Diagram)model ) ).map( s -> s.getName() ).prepend( ModuleGroup.TOP_LEVEL )
                .remove( s -> alreadyAssigned.contains( s ) );
    }

    public boolean hasVariablesToPlot(Diagram diagram)
    {
        Object plotObj = diagram.getAttributes().getValue( "Plots" );
        if( plotObj == null )
            return false;

        PlotsInfo plots = (PlotsInfo)plotObj;
        for( PlotInfo plot : plots.getActivePlots() )
        {
            if( plot.isActive() && plot.getYVariables().length > 0 )
                return true;
        }
        return false;
    }

    public List<String> getIncorrectPlotVariables(Diagram diagram)
    {
        List<String> incorrect = new ArrayList<>();
        Object plotObj = diagram.getAttributes().getValue( "Plots" );
        if( plotObj == null )
            return incorrect;
        PlotsInfo plots = (PlotsInfo)plotObj;
        for( PlotInfo plot : plots.getActivePlots() )
        {
            PlotVariable xVar = plot.getXVariable();
            String path = xVar.getPath();
            EModel emodel = ( path.isEmpty() ? diagram : Util.getSubDiagram( diagram, path ).getDiagram() ).getRole( EModel.class );

            if( !emodel.containsVariable( xVar.getName() ) )
                incorrect.add( xVar.getTitle() );

            for( Curve curve : plot.getYVariables() )
            {
                path = curve.getPath();
                emodel = ( path.isEmpty() ? diagram : Util.getSubDiagram( diagram, path ).getDiagram() ).getRole( EModel.class );

                if( !emodel.containsVariable( curve.getName() ) )
                    incorrect.add( curve.getTitle() );
            }
        }
        return incorrect;
    }

    /**
     * Container class
     */
    public static class SimulationOptions
    {
        private OutputOptions outputOptions = new OutputOptions();
        private Set<ModuleGroup> groups = new HashSet<>();
        private Diagram diagram;

        public void initDiagram(Diagram diagram)
        {
            this.diagram = diagram;
            for( ModuleGroup group : groups )
                group.initDiagram( diagram );
        }

        public OutputOptions getOutputOptions()
        {
            return outputOptions;
        }
        public void setOutputOptions(OutputOptions outputOptions)
        {
            this.outputOptions = outputOptions;
        }

        public ModuleGroup[] getGroups()
        {
            return StreamEx.of( groups ).toArray( ModuleGroup[]::new );
        }
        public void setGroups(ModuleGroup[] groups)
        {
            this.groups = StreamEx.of( groups ).toSet();
            for( ModuleGroup group : groups )
                group.initDiagram( diagram );
        }

        public void addGroup(ModuleGroup group)
        {
            group.initDiagram( diagram );
            groups.add( group );
        }

        public void removeGroup(ModuleGroup group)
        {
            groups.remove( group );
        }
    }

    public class SimulationOptionsBeanInfo extends BeanInfoEx2<SimulationOptions>
    {
        public SimulationOptionsBeanInfo()
        {
            super( SimulationOptions.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "groups" );
            add( "outputOptions" );
        }
    }

    private class SimulationStatusListener extends JobControlListenerAdapter
    {
        @Override
        public void jobStarted(JobControlEvent event)
        {
            simulateAction.setEnabled( false );
            stopSimulateAction.setEnabled( true );
        }

        @Override
        public void resultsReady(JobControlEvent event)
        {
            simulateAction.setEnabled( true );
            stopSimulateAction.setEnabled( false );
        }

        @Override
        public void jobTerminated(JobControlEvent event)
        {
            simulateAction.setEnabled( true );
            stopSimulateAction.setEnabled( false );
        }
    }

    private String generateUniqueName(String baseName)
    {
        String result = baseName;
        Set<String> names = StreamEx.of( options.groups ).map( g -> g.getName() ).toSet();
        int index = 1;
        while( names.contains( result ) )
        {
            result = baseName + " " + index;
        }
        return result;
    }
}
