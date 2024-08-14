package biouml.plugins.optimization;

import java.util.logging.Level;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ObjectPool.PooledObject;
import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.plugins.simulation.ParallelSimulationEngine;
import biouml.plugins.simulation.ParallelSimulationEngine.ModelEngine;
import biouml.plugins.simulation.ParallelSimulationEngine.SimulationTaskFactory;
import biouml.plugins.simulation.SimulationTask;
import biouml.plugins.simulation.SimulationTaskParameters;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import biouml.standard.simulation.plot.Series.SourceNature;
import biouml.standard.state.State;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.JobControl;

public class SingleExperimentParameterEstimation implements OptimizationProblem
{
    private final Logger log = Logger.getLogger(SingleExperimentParameterEstimation.class.getName());

    private ParallelSimulationEngine engine;

    private final OptimizationExperiment experiment;
    private final List<OptimizationConstraint> constraints;

    private final List<Parameter> fParams;
    private final String[] experimentParamNames;

    private int evaluations;

    private Diagram originalDiagram;

    public SingleExperimentParameterEstimation(SimulationTaskParameters simulationParameters, OptimizationExperiment experiment,
            List<Parameter> params, List<OptimizationConstraint> constraints)
    {
        this.experiment = experiment;
        this.fParams = params;
        this.constraints = constraints;
        experimentParamNames = StreamEx.of(params).map(Parameter::getName).distinct().toArray(String[]::new);
        
        evaluations = 0;

        try
        {
            preprocess(simulationParameters, experiment);
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    public OptimizationExperiment getExperiment()
    {
        return experiment;
    }

    private void preprocess(SimulationTaskParameters simulationParameters, OptimizationExperiment experiment) throws Exception
    {
        originalDiagram = simulationParameters.getSimulationEngine().getOriginalDiagram();
        Diagram diagram = simulationParameters.getSimulationEngine().getDiagram();

        if( !experiment.getDiagramStateName().equals("") && !experiment.getDiagramStateName().equals("no state") )
        {
            State st = diagram.getState( experiment.getDiagramStateName() );
            if( st != null )
            {
                diagram.setNotificationEnabled( false );
                diagram.setStateEditingMode( st );
                diagram.setNotificationEnabled( true );
            }
        }

        Diagram cloneDiagram = diagram.clone(diagram.getOrigin(), diagram.getName());
        engine = new ParallelSimulationEngine(cloneDiagram, simulationParameters, initSimulationTaskFactory());

        diagram.restore();
    }

    @Override
    public void stop()
    {
        // Sometimes SingleExperimentParameterEstimation lives longer than necessary for analysis
        // let's remove engine link to save memory
        //engine = null;
    }

    private SimulationTaskFactory initSimulationTaskFactory()
    {
        return new SimulationTaskFactory()
        {
            @Override
            public SimulationTask createSimulationTask(String[] names)
            {
                return SimulationTaskRegistry.getSimulationTask(experiment, engine, names);
            }

            @Override
            public double[] processResult(Object result)
            {
                try
                {
                    if( result != null )
                    {
                        double distance = getDistance(result);
                        double penalty = getPenalty(result);
                        return new double[] {distance, penalty};
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not process the simulation result: " + ExceptionRegistry.log(e));
                }
                return new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            }
        };
    }

    private double getDistance(Object source) throws Exception
    {
        double result = 0;

        if( experiment.isSteadyState() )
            result = getSteadyStateDistance((Map<String, Double>)source);
        else
            result = getTimeCourseDistance((SimulationResult)source);

        if( Double.isNaN(result) )
            result = Double.POSITIVE_INFINITY;

        return result;
    }

    private double getTimeCourseDistance(SimulationResult sr) throws Exception
    {
        double result = 0;

        ExperimentalTableSupport expTableSupport = experiment.getTableSupport();
        TableDataCollection tdc = expTableSupport.getTable();

        String time = experiment.getVariableNameInFile("time");
        double[] times = TableDataCollectionUtils.getColumn(tdc, time);

        double timeLimit = engine.getEngineSettings().getSimulationEngine().getCompletionTime();
        int timeLimitIndex = 0;
        while (timeLimitIndex < times.length && times[timeLimitIndex] <= timeLimit)        
            timeLimitIndex++;              
        
        double[][] srMatrix = sr.interpolateLinear(times);
        Map<String, Integer> varMap = sr.getVariablePathMap();

        for( TableColumn column : tdc.getColumnModel() )
        {
            String columnName = column.getName();
            String codeName = experiment.getVariableNameInDiagram(columnName);

            if( !codeName.endsWith("time") && !codeName.isEmpty())//TODO: refactor so it will not iterate through all table columns!
            {
                int relativeTo = experiment.getRelativePoint(columnName);
                int ind = varMap.get(codeName);

                double[] values = new double[tdc.getSize()];
                Arrays.setAll( values, i -> srMatrix[i][ind] );

                result += expTableSupport.getDistance(values, columnName, relativeTo, experiment.getExactDataColumns(), timeLimitIndex);
            }
        }
        return result;
    }

    private double getSteadyStateDistance(Map<String, Double> stedyState)
    {
        ExperimentalTableSupport expTableSupport = experiment.getTableSupport();
        TableDataCollection tdc = expTableSupport.getTable();

        return tdc.columns().map( TableColumn::getName ).mapToDouble( columnName ->
        {
            String codeName = experiment.getVariableNameInDiagram(columnName);

            double[] values = new double[] {stedyState.get(codeName)};
            return expTableSupport.getDistance(values, columnName, -1, null, 1);
        }).sum();
    }

    private OptimizationConstraintCalculator calculator;
    public void setCalculator(OptimizationConstraintCalculator calculator)
    {
        this.calculator = calculator;
    }
    public OptimizationConstraintCalculator getCalculator()
    {
        return this.calculator;
    }

    private double getPenalty(Object source) throws Exception
    {
        double result = 0;
        if( constraints != null && constraints.size() > 0 )
        {
            result = StreamEx.of( constraints ).filter( experiment::isConstraintApplicable )
                    .mapToDouble( constraint -> getInaccuracy( constraint, source ) )
                    .map( inaccuracy -> inaccuracy * inaccuracy ).sum();
        }

        if( result < 1e-20 )
            result = 0;

        if( Double.isNaN(result) )
            result = Double.POSITIVE_INFINITY;

        return result;
    }

    private double getInaccuracy(OptimizationConstraint constraint, Object source)
    {
        if( calculator == null )
        {
            calculator = new OptimizationConstraintCalculator();
            try (PooledObject<ModelEngine> me = engine.allocModelEngine())
            {
                calculator.parseConstraints(constraints, me.get().getEngine().getDiagram());
            }
            catch( Exception ex )
            {
                throw ExceptionRegistry.translateException(ex);
            }
        }

        if( experiment.isSteadyState() )
        {
            return calculator.getConstraintInaccuracy(constraints.indexOf(constraint), (Map<String, Double>)source);
        }
        return calculator.getConstraintInaccuracy(constraints.indexOf(constraint), (SimulationResult)source,
                constraint.getInitialTime(), constraint.getCompletionTime());
    }

    @Override
    public List<Parameter> getParameters()
    {
        return fParams;
    }

    @Override
    public double[][] testGoodnessOfFit(double[][] values, JobControl jobControl) throws Exception
    {
        evaluations += values.length;
        return engine.simulate(values, experimentParamNames, jobControl);
    }

    @Override
    public double[] testGoodnessOfFit(double[] values, JobControl jobControl) throws Exception
    {
        evaluations++;
        return engine.simulate(values, experimentParamNames, jobControl);
    }

    @Override
    public int getEvaluationsNumber()
    {
        return evaluations;
    }

    public Object getSimulationResult(double[] values) throws Exception
    {
        return engine.getResult(values, this.experimentParamNames);
    }

    @Override
    public Object[] getResults(double[] values, DataCollection<?> origin) throws Exception
    {
        Object result = engine.getResult(values, this.experimentParamNames);

        if( originalDiagram.getType() instanceof CompositeDiagramType )
        {
            Map<String, Map<String, Double>> subDiagramChanges = new HashMap<>();
            Map<String, Double> mainChanges = new HashMap<>();
            Map<SubDiagram, String> subDiagramToState = new HashMap<>();
            HashSet<Object> optimizationResults = new HashSet<>();
            List<SubDiagram> subDiagrams = Util.getSubDiagrams(originalDiagram);
            for( SubDiagram subDiagram : subDiagrams )
            {
                subDiagramChanges.put(subDiagram.getName(), new HashMap<String, Double>());
            }

            for( int i = 0; i < experimentParamNames.length; i++ )
            {
                List<String> items = DiagramUtility.splitPath(experimentParamNames[i]);
                
                String parameterName; //= items.get(items.size() - 1);
                String subDiagramName; //= items.get(items.size() - 2);                
                
                if( items.size() > 1 )
                {
                    parameterName = items.get( items.size() - 1 );
                    subDiagramName = items.get( items.size() - 2 );
                }
                else
                {
                    parameterName = items.get( 0 );
                    subDiagramName = originalDiagram.getName();
                }

                Map<String, Double> changes = ( subDiagramName.equals(originalDiagram.getName()) ) ? mainChanges : subDiagramChanges
                        .get(subDiagramName);
                changes.put(parameterName, values[i]);
            }

            for( SubDiagram subDiagram : subDiagrams )
            {
                Map<String, Double> changes = subDiagramChanges.get(subDiagram.getName());
                State state = generateState(subDiagram.getDiagram(), changes, origin,
                        originalDiagram.getName() + "_" + subDiagram.getName() + "_" + experiment.getName() + "_state");
                optimizationResults.add(state);
                subDiagramToState.put(subDiagram, state.getName());
            }

            State mainState = generateState(originalDiagram, mainChanges, origin,
                    originalDiagram.getName() + "_" + experiment.getName() + "_state", subDiagramToState);

            optimizationResults.add(mainState);

            if( experiment.isTimeCourse() && result != null)
            {
                SimulationResult sr = ( (SimulationResult)result ).clone(origin, experiment.getName() + "_sr");
                sr.getAttributes().add(new DynamicProperty("statePath", String.class, DataElementPath.create(mainState).toString()));
                optimizationResults.add(sr);

                TableDataCollection tdc = experiment.getTableSupport().getTable();
                if( experiment.containsRelativeData() )
                {
                    tdc = generateExperimentalTable(sr, origin);
                    if( tdc != null )
                    {
                        optimizationResults.add(tdc);
                    }
                }
                optimizationResults.add(generateTimeCourseChart(origin, tdc, sr));
                optimizationResults.add(generateTimeCoursePlot(origin, tdc, sr));
            }

            return optimizationResults.toArray();
        }
        else
        {
            if( experiment.isSteadyState() && result != null)
            {
                State state = generateState(originalDiagram, (Map<String, Double>)result, origin, experiment.getName() + "_state");
                return new Object[] {state};
            }

            Map<String, Double> changes = new HashMap<>();
            for( int i = 0; i < values.length; ++i )
                changes.put(experimentParamNames[i], values[i]);

            State state = generateState(originalDiagram, changes, origin, experiment.getName() + "_state");

            if(result == null)
                return new Object[] {state};

            SimulationResult sr = ( (SimulationResult)result ).clone(origin, experiment.getName() + "_sr");
            sr.getAttributes().add(new DynamicProperty("statePath", String.class, DataElementPath.create(state).toString()));

            if( experiment.containsRelativeData() )
            {
                TableDataCollection tdc = generateExperimentalTable(sr, origin);
                if( tdc != null )
                {
                    return new Object[] {state, sr, tdc};
                }
            }

            TableDataCollection tdc = experiment.getTableSupport().getTable();
            ChartDataElement chart = generateTimeCourseChart(origin, tdc, sr);
            Plot plot = generateTimeCoursePlot(origin, tdc, sr);
            return new Object[] {state, sr, chart, plot};
        }
    }

    public void setApplyState(boolean val)
    {
        doApplyState = val;
    }

    private boolean doApplyState = false;

    private State generateState(Diagram diagram, Map<String, Double> changes, DataCollection<?> origin, String name) throws Exception
    {
       return generateState(diagram, changes, origin, name, null);
    }

    private State generateState(Diagram diagram, Map<String, Double> changes, DataCollection<?> origin, String name,
            Map<SubDiagram, String> subDiagramToState) throws Exception
    {
        EModel emodel = diagram.getRole( EModel.class );
        boolean modelNotification = emodel.isNotificationEnabled();
        boolean diagramNotification = diagram.isNotificationEnabled();
        emodel.setNotificationEnabled( true );
        diagram.setNotificationEnabled( true );

        State state = new State( origin, diagram, name );


        if( doApplyState )
            diagram.addState( state );

        diagram.setStateEditingMode( state );


        for( Map.Entry<String, Double> entry : changes.entrySet() )
        {
            emodel.getVariable( entry.getKey() ).setInitialValue( entry.getValue() );
        }

        if( diagram.equals( originalDiagram ) && subDiagramToState != null )
        {
            for( Map.Entry<SubDiagram, String> entry : subDiagramToState.entrySet() )
            {
                entry.getKey().setStateName( entry.getValue() );
            }
        }

        diagram.restore();

        emodel.setNotificationEnabled( modelNotification );
        diagram.setNotificationEnabled( diagramNotification );
        if( doApplyState )
            diagram.save();
        return state;
    }

    public TableDataCollection generateExperimentalTable(SimulationResult sr, DataCollection<?> origin) throws Exception
    {
        TableDataCollection tdc = experiment.getTableSupport().getTable();
        if( tdc != null )
        {
            TableDataCollection newTable = tdc.clone(origin, experiment.getName() + "_exact_values");

            String time = experiment.getVariableNameInFile("time");
            double[] times = TableDataCollectionUtils.getColumn(newTable, time);

            double[][] srMatrix = sr.interpolateLinear(times);

            List<ParameterConnection> connections = experiment.getParameterConnections();
            for( ParameterConnection connection : connections )
            {
                int relativeTo = connection.getRelativeTo();
                if( relativeTo != -1 )
                {
                    String nameInDiagram = connection.getVariableNameInDiagram();
                    int ind = sr.getVariablePathMap().get(nameInDiagram);
                    double baseValue = srMatrix[relativeTo][ind];

                    recalculateColumn(newTable, connection.getNameInFile(), baseValue);
                }
            }

            return newTable;
        }
        return null;
    }

    private void recalculateColumn(TableDataCollection table, String columnName, double baseValue)
    {
        int ind = table.getColumnModel().getColumnIndex(columnName);

        for( int i = 0; i < table.getSize(); ++i )
        {
            double oldValue = (Double)table.getValueAt(i, ind);
            double newValue = oldValue / 100 * baseValue;
            table.setValueAt(i, ind, newValue);
        }
    }

    private ChartDataElement generateTimeCourseChart(DataCollection<?> origin, TableDataCollection expTable, SimulationResult sr)
    {
        Chart chart = new Chart();

        String time = experiment.getVariableNameInFile("time");
        double[] expTimes = TableDataCollectionUtils.getColumn(expTable, time);
        double[] srTimes = sr.getTimes();

        int next = 0;

        for(ParameterConnection connection : experiment.getParameterConnections())
        {
            if(next > PlotsInfo.POSSIBLE_COLORS.length - 1)
                break;

            String nameInDiagram = connection.getNameInDiagram();
            if(nameInDiagram.equals("time"))
                continue;

            String nameInFile = connection.getNameInFile();
            String varNameInDiagram = connection.getVariableNameInDiagram();
            if (varNameInDiagram.isEmpty())
                continue;
            int srIndex = sr.getVariablePathMap().get(varNameInDiagram);
            int expIndex = expTable.getColumnModel().getColumnIndex( nameInFile );

            double[][] srData = new double[srTimes.length][2];
            for(int i = 0; i < srTimes.length; ++i)
            {
                srData[i][0] = srTimes[i];
                srData[i][1] = sr.getValues()[i][srIndex];
            }
            chart.addSeries( createSeries( nameInFile, srData, true, false, PlotsInfo.POSSIBLE_COLORS[next]) );

            double[][] expData = new double[expTimes.length][2];
            for(int i = 0; i < expTimes.length; ++i)
            {
                expData[i][0] = expTimes[i];
                Object objValue = expTable.getValueAt( i, expIndex );
                
                double value = Double.NaN;
                try
                {
                    if (objValue instanceof Double)
                        value = ( (Double)objValue ).doubleValue();
                    else if (objValue instanceof Integer)
                        value = ( (Integer)objValue ).doubleValue();
                    else if (objValue != null)
                        value = Double.parseDouble( objValue.toString() );
                }
                catch (Exception ex)
                {
                    log.log( java.util.logging.Level.SEVERE, "Error during experiment data retrieving ( "+value+" ): " + ex.getMessage(), ex );
                }
                expData[i][1] = value; 
            }
            chart.addSeries( createSeries( nameInFile + " exp", expData, false, true, PlotsInfo.POSSIBLE_COLORS[next]) );

            next++;
        }
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Time");
        options.getYAxis().setLabel("Quantity or concentration");
        chart.setOptions( options );
        return new ChartDataElement(experiment.getName() + "_chart", origin, chart);
    }

    private static ChartSeries createSeries(String name, double[][] data, boolean showLines, boolean showShapes, Color color)
    {
        ChartSeries series = new ChartSeries();
        series.getLines().setShow( showLines );
        series.getLines().setShapesVisible( showShapes );
        series.getBars().setShow( false );
        series.setData( data );
        series.setLabel( name );
        series.setColor( color );
        return series;
    }

    private Plot generateTimeCoursePlot(DataCollection<?> origin, TableDataCollection expTable, SimulationResult sr)
    {
        List<Series> seriesList = new ArrayList<>();

        String time = experiment.getVariableNameInFile("time");
        double[] expTimes = TableDataCollectionUtils.getColumn(expTable, time);
        double[] srTimes = sr.getTimes();

        int next = 0;

        for(ParameterConnection connection : experiment.getParameterConnections())
        {
            if(next > PlotsInfo.POSSIBLE_COLORS.length - 1)
                break;

            String nameInDiagram = connection.getNameInDiagram();
            if(nameInDiagram.isEmpty() || nameInDiagram.equals("time"))
                continue;

            String nameInFile = connection.getNameInFile();

            Series series = new Series();
            series.setXVar("time");
            series.setYVar(connection.getVariableNameInDiagram());
            series.setLegend(connection.getVariableNameInDiagram());
            series.setSource(DataElementPath.create(sr).toString());
            series.setSourceNature(SourceNature.SIMULATION_RESULT);
            series.setSpec(new Pen(1.0f, PlotsInfo.POSSIBLE_COLORS[next]));
            seriesList.add(series);

            series = new Series();
            series.setXVar("time");
            series.setYVar(nameInFile);
            series.setLegend(nameInFile);
            series.setSource(DataElementPath.create(expTable).toString());
            series.setSourceNature(SourceNature.EXPERIMENTAL_DATA);
            series.setSpec(new Pen(1.0f, PlotsInfo.POSSIBLE_COLORS[next]));
            seriesList.add(series);

            next++;
        }

        double startTime = srTimes[0] < expTimes[0] ? srTimes[0] : expTimes[0];
        double endTime = srTimes[srTimes.length - 1] > expTimes[expTimes.length - 1] ? srTimes[srTimes.length - 1] * 1.1 : expTimes[expTimes.length - 1] * 1.1;

        Plot plot = new Plot(origin, experiment.getName() + "_plot", seriesList);
        plot.setXFrom(startTime);
        plot.setXTo(endTime);
        plot.setXTitle("Time");
        plot.setYTitle("Quantity or concentration");
        return plot;
    }
}
