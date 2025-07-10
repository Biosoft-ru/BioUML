package biouml.plugins.physicell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.plugins.physicell.plot.PlotProperties;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.physicell.biofvm.ConstantCoefficientsLOD3D;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellContainer;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions;
import ru.biosoft.physicell.core.CellFunctions.CellDivision;
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
import ru.biosoft.physicell.core.Model.Event;
import ru.biosoft.physicell.core.CellFunctions.set_orientation;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.ReportGenerator;
import ru.biosoft.physicell.core.Rules;
import ru.biosoft.physicell.core.standard.FunctionRegistry;
import ru.biosoft.physicell.core.standard.StandardModels;
import ru.biosoft.physicell.ui.AgentColorer;
import ru.biosoft.physicell.ui.GIFGenerator;
import ru.biosoft.physicell.ui.Visualizer;
import ru.biosoft.physicell.ui.Visualizer2D.Section;
import ru.biosoft.physicell.ui.render.Visualizer3D;
import ru.biosoft.physicell.ui.Visualizer2D;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TempFiles;

public class PhysicellSimulationEngine extends SimulationEngine
{
    private boolean logReport = false;
    private DataElementPath customReportGenerator = null;

    public PhysicellSimulationEngine()
    {
        simulator = new PhysicellSimulator();
        simulatorType = "MULTICELL";
        needToShowPlot = false;
    }
    
    @Override
    public void restoreOriginalDiagram()
    {
        this.diagram = originalDiagram;
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
        PhysicellOptions opts = (PhysicellOptions)getSimulatorOptions();
        
        ru.biosoft.physicell.core.Model model = null;
        if( opts.getModelType().equals( PhysicellOptions.COVID_MODEL ) )
            model = new ru.biosoft.physicell.covid.ModelCovid();
        else
            model = new ru.biosoft.physicell.core.Model();

        Microenvironment m = model.getMicroenvironment();
        m.options.initial_condition_vector = new double[1];

        MulticellEModel emodel = diagram.getRole( MulticellEModel.class );

        for( UserParameter param : emodel.getUserParmeters().getParameters() )
            model.addParameter( param.getName(), param.getValue(), param.getDescription() );

        List<SubstrateProperties> substrates = emodel.getSubstrates();
        for( int i = 0; i < substrates.size(); i++ )
        {
            SubstrateProperties substrate = substrates.get( i );

            if( i == 0 )
                m.setDensity( 0, substrate.getName(), "", substrate.getDiffusionCoefficient(), substrate.getDecayRate() );
            else
                m.addDensity( substrate.getName(), "", substrate.getDiffusionCoefficient(), substrate.getDecayRate() );

            m.options.initial_condition_vector[i] = substrate.getInitialCondition();

            if( substrate.getXMin() > 0 )
            {
                m.options.Dirichlet_xmin[i] = true;
                m.options.Dirichlet_xmin_values[i] = substrate.getXMin();
                m.options.outer_Dirichlet_conditions = true;
            }
            if( substrate.getXMax() > 0 )
            {
                m.options.Dirichlet_xmax[i] = true;
                m.options.Dirichlet_xmax_values[i] = substrate.getXMax();
                m.options.outer_Dirichlet_conditions = true;
            }
            if( substrate.getYMin() > 0 )
            {
                m.options.Dirichlet_ymin[i] = true;
                m.options.Dirichlet_ymin_values[i] = substrate.getYMin();
                m.options.outer_Dirichlet_conditions = true;
            }
            if( substrate.getYMax() > 0 )
            {
                m.options.Dirichlet_ymax[i] = true;
                m.options.Dirichlet_ymax_values[i] = substrate.getYMax();
                m.options.outer_Dirichlet_conditions = true;
            }
            if( substrate.getZMin() > 0 )
            {
                m.options.Dirichlet_zmin[i] = true;
                m.options.Dirichlet_zmin_values[i] = substrate.getZMin();
                m.options.outer_Dirichlet_conditions = true;
            }
            if( substrate.getZMax() > 0 )
            {
                m.options.Dirichlet_zmax[i] = true;
                m.options.Dirichlet_zmax_values[i] = substrate.getZMax();
                m.options.outer_Dirichlet_conditions = true;
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

        
        model.setDiffusionDt( opts.getDiffusionDt() );
        model.setMechanicsDt( opts.getMechanicsDt() );
        model.setPhenotypeDt( opts.getPhenotypeDt() );
        model.setTMax( opts.getFinalTime() );

        ( (ConstantCoefficientsLOD3D)model.getMicroenvironment().getSolver() ).setPrallel( opts.isParallelDiffusion() );

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
        Rules.setupRules( model );
        for( int i = 0; i < cds.size(); i++ )
        {
            CellDefinitionProperties cdp = cds.get( i );
            CellDefinition cd = model.getCellDefinition( cdp.getName() );
            cdp.getVolumeProperties().createVolume( cd );
            cdp.getMechanicsProperties().createMechanics( cd, model );
            cdp.getCycleProperties().createCycle( cd );
            cdp.getDivisionProperties().createDivision( cd, model );
            cdp.getDeathProperties().createDeath( cd );
            cdp.getSecretionsProperties().createSecretion( cd );
            cdp.getMotilityProperties().createMotility( cd );
            cdp.getInteractionsProperties().createCellInteractions( cd, model );
            cdp.getTransformationsProperties().createCellTransformations( cd, model );
            cdp.getCustomDataProperties().createCustomData( cd );
            cdp.getIntracellularProperties().createIntracellular( cd, model );
            cdp.getRulesProperties().createRules( cd, model );
            cdp.getIntegrityProperties().createIntegrity( cd );
            cdp.getInitialDistributionProperties().createInitialDistribution( cd, model );

            FunctionsProperties fp = cdp.getFunctionsProperties();
            CellFunctions f = cd.functions;
            f.updatePhenotype = getFunction( fp.getPhenotypeUpdate(), fp.getPhenotypeUpdateCustom(), UpdatePhenotype.class, model );
            f.updateVelocity = getFunction( fp.getVelocityUpdate(), fp.getVelocityUpdateCustom(), UpdateVelocity.class, model );
            f.updateVolume = getFunction( fp.getVolumeUpdate(), fp.getVolumeUpdateCustom(), VolumeUpdate.class, model );
            f.customCellRule = getFunction( fp.getCustomRule(), fp.getCustomRuleCustom(), CustomCellRule.class, model );
            f.membraneDistanceCalculator = getFunction( fp.getMembraneDistance(), fp.getMembraneDistanceCustom(), DistanceCalculator.class,
                    model );
            f.contact = getFunction( fp.getContact(), fp.getContactCustom(), Contact.class, model );
            f.membraneInteraction = getFunction( fp.getMembraneInteraction(), fp.getMembraneInteractionCustom(), MembraneInteractions.class,
                    model );
            f.set_orientation = getFunction( fp.getOrientation(), fp.getOrientationCustom(), set_orientation.class, model );
            f.updateMigration = getFunction( fp.getMigrationUpdate(), fp.getMigrationUpdateCustom(), UpdateMigrationBias.class, model );
            f.instantiator = getFunction( fp.getInstantiate(), fp.getInstantiateCustom(), Instantiator.class, model );
            f.cellDivision = getFunction( fp.getDivision(), fp.getDivisionCustom(), CellDivision.class, model );
        }

        if( getCustomReportGenerator() != null && !getCustomReportGenerator().isEmpty() )
            model.setReportGenerator( FunctionsLoader.load( getCustomReportGenerator(), ReportGenerator.class, log.getLogger(), model ) );

        model.disableAutomatedSpringAdhesions = emodel.getOptions().isDisableAutomatedAdhesions();
        model.signals.setupDictionaries( model );
        model.setupInitial();

        InitialCondition condition = emodel.getInitialCondition();
        if( condition.isCustomCondition() )
        {
            DataElementPath codePath = condition.getCustomConditionCode();
            if( codePath != null && !codePath.isEmpty() )
                FunctionsLoader.load( codePath, InitialCellsArranger.class, log.getLogger(), model ).arrange( model );
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

        if( emodel.getReportProperties().isCustomReport() )
        {
            DataElementPath dep = emodel.getReportProperties().getReportPath();
            model.setReportGenerator( FunctionsLoader.load( dep, ReportGenerator.class, log.getLogger(), model ) );
        }

        AgentColorer colorer = null;

        if( opts.isSaveCellsText() || opts.isSaveCellsTable()|| opts.isSaveImage() || opts.isSaveVideo() || opts.isSaveGIF() )
        {
            VisualizerProperties visualizerProperties = emodel.getVisualizerProperties();
            if( visualizerProperties.getProperties().length > 0 )
            {
                colorer = new DefaultColorer( visualizerProperties );
            }
            else
            {
                colorer = model.getDefaultColorer();
                if( colorer == null )
                {
                    colorer = new DefinitionVisualizer();
                    for( CellDefinitionProperties cd : emodel.getCellDefinitions() )
                        ( (DefinitionVisualizer)colorer ).setColor( cd.getName(), cd.getColor() );
                }
            }
            if( emodel.getReportProperties().isCustomVisualizer() )
            {
                DataElementPath dep = emodel.getReportProperties().getVisualizerPath();
                colorer = FunctionsLoader.load( dep, AgentColorer.class, log.getLogger(), model );
            }
        }

        if (opts.isSaveCellsTable())
        {
            VisualizerTextTable tableVisualzer = new VisualizerTextTable(opts.getResultPath(), cellUpdateType, colorer, opts.getFinalTime(), opts.getImageInterval());
            ((PhysicellSimulator)simulator).addTableVisualizer( tableVisualzer );
        }
        
        if (opts.isSaveCellsText())
        {
            VisualizerText textVisualzer = new VisualizerText(opts.getResultPath(), cellUpdateType, colorer, opts.getFinalTime(), opts.getImageInterval());
            ((PhysicellSimulator)simulator).addTextVisualizer( textVisualzer );
        }
        
        if( opts.isSaveImage() || opts.isSaveVideo() || opts.isSaveGIF() )
        {
            if( options.isUse2D() )
            {
                for( String density : m.densityNames )
                {
                    Visualizer2D visualizer2D = new Visualizer2D( null, density, Section.Z, 0 );
                    visualizer2D.setStubstrateIndex( m.findDensityIndex( density ) ).setAgentColorer( colorer );
                    visualizer2D.setSaveImage( opts.isSaveImage() );
                    model.addVisualizer( visualizer2D );
                }
            }
            else
                model.addVisualizer( new Visualizer3D( null, "3d", m ).setAgentColorer( colorer ) );

            for( Visualizer v : model.getVisualizers() )
            {
                if( opts.isSaveGIF() )
                    v.addResultGenerator( new GIFGenerator( TempFiles.file( v.getName() + ".gif" ) ) );

                if( opts.isSaveVideo() )
                    v.addResultGenerator( new VideoGenerator( TempFiles.file( v.getName() + ".mp4" ) ) );
            }
        }

        for( EventProperties eventProperties : emodel.getEvents() )
        {
            Event event = FunctionsLoader.load( eventProperties.getExecutionCodePath(), Event.class, this.log.getLogger(), model );
            event.executionTime = eventProperties.getExecutionTime();
            model.addEvent( event );
        }

        model.init( false );
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
            String value = row[3].toString();
            CellDefinition cd = model.getCellDefinition( value );
            if( cd == null )
            {
                int code = (int)Double.parseDouble( row[3].toString() );
                cd = model.getCellDefinition( code );
            }
            Cell cell = Cell.createCell( cd, model, new double[] {x, y, z} );

            //additional properties
            for( int i = 4; i < row.length; i++ )
            {
                String val = row[i].toString();
                if( val.equals( "skip" ) )
                    continue;
                String colName = tdc.getColumnModel().getColumn( i ).getName();
                if( colName.equals( "volume" ) )
                    cell.setTotalVolume( Double.parseDouble( row[i].toString() ) );
                else
                    cell.getModel().signals.setSingleBehavior( cell, colName, Double.parseDouble( row[i].toString() ) );
            }
        }
    }

    public <T extends Function> T getFunction(String standardFunction, DataElementPath path, Class<T> c,
            ru.biosoft.physicell.core.Model model) throws Exception
    {
        if( PhysicellConstants.CUSTOM.equals( standardFunction ) )
        {
            return FunctionsLoader.load( path, c, this.log.getLogger(), model );
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
            {
                return c.cast( f );
            }
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
        try
        {
            Object plotsObj = diagram.getAttributes().getValue( PLOTS );
            if( plotsObj instanceof PlotProperties )
                ( (PhysicellSimulator)simulator ).setPlotProperties( (PlotProperties)plotsObj );
            this.simulator.start( model, null, resultListeners, jobControl );
            return "";
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            return "";
        }
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

    @PropertyName ( "Show plot" )
    public boolean getNeedToShowPlot()
    {
        return false;
    }

    @Override
    public boolean hasVariablesToPlot()
    {
        return false;
    }

    @Override
    public List<String> getIncorrectPlotVariables()
    {
        return new ArrayList<String>();
    }

    @Override
    public ResultListener[] getListeners()
    {
        return new ResultListener[0];
    }

    @Override
    public SimulationResult generateSimulationResult()
    {
        return null;
    }

    @Override
    public String[] getVariableNames()
    {
        return new String[0];
    }
    
    public static final String PLOTS = "Plots";
    
    @Override
    public Object getPlotsBean(Diagram diagram)
    {
        Role role = diagram.getRole();
        if (!(role instanceof MulticellEModel))
            return null;
        Object plotsObj = diagram.getAttributes().getValue( PLOTS );
        PlotProperties result = null;
        if( ! ( plotsObj instanceof PlotProperties ) )
        {
            result = new  PlotProperties ( (MulticellEModel)role );
            diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( PLOTS, PlotProperties.class, result ) );
        }
        else
            result = (PlotProperties)plotsObj;
        return result;
    }
}