package biouml.model.javascript;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.data.xy.XYSeries;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineUtils;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorRegistry;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.plugins.javascript.Global;
import ru.biosoft.plugins.javascript.JSPlotGenerator;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.BeanUtil;

public class JavaScriptSimulationEngine extends JavaScriptHostObjectBase
{
    protected static final Logger log = Logger.getLogger( JavaScriptSimulationEngine.class.getName() );

    private SimulationEngine engine;
    private ResultWriter currentResults;

    public JavaScriptSimulationEngine()
    {
        engine = new JavaSimulationEngine();
    }

    public SimulationEngine getSimulationEngine()
    {
        return this.engine;
    }

    /**@return available solver names. */
    public String[] getSolvers()
    {
        return engine.getAvailableSolvers();
    }

    /** @return the name of the current solver.*/
    public String getSolver()
    {
        return engine.getSolverName();
    }

    /** Sets solver by its name.
     * @param name the solver name.*/
    public void setSolver(String name)
    {
        engine.setSolverName( name );
    }

    /** @return array of the current solver options.*/
    public String[] getOptions()
    {
        return getOptions( getSolver() );
    }

    /**
     * Gets options names for the specified solver.
     *
     * @param solver the solver name.
     * @return array of the solver options.
     */
    public String[] getOptions(String solver)
    {
        Simulator simulator;
        try
        {
            simulator = SimulatorRegistry.getSimulator( solver );
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException( "Solver with name " + solver + " was not found" );
        }
        return BeanUtil.properties( simulator.getOptions() ).map( Property::getName ).toArray( String[]::new );
    }

    /**
     * Gets value of the specified option of the current solver.
     *
     * @param optionName the option name.
     * @return the option value.
     */
    public String getOption(String optionName)
    {
        return getProperty( optionName, engine.getSimulator() ).getValue().toString();
    }

    /**
     * Sets value of the specified option of the current solver.
     *
     * @param optionName the option name.
     * @param value the option value.
     * @throws NoSuchMethodException
     */
    public void setOption(String optionName, Object value) throws NoSuchMethodException
    {
        getProperty( optionName, engine.getSimulator() ).setValue( value );
    }

    /**
     * Gets the specified option info for the current solver.
     *
     * @param optionName the option name
     * @return the option info.
     */
    public String getOptionInfo(String optionName)
    {
        return getOptionInfo( optionName, getSolver() );
    }

    /**
     * Gets info of the specified option for the specified solver.
     *
     * @param solver the solver name.
     * @param optionName the option name.
     * @return the option info.
     */
    public String getOptionInfo(String optionName, String solver)
    {
        Simulator simulator;
        try
        {
            simulator = SimulatorRegistry.getSimulator( solver );
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException( "Solver with name " + solver + " was not found" );
        }
        return getProperty( optionName, simulator ).getShortDescription();
    }

    private Property getProperty(String optionName, Simulator simulator)
    {
        ComponentModel model = ComponentFactory.getModel( simulator.getOptions() );
        Property prop = model.findProperty( optionName );
        if( prop == null )
            throw new IllegalArgumentException( "Option with name " + optionName + " was not found" );
        return prop;
    }

    /**
     * Creates span for the diagram simulation.
     *
     * @param startTime the initial time point of the simulation.
     * @param endTime the completion time point of the simulation.
     * @param timeIncrement the delta for time. The less increment the more calculations will be made by a solver
     * to get values of variables, but it is time consuming.
     * @return created UniformSpan in the case of non-zero time increment or created ArraySpan otherwise.
     */
    public Span createSpan(double startTime, double endTime, double timeIncrement)
    {
        return ( timeIncrement != 0.0 ) ? new UniformSpan( startTime, endTime, timeIncrement ) : new ArraySpan( startTime, endTime );
    }

    public void generateCode(Diagram diagram, boolean logging)
    {
        loadEngine(diagram);
        engine.setDiagram( diagram );
        engine.setLogLevel( Level.SEVERE );

        long time = System.nanoTime();
        try
        {
            if( logging )
                System.out.println( "Java code generation..." );

            ((JavaSimulationEngine)engine).generateModel(true);// .createModel();

            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "Error during model compilation", t );
        }
    }
    
    public Model compileModel(Diagram diagram, boolean logging)
    {
        loadEngine(diagram);
        return compileModel(diagram, engine, logging);
    }
    
    public Model compileModel(Diagram diagram, SimulationEngine engine, boolean logging)
    {
        engine.setDiagram( diagram );
        engine.setLogLevel( Level.SEVERE );

        long time = System.nanoTime();
        try
        {
            if( logging )
                System.out.println( "Model generation..." );

            Model model = engine.createModel();

            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );

            return model;
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "Error during model compilation", t );
            return null;
        }
    }

    public Model compileModel(Diagram diagram)
    {
        loadEngine(diagram);
        return compileModel( diagram, engine, true );
    }

    public SimulationResult simulateModel(Model model, Span span)
    {
        return simulateModel( model, span, true );
    }

    public SimulationResult simulateModel(Model model, Span span, boolean logging)
    {
        return simulateModel( model, span, logging, "Simulation_result" );
    }

    public SimulationResult simulateModel(Model model, Span span, boolean logging, String resultName)
    {
        long time = System.nanoTime();
        engine.setSpan( span );
        engine.setLogLevel( Level.SEVERE );

        try
        {
            model.init();

            if( logging )
                System.out.println( "Simulation..." );

            message = engine.simulate(model, getResultListeners(engine));
            if( message != null )
            {
                if( logging )
                    System.out.println( "Done with errors:" + message );
                //                return null;
                return getSimulationResult( engine, resultName );
            }

            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "Error during model simulation", t );
            return null;
        }

        return getSimulationResult( engine, resultName );
    }
    
    public SimulationEngine getEngine(Diagram diagram)
    {
        return DiagramUtility.getEngine(diagram);
    }
    
    public void loadEngine(Diagram diagram)
    {
        this.engine = DiagramUtility.getEngine(diagram);
    }
    
    private String message;
    public String getMessage()
    {
        return message;
    }
    public SimulationResult simulate(Diagram diagram, SimulationEngine engine, boolean logging)
    {
        if( !checkDiagram( diagram ) )
            return null;
        
        engine.setDiagram( diagram );
        engine.setLogLevel( Level.SEVERE );

        long time = System.nanoTime();
        try
        {
            if( logging )
                System.out.println( "Model generation..." );
            Model model = engine.createModel();
            
            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );

            if( model == null )
                log.log( Level.SEVERE, "Model was not generated!" );

            if( logging )
            {
                time = System.nanoTime();
                System.out.println( "Simulation..." );
            }
            message = engine.simulate( model, getResultListeners( engine ) );

            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "Error during model simulation", t );
        }

        return getSimulationResult( engine, diagram );
    }
    
    public SimulationResult simulateAndSave(Diagram diagram, SimulationEngine engine, boolean logging, DataCollection resultFolder,
            String resultName)
    {
        if( !checkDiagram( diagram ) )
            return null;

        engine.setDiagram( diagram );
        engine.setLogLevel( Level.SEVERE );

        long time = System.nanoTime();
        try
        {
            if( logging )
                System.out.println( "Model generation..." );
            Model model = engine.createModel();

            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );

            if( model == null )
                log.log( Level.SEVERE, "Model was not generated!" );

            if( logging )
            {
                time = System.nanoTime();
                System.out.println( "Simulation..." );
            }
            SimulationResult result = new SimulationResult( resultFolder, resultName );
            engine.initSimulationResult( result );
            message = engine.simulate( model, new ResultWriter[] {new ResultWriter( result )} );
            resultFolder.put( result );
            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );
            return result;
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "Error during model simulation", t );
            return null;
        }
    }
    
    /**
     * Simulates the diagram.
     * @param diagram the diagram to simulate.
     * @param span the simulation span. See 'createSpan' function for further details.
     * @return generated SimulationResult on success or null otherwise.
     */
    public SimulationResult simulate(Diagram diagram, Span span, boolean logging)
    {
        if( !checkDiagram( diagram ) )
            return null;

        loadEngine(diagram);
        engine.setDiagram( diagram );
        engine.setSpan( span );        
        engine.setLogLevel( Level.SEVERE );

        long time = System.nanoTime();
        try
        {
            if( logging )
                System.out.println( "Code generation..." );

            File[] files = SimulationEngineUtils.generateCode( (EModel)diagram.getRole(), engine );

            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );

            if( files == null )
                log.log( Level.SEVERE, "Code was not generated!" );

            if( logging )
            {
                time = System.nanoTime();
                System.out.println( "Code compilation..." );
            }

            Model model = (Model)engine.compileModel( files, true, engine.getOutputDir() )[0];

            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );

            if( model == null )
                log.log( Level.SEVERE, "Model was not generated!" );

            if( logging )
            {
                time = System.nanoTime();
                System.out.println( "Simulation..." );
            }
            message = engine.simulate(model, getResultListeners(engine));

            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "Error during model simulation", t );
        }

        return getSimulationResult( engine, diagram );
    }

    public SimulationResult simulate(Diagram diagram, Span span)
    {
        return simulate( diagram, span, true );
    }

    public SimulationResult simulate(Diagram diagram)
    {
        return simulate( diagram, new ArraySpan( 0, 100, 1 ) );
    }
       
    public ResultListener[] getResultListeners(SimulationEngine simulationEngine) throws Exception
    {
        SimulationResult res = new SimulationResult( null, "tmp" );
        simulationEngine.initSimulationResult( res );
        currentResults = new ResultWriter( res );
        return new ResultListener[] {currentResults};
    }

    public SimulationResult getSimulationResult(SimulationEngine simulationEngine, String name)
    {
        SimulationResult simulationResult = null;
        try
        {
            simulationResult = new SimulationResult( null, name );
            SimulationResult tmpResult = currentResults.getResults();
            simulationResult.setTimes( tmpResult.getTimes() );
            simulationResult.setValues( tmpResult.getValues() );
            simulationEngine.initSimulationResult( simulationResult );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Can not create simulation result.", e );
            return null;
        }
        return simulationResult;
    }

    public SimulationResult getSimulationResult(SimulationEngine simulationEngine, Diagram diagram)
    {
        SimulationResult simulationResult = null;
        try
        {
            simulationResult = new SimulationResult(null, diagram.getName() + "_result");
            SimulationResult tmpResult = currentResults.getResults();
            simulationResult.setTimes( tmpResult.getTimes() );
            simulationResult.setValues( tmpResult.getValues() );
            simulationEngine.initSimulationResult( simulationResult );
            //            simulationResult.setDiagramPath( diagram.getCompletePath() );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Can not create simulation result.", e );
            return null;
        }
        return simulationResult;
    }

    /**
     * Saves result of the diagram simulation by the specified path of ru.biosoft.access.core.DataCollection.
     *
     * @param simulationResult the simulation result to save.
     * @param dataElementPath the path to simulation result in repository.
     * @param description the simulation result description.
     */
    public void saveResults(SimulationResult simulationResult, String dataElementPath, String description)
    {
        if( simulationResult == null )
        {
            log.log( Level.SEVERE, "Simulation result is null." );
            return;
        }
        if( dataElementPath == null )
        {
            log.log( Level.SEVERE, "Incorrect data element path." );
            return;
        }
        try
        {
            DataElementPath path = DataElementPath.create( dataElementPath );
            DataCollection origin = path.optParentCollection();
            if( origin != null )
            {
                SimulationResult simulationResultCopy = simulationResult.clone( origin, path.getName() );
                simulationResultCopy.setDescription( description );
                origin.put( simulationResultCopy );
            }
            else
            {
                log.log( Level.SEVERE, "Can not get data collection '" + dataElementPath + "' to save simulation result '"
                        + simulationResult.getName() + "'." );
            }
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Can not save simulation result '" + simulationResult.getName() + "'.", e );
        }
    }

    /**
     * Saves result of the diagram simulation by the specified path as ru.biosoft.table.TableDataCollectionn.
     *
     * @param simulationResult the simulation result to save.
     * @param dataElementPath the path to simulation result table in repository.
     * @param description the simulation result description.
     */
    public void saveResultsTable(SimulationResult simulationResult, String dataElementPath, String description)
    {
        if( simulationResult == null )
        {
            log.log( Level.SEVERE, "Simulation result is null." );
            return;
        }
        if( dataElementPath == null )
        {
            log.log( Level.SEVERE, "Incorrect data element path." );
            return;
        }
        try
        {
            DataElementPath path = DataElementPath.create( dataElementPath );
            TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( path );
            ColumnModel cm = table.getColumnModel();
            cm.addColumn( "time", Double.class );

            Map<String, Integer> variableMap = simulationResult.getVariableMap();
            double[] times = simulationResult.getTimes();
            double[][] values = simulationResult.getValues();

            if( variableMap != null && times != null && values != null )
            {
                try
                {
                    List<Integer> indeces = new ArrayList<Integer>();
                    List<String> names = new ArrayList<String>();
                    for( Entry<String, Integer> entry : variableMap.entrySet() )
                    {
                        String name = entry.getKey();
                        if( !name.equals( "time" ) && !name.endsWith( "/time" ) )
                        {
                            names.add( name );
                            indeces.add( entry.getValue() );
                        }
                    }

                    // fill in attribute names
                    for( int i = 0; i < names.size(); i++ )
                    {
                        String columnName = names.get( i );
                        if( columnName.contains( "/" ) )
                            columnName = columnName.replaceAll( "/", ": " );
                        cm.addColumn( columnName, Double.class );
                    }


                    // now fill this table with values
                    int timeSliceNumber = times.length;

                    int rowId = 1;
                    for( int i = 0; i < timeSliceNumber; i++ )
                    {
                        Double[] vals = new Double[names.size() + 1];
                        vals[0] = times[i];
                        for( int j = 0; j < names.size(); j++ )
                            vals[j + 1] = values[i][indeces.get( j )];

                        TableDataCollectionUtils.addRow( table, "row_" + Integer.toString( rowId ), vals, true );
                        rowId++;
                    }
                }
                catch( Exception ex )
                {
                    log.log( Level.SEVERE, "An error occured when saving simulation result table " + path.getName() + " query: " + ex );
                }
            }

            table.finalizeAddition();
            CollectionFactoryUtils.save( table );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Can not save simulation result table '" + simulationResult.getName() + "'.", e );
        }
    }


    public void saveResults(SimulationResult simulationResult, String dataElementPath)
    {
        this.saveResults( simulationResult, dataElementPath, null );
    }

    /**
     * Plots result of the diagram simulation.
     *
     * @param simulationResult the simulation result to plot.
     * @param variables the identifiers of diagram variables to plot.
     * @param lineSpec the argument that defines three components used for lines specification: line style, marker symbol and color.
     * Processing of this option is not implemented yet.
     */
    public BufferedImage doPlotResults(SimulationResult simulationResult, String[] variables, String[] lineSpec, boolean show)
    {
        if( simulationResult == null )
        {
            log.log( Level.SEVERE, "Can not plot. Simulation result is null." );
            return null;
        }

        if( variables == null || variables.length == 0 )
        {
            log.info( "Array of variables to plot is empty." );
            return null;
        }

        try
        {
            EModel emodel = null;
            if( simulationResult.getDiagramPath() != null )
            {
                Diagram diagram = simulationResult.getDiagramPath().optDataElement( Diagram.class );
                if( diagram != null && diagram.getRole() instanceof EModel )
                {
                    emodel = (EModel)diagram.getRole();
                }
            }

            String[] titles = new String[variables.length];
            for( int i = 0; i < titles.length; ++i )
            {
                titles[i] = variables[i];
                if( emodel != null && emodel.getVariable( titles[i] ) instanceof VariableRole )
                {
                    VariableRole var = (VariableRole)emodel.getVariable( titles[i] );
                    titles[i] = var.getDiagramElement().getTitle();
                }
            }

            Map<String, Integer> variablesMap = simulationResult.getVariableMap();

            double[] times = simulationResult.getTimes();
            double[][] values = simulationResult.getValues();

            List<XYSeries> series = new ArrayList<>();
            for( int i = 0; i < variables.length; ++i )
            {
                int ind = variablesMap.get( variables[i] );
                double[] variableValues = StreamEx.of( values ).mapToDouble( val -> val[ind] ).toArray();
                series.add( JSPlotGenerator.createLineSeries( titles[i], times, variableValues ) );
            }

            BufferedImage plotImage = JSPlotGenerator.generatePlot( "Time", "Quantity or concentration", series, null );
            
            if( show )
            {
                ScriptEnvironment environment = Global.getEnvironment();
                if( environment != null )

                    environment.showGraphics( plotImage );
            }
            return plotImage;
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Error occured when creating plot for simulation result " + simulationResult.getName() + " : "
                    + ExceptionRegistry.log( ex ) );
        }
        return null;
    }
    
    public BufferedImage doPlotResults2(SimulationResult simulationResult, String[] variables, TableDataCollection tdc, String timeColumn, String[] columns)
    {
        if( simulationResult == null )
        {
            log.log( Level.SEVERE, "Can not plot. Simulation result is null." );
            return null;
        }

        if( variables == null || variables.length == 0 )
        {
            log.info( "Array of variables to plot is empty." );
            return null;
        }

        try
        {
            boolean usePaths = false;
            
            Set<String> titlesSet = new HashSet<>();
            for( int i = 0; i < variables.length; ++i )
            {
                String title = variables[i].contains( "/" ) ? variables[i].substring( variables[i].lastIndexOf( "/" ) + 1 ) : variables[i];
                if( titlesSet.contains( title ) ) //there are two variables with equal names but different paths
                {
                    usePaths = true;
                    break;
                }
                titlesSet.add( title );
            }        
            
            
            String[] titles = new String[variables.length];
            for( int i = 0; i < titles.length; ++i )
            {
                if( usePaths )
                    titles[i] = variables[i];
                else
                    titles[i] = variables[i].contains( "/" ) ? variables[i].substring( variables[i].lastIndexOf( "/" )+1 ): variables[i];
                titlesSet.add(titles[i]);
            }
            Map<String, Integer> variablesMap = simulationResult.getVariablePathMap();

            double[] times = simulationResult.getTimes();
            double[][] values = simulationResult.getValues();

            List<String> seriesTypes = new ArrayList<>();
            List<XYSeries> series = new ArrayList<>();
            for( int i = 0; i < variables.length; ++i )
            {
                int ind = variablesMap.get( variables[i] );
                double[] variableValues = StreamEx.of( values ).mapToDouble( val -> val[ind] ).toArray();
                series.add( JSPlotGenerator.createLineSeries( titles[i], times, variableValues ) );
                seriesTypes.add( JSPlotGenerator.TYPE_LINE );
            }

            if( tdc != null )
            {
                double[] timeValues = TableDataCollectionUtils.getColumn( tdc, timeColumn );
                for( int i = 0; i < columns.length; i++ )
                {
                    double[] columnValues = TableDataCollectionUtils.getColumn( tdc, columns[i] );
                    series.add( JSPlotGenerator.createLineSeries( columns[i], timeValues, columnValues ) );
                    seriesTypes.add( JSPlotGenerator.TYPE_EXPERIMENT );
                }
            }
            BufferedImage plotImage = JSPlotGenerator.generatePlot( "Time", "Quantity or concentration", series, seriesTypes );
            return plotImage;
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Error occured when creating plot for simulation result " + simulationResult.getName() + " : "
                    + ExceptionRegistry.log( ex ) );
        }
        return null;
    }
    
    public BufferedImage createPlot(SimulationResult simulationResult, String[] variables)
    {
        return doPlotResults( simulationResult, variables, null, false );
    }
    
    public BufferedImage createPlot2(SimulationResult simulationResult, String[] variables, TableDataCollection table, String[] columns)
    {
        return doPlotResults2( simulationResult, variables, table, "time", columns );
    }
    
    public BufferedImage createPlot2(SimulationResult simulationResult, String[] variables)
    {
        return doPlotResults2( simulationResult, variables, null, null, null );
    }
    
    public void plotResults(SimulationResult simulationResult, String[] variables, String[] lineSpec)
    {
        doPlotResults( simulationResult, variables, lineSpec, true );
    }

    public void plotResults(SimulationResult simulationResult, String[] variables)
    {
        this.plotResults( simulationResult, variables, null );
    }

    private boolean checkDiagram(Diagram diagram)
    {
        if( diagram == null )
        {
            log.log( Level.SEVERE, "The diagram is null." );
            return false;
        }

        if( ! ( diagram.getRole() instanceof EModel ) )
        {
            log.log( Level.SEVERE, "Invalid executable model of the diagram " + diagram.getName() );
            return false;
        }
        return true;
    }

    public void writeResult(SimulationResult result, String path, String delimiter)
    {
        this.writeResult( result, path, delimiter, true );
    }

    public void writeResult(SimulationResult result, String path, String delimiter, boolean logging)
    {
        if( logging )
            System.out.println( "Start to write" );
        long time = System.nanoTime();
        File resultFile = new File( path );
        double[] times = result.getTimes();
        try (PrintWriter pw = new PrintWriter( resultFile, "UTF-8" ))
        {
            StringBuilder firstLine = new StringBuilder();
            firstLine.append( "time" );

            Map<Integer, String> inverted = new TreeMap<>();
            result.getVariableMap().entrySet().forEach( e -> inverted.put( e.getValue(), e.getKey() ) );
            String[] varNames = inverted.values().toArray( new String[inverted.size()]);

            for( String var : varNames )
            {
                firstLine.append( delimiter );
                firstLine.append( var );
            }
            pw.println( firstLine.toString() );

            for( int i = 0; i < times.length; i++ )
            {
                double[] values = result.getValue( i );
                StringBuilder line = new StringBuilder();
                line.append( times[i] );

                for( double value : values )
                {
                    line.append( delimiter );
                    line.append( value );
                }
                pw.println( line.toString() );
            }
        }
        catch( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            if( logging )
                System.out.println( "Done. Elapsed time: " + ( System.nanoTime() - time ) / 1E9 );
        }
    }

    public static void simulateAndApply(Diagram diagram, double endTime, double step) throws Exception
    {
        SimulationEngine engine = DiagramUtility.getEngine( diagram );
        engine.setDiagram( diagram );
        engine.setCompletionTime( endTime );
        SimulationResult result = new SimulationResult( null, "" );
        engine.setTimeIncrement( step );
        engine.simulate( result );
        Map<String, Integer> mapping = result.getVariablePathMap();

        double[] values = result.getValue( result.getCount() - 1 );
        Set<Diagram> diagrams = new HashSet<>();
        for( Entry<String, Integer> e : mapping.entrySet() )
        {
            if( e.getKey().contains( "time" ) )
                continue;

            String path = e.getKey();

            if( path.contains( "/" ) )
            {
                String varName = path.substring( path.lastIndexOf( "/" )+1 );
                SubDiagram subDiagram = Util.getSubDiagram( diagram, path );
                Diagram innerDiagram = (Diagram)DataElementPath.create( subDiagram.getDiagramPath() ).getDataElement();
                Variable var = innerDiagram.getRole( EModel.class ).getVariable( varName );
                int index = e.getValue();
                double value = values[index];
                var.setInitialValue( value );
                diagrams.add( innerDiagram );                
            }
            else
            {
                Variable var = diagram.getRole( EModel.class ).getVariable( path );
                        if (var ==null)
                            System.out.println( var );
                        else
                var.setInitialValue( values[e.getValue()] );
            }
        }
        for (Diagram d: diagrams)
            d.save();
        
        diagram.save();
    }
}