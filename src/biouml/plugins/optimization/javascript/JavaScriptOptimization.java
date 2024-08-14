package biouml.plugins.optimization.javascript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.NativeObject;

import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.optimization.ExperimentalTableSupport;
import biouml.plugins.optimization.ExperimentalTableSupport.WeightMethod;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationConstraintCalculator;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationExperiment.ExperimentType;
import biouml.plugins.optimization.OptimizationMethodRegistry;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.ParameterConnection;
import biouml.plugins.optimization.ParameterEstimationProblem;
import biouml.plugins.simulation.SimulationTaskParameters;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod.SRESOptMethodParameters;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

public class JavaScriptOptimization extends JavaScriptHostObjectBase
{
    protected static final Logger log = Logger.getLogger(JavaScriptOptimization.class.getName());

    public JavaScriptOptimization()
    {
    }

    public Optimization createOptimization(Object obj)
    {
        if( ! ( obj instanceof NativeObject ) )
        {
            log.log( Level.SEVERE, "Incorrect array of the optimization settings." );
            return null;
        }

        Optimization opt = null;

        NativeObject njo = (NativeObject)obj;

        if( !njo.containsKey( "diagram") )
        {
            log.log( Level.SEVERE, "Please specify a path to the diagram." );
            return null;
        }
        String diagramPath = njo.get( "diagram", scope ).toString();
        DataElement diagram = CollectionFactory.getDataElement(diagramPath);

        DataCollection<?> output = null;
        if( !njo.containsKey( "outputFolder") )
        {
            log.log( Level.SEVERE, "Please specify a path to save results." );
            return null;
        }
        String outputFolder = njo.get( "outputFolder", scope ).toString();
        try
        {
            DataElementPath outputPath = DataElementPath.create( outputFolder );
            DataCollectionUtils.createFoldersForPath( outputPath );
            DataCollectionUtils.createSubCollection( outputPath );
            output = outputPath.getDataCollection();
        }
        catch( Exception e )
        {
            log.log(Level.INFO, "Can not create output folder " + outputFolder + ". " + e);
        }

        if( checkOptimization(diagram, diagramPath) )
        {
            try
            {
                opt = Optimization.createOptimization(diagram.getName() + "_optimization", output, (Diagram)diagram);

                OptimizationMethod<?> optMethod = null;
                if( njo.containsKey( "method" ) )
                {
                    String methodName = njo.get( "method", scope ).toString();
                    if( methodName != null )
                        optMethod = OptimizationMethodRegistry.getOptimizationMethod( methodName );
                    
                    if( optMethod != null )
                    {
                        opt.setOptimizationMethod( optMethod );
                    }
                    else
                        log.log( Level.INFO, "Can not get optimization method '" + methodName + "'. The default method '"
                                + opt.getOptimizationMethod().getName() + "' will be used." );
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "An optimization creation error. " + t);
            }
        }
        return opt;
    }

    public Optimization createOptimization(String name, String databaseName, String diagramName, int optimizationMethod)
    {
        String diagramPath = "databases/" + databaseName + "/Diagrams/" + diagramName;
        DataElement diagram = CollectionFactory.getDataElement(diagramPath);
        Optimization opt = null;

        if( checkOptimization(diagram, diagramPath, optimizationMethod) )
        {
            try
            {
                String optimizationName = name + ".xml";

                opt = Optimization.createOptimization(optimizationName, null, (Diagram)diagram);
                OptimizationMethod<?> optMethod = processOptimizationMethod(optimizationMethod);

                if( optMethod != null )
                {
                    opt.setOptimizationMethod(optMethod);
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "An optimization creation error. " + t);
            }
        }
        return opt;
    }
    
    public Optimization createOptimization(String name, Diagram diagram, int optimizationMethod)
    {
        Optimization opt = null;

        try
        {
            String optimizationName = name + ".xml";

            opt = Optimization.createOptimization( optimizationName, null, (Diagram)diagram );
            OptimizationMethod<?> optMethod = processOptimizationMethod( optimizationMethod );

            if( optMethod != null )
            {
                opt.setOptimizationMethod( optMethod );
            }
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "An optimization creation error. " + t );
        }
        return opt;
    }
    
    public ParameterConnection createParameterConnection(TableDataCollection tdc, String column,String path, String variable)
    {
        ParameterConnection connection = new ParameterConnection();
        connection.setNameInDiagram(variable);
        connection.setSubdiagramPath( path );
        connection.setNameInFile(column);
        //connection.setRelativeTo(relativeTo);
        return connection;
    }

    private OptimizationMethod<?> processOptimizationMethod(int index)
    {
        String methodName = OptimizationMethodRegistry.getOptimizationMethodNames().toArray( String[]::new )[index];

        OptimizationMethod<?> method = OptimizationMethodRegistry.getOptimizationMethod(methodName);
        if( method != null )
            method = method.clone(method.getOrigin(), method.getName());

        return method;
    }

    
    public ParameterConnection createParameterConnection(TableColumn tableColumn, Variable diagramVariable, int relativeTo)
    {
        ParameterConnection connection = new ParameterConnection();
        connection.setNameInDiagram(diagramVariable.getName());
        connection.setNameInFile(tableColumn.getName());

        connection.setRelativeTo(relativeTo);
        return connection;
    }

    public void addExperiment(Optimization optimization, String name, String diagramState, String dataFilePath, int weightMethod,
            int experimentType, String cellLine, ParameterConnection[] parameterConnections)
    {
        if( checkExperiment(optimization, name, diagramState, dataFilePath, weightMethod, experimentType, parameterConnections) )
        {
            OptimizationExperiment exp = new OptimizationExperiment(name, DataElementPath.create(dataFilePath));
            exp.setDiagramStateName(diagramState);
            //exp.setFilePath(DataElementPath.createPath(dataFileName));
            exp.setCellLine(cellLine);

            List<ParameterConnection> connections = new ArrayList<>(Arrays.asList( parameterConnections ));
            exp.setParameterConnections(connections);

            List<String> weightMethods = ExperimentalTableSupport.WeightMethod.getWeightMethods();
            exp.setWeightMethod(weightMethods.get(weightMethod));

            List<String> expTypes = OptimizationExperiment.ExperimentType.getExperimentTypes();
            exp.setExperimentType(expTypes.get(experimentType));

            exp.initWeights();

            optimization.getParameters().getOptimizationExperiments().add(exp);
        }
    }

    public void addExperiment(Optimization optimization, Object obj)
    {
        if( ! ( obj instanceof NativeObject ) )
        {
            log.log( Level.SEVERE, "Incorrect array of the optimization experiment settings." );
            return;
        }

        NativeObject njo = (NativeObject)obj;

        if( checkExperiment( optimization, njo ) )
        {
            String name = njo.get( "name", scope ).toString();
            String path = njo.get( "data", scope ).toString();
            String experimentType = njo.get( "experimentType", scope ).toString();

            OptimizationExperiment exp = new OptimizationExperiment( name );
            exp.setExperimentType( experimentType );
            exp.setDiagram( optimization.getDiagram() );
            exp.setFilePath( DataElementPath.create( path ) );

            if( njo.containsKey( "weightMethod" ) )
            {
                String weightMethod = njo.get( "weightMethod", scope ).toString();
                if( checkWeightMethod( weightMethod ) )
                    exp.setWeightMethod( weightMethod );
            }
            else
                exp.setWeightMethod( "Mean square" );

            if( njo.containsKey( "diagramState" ) )
            {
                String diagramState = njo.get( "diagramState", scope ).toString();
                if( checkDiagramSate( optimization, diagramState ) )
                    exp.setDiagramStateName( diagramState );
            }
            else
                exp.setDiagramStateName( "no state" );

            if( njo.containsKey( "cellLine" ) )
                exp.setCellLine( njo.get( "cellLine", scope ).toString() );

            boolean isTimeColumnExist = false;

            List<ParameterConnection> connections = exp.getParameterConnections();
            for( ParameterConnection con : connections )
            {
                if( njo.containsKey( con.getNameInFile() ) )
                {
                    String param = njo.get( con.getNameInFile(), scope ).toString();
                    if( param.equals( "time" ) )
                        isTimeColumnExist = true;
                    Role dRole = optimization.getDiagram().getRole();
                    if( dRole instanceof EModel && ( (EModel)dRole ).getVariable( param ) != null )
                        con.setNameInDiagram( param );
                    else
                    {
                        log.log( Level.SEVERE,
                                "Can not find variable " + param + " in the diagram " + optimization.getDiagram().getName() );
                        return;
                    }
                }
                else
                {
                    log.log( Level.SEVERE, "Please specify the name of the parameter '" + con.getNameInFile() + "' in the diagram." );
                    return;
                }

                if( experimentType.equals( "Time course" ) && !isTimeColumnExist )
                {
                    log.log( Level.SEVERE, "Time column in the time course experiment does not init." );
                    return;
                }
            }

            List<OptimizationExperiment> newArray = new ArrayList<>( optimization.getParameters().getOptimizationExperiments() );
            newArray.add( exp );
            optimization.getParameters().setOptimizationExperiments( newArray );
        }
    }

    public void addExperiment(Optimization optimization, String name, String dataFileName,
            ParameterConnection[] parameterConnections)
    {
            OptimizationExperiment exp = new OptimizationExperiment(name, DataElementPath.create(dataFileName));
            List<ParameterConnection> connections = new ArrayList<>(Arrays.asList( parameterConnections ));
            exp.setParameterConnections(connections);
            exp.setWeightMethod(WeightMethod.toString( WeightMethod.EDITED ));
            exp.setExperimentType(ExperimentType.toString( ExperimentType.TIME_COURSE));
            exp.initWeights();
            optimization.getParameters().getOptimizationExperiments().add(exp);
    }

    public void addFittingParameter(Optimization optimization, String name, double value, double lowerBound, double upperBound)
    {
        Parameter param = new Parameter( name, value, lowerBound, upperBound );
        param.setParentDiagramName( optimization.getDiagram().getName() );
        optimization.getParameters().getFittingParameters().add( param );
    }

    public void addFittingParameter(Optimization optimization, Variable var, double lowerBound, double upperBound, String locality)
    {
        if( checkFittingParameter(optimization, var, lowerBound, upperBound) )
        {
            Parameter param = new Parameter(var.getName(), var.getInitialValue(), lowerBound, upperBound);
            if( var instanceof VariableRole )
            {
                String title = ( (VariableRole)var ).getDiagramElement().getTitle();
                param.setTitle(title);
            }
            param.setLocality(locality);
            optimization.getParameters().getFittingParameters().add(param);
        }
    }

    public void addConstraint(Optimization optimization, String formula, double startTime, double endTime)
    {
        OptimizationConstraint constraint = new OptimizationConstraint();
        constraint.setFormula(formula);
        constraint.setInitialTime(startTime);
        constraint.setCompletionTime(endTime);

        optimization.getParameters().getOptimizationConstraints().add(constraint);
    }

    public void setSimulationParameters(Optimization optimization, String expName, double startTime, double endTime, double timeIncrement)
    {
        SimulationTaskParameters stp = optimization.getParameters().getSimulationTaskParameters().get( expName );
        if( stp != null )
        {
            stp.getSimulationEngine().setInitialTime( startTime );
            stp.getSimulationEngine().setCompletionTime( endTime );
            stp.getSimulationEngine().setTimeIncrement( timeIncrement );
        }
    }

    public Object[] optimize(Optimization optimization)
    {
        OptimizationConstraintCalculator calculator = new OptimizationConstraintCalculator();

        try
        {
            checkOptimization(optimization, calculator);
            OptimizationParameters params = optimization.getParameters();
            OptimizationProblem problem = new ParameterEstimationProblem(params, calculator);
            OptimizationMethod<?> method = optimization.getOptimizationMethod();
            method.setOptimizationProblem(problem);
            double[] result = method.getSolution();
            if( optimization.getOrigin() != null )
            {
                DataElementPath resultPath = DataElementPath.create( optimization.getOrigin() );
                method.saveResults( resultPath, result, method.getDeviation(), method.getPenalty(), method.getOptimizationProblem().getEvaluationsNumber(), false, false );
            }
            return problem.getResults(result, null);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not simulate: " + e.getMessage());
        }
        return null;
    }

    private boolean checkOptimization(DataElement diagram, String path, int optimizationMethod)
    {
        if( optimizationMethod < 0 || optimizationMethod > 5 )
        {
            log.log(Level.SEVERE, "Incorrect number of the optimization method. Enter a number from 0 to 5.");
            return false;
        }
        return checkOptimization(diagram, path);
    }

    private boolean checkOptimization(DataElement diagram, String path)
    {
        if( ( diagram == null ) || ! ( diagram instanceof Diagram ) || ( ( (Diagram)diagram ).getRole() == null ) )
        {
            log.log(Level.SEVERE, "Can not create optimization. The diagram " + path + " is null or not contains a model. Choose another diagram.");
            return false;
        }
        else if( ( (Diagram)diagram ).getRole() == null || ! ( ( (Diagram)diagram ).getRole() instanceof EModel ) )
        {
            log.log(Level.SEVERE, "The diagram " + diagram.getName() + " can not be simulated. Choose another diagram.");
            return false;
        }
        return true;
    }

    private boolean checkFittingParameter(Optimization optimization, Variable var, double lowerBound, double upperBound)
    {
        List<Parameter> fParams = optimization.getParameters().getFittingParameters();
        for( Parameter param : fParams )
        {
            if( param.getName().equals(var.getName()) )
            {
                log.info("Parameter " + var.getName() + " has been already addet to the fitting set.");
                return false;
            }
        }
        if( var.getInitialValue() < lowerBound || var.getInitialValue() > upperBound )
        {
            log.log(Level.SEVERE, "The initial value is outside of the search space bounds.");
            return false;
        }
        if( lowerBound < 0 || upperBound < 0 )
        {
            log.log(Level.SEVERE, "The bounds must be the positive numbers. Enter another values for them.");
            return false;
        }
        return true;
    }

    private boolean checkExperiment(Optimization optimization, String name, String diagramState, String dataFilePath, int weightMethod,
            int experimentType, ParameterConnection[] parameterConnections)
    {
        if(!checkExperiment(optimization, name, dataFilePath, parameterConnections, experimentType))
            return false;

        if(!checkDiagramSate(optimization, diagramState))
            return false;

        //checking the weight method number correctness
        if( weightMethod < 0 || weightMethod > 2 )
        {
            log.log(Level.SEVERE, "Incorrect number of the weight method. Enter a number from 0 to 2.");
            return false;
        }

        //checking the experiment type number correctness
        if( experimentType < 0 || experimentType > 1 )
        {
            log.log(Level.SEVERE, "Incorrect number of the experiment type. Enter the number 0 or 1.");
            return false;
        }
        return true;
    }

    private boolean checkExperiment(Optimization optimization, NativeObject njo)
    {
        if( !njo.containsKey( "name" ) )
        {
            log.log( Level.SEVERE, "Please specify a name of the optimization experiment." );
            return false;
        }
        if( !njo.containsKey( "data" ) )
        {
            log.log( Level.SEVERE, "Please specify a path to the experimental data file." );
            return false;
        }
        if( !njo.containsKey( "experimentType" ) )
        {
            log.log( Level.SEVERE, "Please specify a type of the experimental data: 'Time course' or 'Steady state'" );
            return false;
        }

        String experimentType = njo.get( "experimentType", scope ).toString();
        if( !experimentType.equals( "Time course" ) && !experimentType.equals( "Steady state" ))
        {
            log.log(Level.SEVERE, "Incorrect experiment type. Available options: Time course, Steady state");
            return false;
        }

        if( !checkExperiment( optimization, njo.get( "name", scope ).toString(), njo.get( "data", scope ).toString() ) )
            return false;

        return true;
    }

    private boolean checkWeightMethod(String weightMethod)
    {
        if( !weightMethod.equals( "Mean" ) && !weightMethod.equals( "Mean square" ) && !weightMethod.equals( "Standard deviation" ) )
        {
            log.log( Level.SEVERE, "Incorrect weight method. Available options: Mean, Mean square, Standard deviation" );
            return false;
        }
        return true;
    }

    private boolean checkExperiment(Optimization optimization, String name, String dataFilePath, ParameterConnection[] parameterConnections, int experimentType)
    {
        if( !checkExperiment( optimization, name, dataFilePath ) )
            return false;

        TableDataCollection tableData = DataElementPath.create( dataFilePath ).optDataElement(TableDataCollection.class);

        //checking the correctness of the parameter connections
        if( tableData.getColumnModel().getColumnCount() != parameterConnections.length )
        {
            log.log(Level.SEVERE, "Number of the parameter connections does not coincide with the number of columns in the experimental data table.");
            return false;
        }
        else
        {
            Set<String> engagedNames = new TreeSet<>();
            boolean isTimeColumnDefined = false;
            for( ParameterConnection parameterConnection : parameterConnections )
            {
                engagedNames.add(parameterConnection.getNameInFile());
                if( parameterConnection.getNameInDiagram().equals("time") )
                    isTimeColumnDefined = true;
            }

            if( !isTimeColumnDefined && experimentType == 0 )
            {
                log.log(Level.SEVERE, "Time column in the time course experiment does not init.");
                return false;
            }

            for( TableColumn column : tableData.getColumnModel() )
            {
                if( !engagedNames.contains(column.getName()) )
                {
                    log.log(Level.SEVERE, "Connection for the column " + column.getName() + "of the experimental data table does not exist.");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean checkExperiment(Optimization optimization, String name, String dataFilePath)
    {
        List<OptimizationExperiment> experiments = optimization.getParameters().getOptimizationExperiments();

        //checking the new experiment name
        for( OptimizationExperiment exp : experiments )
        {
            if( exp.getName().equals(name) )
            {
                log.info("Experiment with the name " + name + " already exists. Enter another name for the experiment.");
                return false;
            }
        }

        //checking the data file existence
        TableDataCollection tableData = DataElementPath.create( dataFilePath ).optDataElement(TableDataCollection.class);
        if( tableData == null )
        {
            log.info("Can not get experimental data from the file " + dataFilePath);
            return false;
        }

        return true;
    }

    private boolean checkDiagramSate(Optimization optimization, String diagramState)
    {
        //checking the diagram state existence
        if( optimization.getDiagram().getState( diagramState ) == null )
        {
            log.info("Diagram " + optimization.getDiagram().getName() + " does not have the state " + diagramState
                    + ". Enter another name for the state.");
            return false;
        }
        return true;
    }

    private void checkOptimization(Optimization optimization, OptimizationConstraintCalculator calculator)
    {
        List<Parameter> fParams = optimization.getParameters().getFittingParameters();
        List<OptimizationExperiment> experiments = optimization.getParameters().getOptimizationExperiments();

        if( fParams.size() == 0 )
            throw new IllegalArgumentException("Set parameters to fit.");
        if( experiments.size() == 0 )
            throw new IllegalArgumentException("Set experimental data.");
        List<OptimizationConstraint> constraints = optimization.getParameters().getOptimizationConstraints();
        calculator.parseConstraints(constraints, optimization.getDiagram());
    }

    public double[] optimize(Diagram diagram, TableDataCollection tdc) throws Exception
    {
        Optimization opt = Optimization.createOptimization( "opt", null, (Diagram)diagram );
        OptimizationExperiment exp = new OptimizationExperiment( "exp", DataElementPath.create( tdc ) );
        ParameterConnection connection = new ParameterConnection();
        connection.setDiagram( diagram );
        connection.setSubdiagramPath( "Template_model" );
        connection.setNameInDiagram( "Registered" );        
        connection.setNameInFile( "total_cases" );      
        ParameterConnection connection2 = new ParameterConnection();
        connection2.setDiagram( diagram );
        connection2.setNameInDiagram( "time" );
        connection2.setNameInFile( "time" );
        List<ParameterConnection> connections = new ArrayList<>();
        connections.add( connection );
        connections.add( connection2 );
        exp.setParameterConnections( connections );
        exp.setWeightMethod( WeightMethod.toString( WeightMethod.EDITED ) );
        exp.setExperimentType( ExperimentType.toString( ExperimentType.TIME_COURSE ) );
        exp.initWeights();
        opt.getParameters().setOptimizationExperiments( StreamEx.of( exp).toList() );
        Parameter param = new Parameter("N", 0.1, 0.05, 0.5);
        param.setParentDiagramName( "Template_model" );
        opt.getParameters().getFittingParameters().add( param );
        
        OptimizationConstraintCalculator calculator = new OptimizationConstraintCalculator();
        OptimizationParameters params = opt.getParameters();
        OptimizationProblem problem = new ParameterEstimationProblem( params, calculator );
        OptimizationMethod<?> method = opt.getOptimizationMethod();
        method.setOptimizationProblem( problem );
        ((SRESOptMethodParameters)method.getParameters()).setNumOfIterations( 5);
        ((SRESOptMethodParameters)method.getParameters()).setSurvivalSize( 5 );
        return method.getSolution();        
    }

    public Map<String, Double> optimize(Diagram diagram, TableDataCollection tdc, Optimization optimization) throws Exception
    {
        Optimization opt = Optimization.createOptimization("opt", null, (Diagram)diagram);
        OptimizationExperiment exp = new OptimizationExperiment("exp", DataElementPath.create(tdc));
        List<ParameterConnection> newConnections = new ArrayList();
        OptimizationExperiment experiment = optimization.getParameters().getOptimizationExperiments().get(0);
        for( ParameterConnection con : experiment.getParameterConnections() )
        {
            if( con.getNameInDiagram().isEmpty() )
                continue;
            ParameterConnection connection = new ParameterConnection();
            connection.setDiagram(diagram);
            connection.setSubdiagramPath(con.getSubdiagramPath());
            connection.setNameInDiagram(con.getNameInDiagram());
            connection.setNameInFile(con.getNameInFile());
            newConnections.add(connection);
        }
        exp.setParameterConnections(newConnections);
        exp.setWeightMethod(WeightMethod.toString(WeightMethod.EDITED));
        exp.setExperimentType(ExperimentType.toString(ExperimentType.TIME_COURSE));
        exp.initWeights();
        opt.getParameters().setOptimizationExperiments(StreamEx.of(exp).toList());

        SimulationTaskParameters taskParams = optimization.getParameters().getSimulationTaskParameters().values().iterator().next();
        double completion = taskParams.getEngineWrapper().getEngine().getCompletionTime();
        opt.getParameters().getSimulationTaskParameters().get("exp").getEngineWrapper().getEngine().setCompletionTime(completion);

        for( Parameter p : optimization.getParameters().getFittingParameters() )
        {
            p.setParentDiagramName(diagram.getName());
            opt.getParameters().getFittingParameters().add(p);
        }

        OptimizationMethod<?> method = optimization.getOptimizationMethod().clone(null, "opt");
        OptimizationConstraintCalculator calculator = new OptimizationConstraintCalculator();
        OptimizationParameters params = opt.getParameters();
        OptimizationProblem problem = new ParameterEstimationProblem(params, calculator);
        method.setOptimizationProblem(problem);
        double[] values = method.getSolution();
        List<Parameter> fittingParams = opt.getParameters().getFittingParameters();
        Map<String, Double> result = new HashMap<>();
        for( int i = 0; i < fittingParams.size(); i++ )
            result.put(fittingParams.get(i).getName(), values[i]);
        return result;

    }
}
