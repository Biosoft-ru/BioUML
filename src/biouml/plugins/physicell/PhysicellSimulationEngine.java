package biouml.plugins.physicell;

import java.io.File;
import java.util.List;
import java.util.Map;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.simulation.ResultListener;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellContainer;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions;
import ru.biosoft.physicell.core.CellFunctions.Contact;
import ru.biosoft.physicell.core.CellFunctions.CustomCellRule;
import ru.biosoft.physicell.core.CellFunctions.DistanceCalculator;
import ru.biosoft.physicell.core.CellFunctions.Function;
import ru.biosoft.physicell.core.CellFunctions.Instantiator;
import ru.biosoft.physicell.core.CellFunctions.MembraneInteractions;
import ru.biosoft.physicell.core.CellFunctions.UpdateMigrationBias;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.CellFunctions.UpdateVelocity;
import ru.biosoft.physicell.core.CellFunctions.VolumeUpdate;
import ru.biosoft.physicell.core.InitialCellsArranger;
import ru.biosoft.physicell.core.CellFunctions.set_orientation;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.ReportGenerator;
import ru.biosoft.physicell.core.standard.FunctionRegistry;
import ru.biosoft.physicell.core.standard.StandardModels;
import ru.biosoft.physicell.ui.AgentVisualizer;
import ru.biosoft.physicell.ui.AgentVisualizer2;
import ru.biosoft.physicell.ui.Visualizer;
import ru.biosoft.physicell.ui.Visualizer.Section;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class PhysicellSimulationEngine extends SimulationEngine
{
    private boolean logReport = false;
    private DataElementPath customReportGenerator = null;

    public PhysicellSimulationEngine()
    {
        simulator = new PhysicellSimulator();
        simulatorType = "MULTICELL";
    }

    @Override
    public String getEngineDescription()
    {
        return "Multicellular agent engine";
    }

    @Override
    public String getVariableCodeName(String varName)
    {
        return null;
    }

    @Override
    public String getVariableCodeName(String diagramName, String varName)
    {
        return null;
    }

    @Override
    public Map<String, Integer> getVarIndexMapping()
    {
        return null;
    }

    @Override
    public Map<String, Integer> getVarPathIndexMapping()
    {
        return null;
    }

    public File[] generateModel(boolean forceRewrite) throws Exception
    {
        createModel();
        return null;
    }


    @Override
    public PhysicellModel createModel() throws Exception
    {
        ru.biosoft.physicell.core.Model model = new ru.biosoft.physicell.core.Model();

        Microenvironment m = model.getMicroenvironment();
        m.options.initial_condition_vector = new double[1];

        MulticellEModel emodel = diagram.getRole( MulticellEModel.class );

        for( UserParameter param : emodel.getUserParmeters().getParameters() )
            model.addParameter( param.getName(), param.getValue() );

        List<SubstrateProperties> substrates = emodel.getSubstrates();
        for( int i = 0; i < substrates.size(); i++ )
        {
            SubstrateProperties substrate = substrates.get( i );

            if( i == 0 )
                m.setDensity( 0, substrate.getName(), "", substrate.getDiffusionCoefficient(), substrate.getDecayRate() );
            else
                m.addDensity( substrate.getName(), "", substrate.getDiffusionCoefficient(), substrate.getDecayRate() );

            m.options.initial_condition_vector[i] = substrate.getInitialCondition();

            if( substrate.isDirichletCondition() )
            {
                m.options.Dirichlet_condition_vector[i] = substrate.getDirichletValue();
                m.options.Dirichlet_all[i] = true;
            }
        }

        DomainOptions options = emodel.getDomain();
        m.options.dx = options.getXStep();
        m.options.dy = options.getYStep();
        m.options.dz = options.getZStep();
        m.options.X_range = new double[] {options.getXFrom(), options.getXTo()};
        m.options.Y_range = new double[] {options.getYFrom(), options.getYTo()};
        m.options.Z_range = new double[] {options.getZFrom(), options.getZTo()};
        m.options.simulate2D = options.isUse2D();

        for( String density : m.densityNames )
        {
            Visualizer v = new Visualizer( null, density, Section.Z, 0 );
            v.setStubstrateIndex( m.findDensityIndex( density ) );
            v.setAgentVisualizer( new DefinitionVisualizer() );//TODO: hardcoded for now
            model.addVisualizer( v );
        }

        PhysicellOptions opts = (PhysicellOptions)getSimulatorOptions();
        model.setDiffusionDt( opts.getDiffusionDt() );
        model.setMechanicsDt( opts.getMechanicsDt() );
        model.setPhenotypeDt( opts.getPhenotypeDt() );
        model.setTMax( opts.getFinalTime() );

        m.options.calculate_gradients = opts.isCalculateGradient();
        m.options.track_internalized_substrates_in_each_agent = opts.isTrackInnerSubstrates();
                
        Microenvironment.initialize( m );
        
        String cellUpdateType = opts.getCellUpdateType();
        CellContainer container = CellContainer.createCellContainer( m, cellUpdateType, 30 );
        container.setRulesEnabled( true );

        List<CellDefinitionProperties> cds = emodel.getCellDefinitions();
        for( int i = 0; i < cds.size(); i++ )
        {
            model.registerCellDefinition( StandardModels.createFromDefault( cds.get( i ).getName(), i, m ) );
        }

        for( int i = 0; i < cds.size(); i++ )
        {
            CellDefinitionProperties cdp = cds.get( i );
            CellDefinition cd = model.getCellDefinition( cdp.getName() );
            cdp.getVolumeProperties().createVolume( cd );
            cdp.getMechanicsProperties().createMechanics( cd, model );
            cdp.getCycleProperties().createCycle( cd );
            cdp.getDeathProperties().createDeath( cd );
            cdp.getSecretionsProperties().createSecretion( cd );
            cdp.getMotilityProperties().createMotility( cd );
            cdp.getInteractionsProperties().createCellInteractions( cd, model );
            cdp.getTransformationsProperties().createCellTransformations( cd, model );
            cdp.getCustomDataProperties().createCustomData( cd );
            cdp.getIntracellularProperties().createIntracellular( cd, model );
            cdp.getRulesProperties().createRules( cd, model );

            FunctionsProperties fp = cdp.getFunctionsProperties();
            CellFunctions f = cd.functions;
            f.updatePhenotype = getFunction( fp.getPhenotypeUpdate(), fp.getPhenotypeUpdateCustom(), UpdatePhenotype.class );
            f.updateVelocity = getFunction( fp.getVelocityUpdate(), fp.getVelocityUpdateCustom(), UpdateVelocity.class );
            f.updateVolume = getFunction( fp.getVolumeUpdate(), fp.getVolumeUpdateCustom(), VolumeUpdate.class );
            f.customCellRule = getFunction( fp.getCustomRule(), fp.getCustomRuleCustom(), CustomCellRule.class );
            f.membraneDistanceCalculator = getFunction( fp.getMembraneDistance(), fp.getMembraneDistanceCustom(),
                    DistanceCalculator.class );
            f.contact = getFunction( fp.getContact(), fp.getContactCustom(), Contact.class );
            f.membraneInteraction = getFunction( fp.getMembraneInteraction(), fp.getMembraneInteractionCustom(),
                    MembraneInteractions.class );
            f.set_orientation = getFunction( fp.getOrientation(), fp.getOrientationCustom(), set_orientation.class );
            f.updateMigration = getFunction( fp.getMigrationUpdate(), fp.getMigrationUpdateCustom(), UpdateMigrationBias.class );
            f.instantiator = getFunction( fp.getInstantiate(), fp.getInstantiateCustom(), Instantiator.class );
        }

        if( getCustomReportGenerator() != null && !getCustomReportGenerator().isEmpty() )
            model.setReportGenerator( FunctionsLoader.load( getCustomReportGenerator(), ReportGenerator.class, log.getLogger() ) );

        model.init( false );

        InitialCondition condition = emodel.getInitialCondition();
        if( condition.isCustomCondition() )
        {
            DataElementPath codePath = condition.getCustomConditionCode();
            if( codePath != null && !codePath.isEmpty() )
                FunctionsLoader.load( codePath, InitialCellsArranger.class, log.getLogger()  ).arrange( model );
            DataElementPath tablePath = condition.getCustomConditionTable();
            if( tablePath != null && !tablePath.isEmpty() )
                loadCellsTable( tablePath, model );
        }
        else
        {
            for( CellDefinitionProperties cdp : emodel.getCellDefinitions() )
            {
                PhysiCellUtilities.place( model, model.getCellDefinition( cdp.getName() ), cdp.getInitialNumber() );
            }
        }

        if( logReport )
            log.info( model.display() );
        
        if (emodel.getReportProperties().isCustomReport())
        {
            DataElementPath dep = emodel.getReportProperties().getReportPath();
            model.setReportGenerator( FunctionsLoader.load( dep, ReportGenerator.class, log.getLogger()  ));
        }
        
        if (emodel.getReportProperties().isCustomVisualizer())
        {
            DataElementPath dep = emodel.getReportProperties().getVisualizerPath();
            AgentVisualizer visualizer = FunctionsLoader.load( dep, AgentVisualizer2.class, log.getLogger()  );     
            for (Visualizer v: model.getVisualizers())
                    v.setAgentVisualizer( visualizer );
        }
        
        
        return new PhysicellModel( model );
    }

    private void loadCellsTable(DataElementPath dep, ru.biosoft.physicell.core.Model model) throws Exception
    {
        TableDataCollection tdc = dep.getDataElement( TableDataCollection.class );
        for( String name : tdc.getNameList() )
        {
            Object[] row = TableDataCollectionUtils.getRowValues( tdc, name );
            double x = Double.parseDouble( row[0].toString() );
            double y = Double.parseDouble( row[1].toString() );
            double z = Double.parseDouble( row[2].toString() );
            String type = row[3].toString();
            Cell.createCell( model.getCellDefinition( type ), model, new double[] {x, y, z} );
        }
    }

    public <T extends Function> T getFunction(String standardFunction, DataElementPath path, Class<T> c) throws Exception
    {
        if( PhysicellConstants.CUSTOM.equals( standardFunction ) )
        {
            return FunctionsLoader.load( path, c, this.log.getLogger() );
        }
        else if( PhysicellConstants.NOT_SELECTED.equals( standardFunction ) )
        {
            return null;
        }
        else
        {
            Function f = FunctionRegistry.getFunction( standardFunction );
            if( f == null )
                throw new Exception( "Could not find function " + standardFunction );
            else if( c.isInstance( f ) )
                return c.cast( f );
            else
                throw new Exception( "Trying to load function" + standardFunction + " expected class: " + c + " but was: " + f.getClass() );
        }
    }

    @Override
    public Object getSolver()
    {
        return simulator;
    }

    @Override
    public void setSolver(Object solver)
    {
        this.simulator = (PhysicellSimulator)solver;
    }

    @Override
    public String simulate(Model model, ResultListener[] resultListeners) throws Exception
    {
        this.simulator.start( model, null, resultListeners, jobControl );
        return "";
    }

    @Override
    public PlotInfo[] getPlots()
    {
        return new PlotInfo[0];
    }

    @PropertyName ( "Log Model report" )
    public boolean isLogReport()
    {
        return logReport;
    }

    public void setLogReport(boolean logReport)
    {
        this.logReport = logReport;
    }

    @PropertyName ( "Report generator" )
    public DataElementPath getCustomReportGenerator()
    {
        return customReportGenerator;
    }
    public void setCustomReportGenerator(DataElementPath customReportGenerator)
    {
        this.customReportGenerator = customReportGenerator;
    }
}