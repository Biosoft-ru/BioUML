package biouml.plugins.modelreduction;

import java.util.logging.Level;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.SpecieReference;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

public class KeyNodesSensitivityAnalysis extends SteadyStateAnalysis
{
    private boolean debug = false;
    private Diagram diagram;

    public KeyNodesSensitivityAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new KeyNodesSensitivityAnalysisParameters());
    }

    @Override
    public void setParameters(AnalysisParameters params) throws IllegalArgumentException
    {
        try
        {
            parameters = (KeyNodesSensitivityAnalysisParameters)params;
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException("Wrong parameters");
        }
    }

    @Override
    public KeyNodesSensitivityAnalysisParameters getParameters()
    {
        return (KeyNodesSensitivityAnalysisParameters)parameters;
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        TableDataCollection result = TableDataCollectionUtils
                .createTableDataCollection( ( (KeyNodesSensitivityAnalysisParameters)parameters ).getResult());
        justAnalyze(result);
        result.getOrigin().put(result);
        return result;
    }

    private static Diagram flat(Diagram diagram) throws Exception
    {
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        preprocessor.setNameStyle(CompositeModelPreprocessor.BIOUML_STYLE);
        diagram = preprocessor.preprocess(diagram, diagram.getOrigin(), diagram.getName()+"_flat");
        diagram.getOrigin().put(diagram);
        return diagram;
    }
    
    public void justAnalyze(TableDataCollection collection) throws Exception
    {
        engine = (JavaSimulationEngine)parameters.getEngineWrapper().getEngine();
        engine.setLogLevel( Level.SEVERE );

        diagram = engine.getDiagram();
        if( DiagramUtility.containModules(diagram) )
        {
            diagram = flat(diagram);
            engine.setDiagram(diagram);
        }
        collection.getColumnModel().addColumn("Title", DataType.Text);
        collection.getColumnModel().addColumn("Score feedback", DataType.Float);
        collection.getColumnModel().addColumn("Score general", DataType.Float);
        
        
        Set<String> producingGenes = this.findProducingGenes(diagram);
        Set<String> nonProducingGenes = this.findGenes(diagram);
        nonProducingGenes.removeAll(producingGenes);

        log.info("Total number of genes:" + (nonProducingGenes.size() + producingGenes.size()));
        log.info("Number of genes with feedback:" + producingGenes.size());


        producingGenes = StreamEx.of(producingGenes).map(g->g.substring(1)+"_activity").toSet();
        nonProducingGenes = StreamEx.of(nonProducingGenes).map(g->g.substring(1)+"_activity").toSet();
        
        getParameters().setValidationSize(10);
        //TODO: sort it out
        switch (getParameters().getSteadyStateVariables())
        {
            case KeyNodesSensitivityAnalysisParameters.ALL_VARIABLES:
            {
//                getParameters().setVariableNames(new String[0]);
                break;
            }
            case KeyNodesSensitivityAnalysisParameters.ALL_GENES:
            {
//                getParameters().setVariableNames(StreamEx.of(nonProducingGenes).append(producingGenes).toArray(String[]::new));
                break;
            }
            case KeyNodesSensitivityAnalysisParameters.PRODUCING_GENES:
            {
//                getParameters().setVariableNames(StreamEx.of(producingGenes).toArray(String[]::new));
                break;
            }
            default:
                break;
        }
        
        ExperimentData experimentData = initExperimentData(producingGenes);

        List<String> vars = getCandidades(experimentData).toList();
        log.info("Number of candidade nodes: " + vars.size());
        //if this parameter is set - only elements from the list are available for analysis
        if( getParameters().getKeyNodes() != null )
        {
            Set<String> keyNodes = StreamEx.of(getParameters().getKeyNodes().getDataElement(TableDataCollection.class).getNameList())
                    .toSet();
            vars = StreamEx.of(vars).filter(v -> keyNodes.contains(v) || keyNodes.contains(v.replace("$", ""))).toList();
            log.info("Number of keynodes in candidades: " + vars.size());
        }

        int varsNumber = vars.size();
       
        double step = 100.0 / varsNumber;
        double progress = 0;
        for( String variable : vars )
        {
            jobControl.pushProgress((int)progress, (int) ( progress + step ));
            progress += step;
            if( jobControl.isStopped() )
                return;
            VariableRole role = (VariableRole)diagram.getRole(EModel.class).getVariable(variable);

            producingGenes = getTargets(experimentData).toSet();
            double score1 = calculateScore(role, experimentData, producingGenes);
            double score2 = calculateScore(role, experimentData, nonProducingGenes);
            
            TableDataCollectionUtils.addRow(collection, role.getDiagramElement().getCompleteNameInDiagram(), new Object[] {role.getTitle(),
                    Double.valueOf(score1), Double.valueOf(score2)});

            jobControl.popProgress();
        }
    }

    private StreamEx<String> getTargets(ExperimentData experimentData)
    {
        Set<String> observed = experimentData.getVariableNames();
        EModel emodel = diagram.getRole(EModel.class);
        return StreamEx.of( emodel.getVariableRoles().stream() ).map( v -> v.getName() ).filter( n -> observed.contains( n ) );
    }
    
    private StreamEx<String> getCandidades(ExperimentData experimentData)
    {
        Set<String> observed = experimentData.getVariableNames();
        EModel emodel = diagram.getRole(EModel.class);
        return StreamEx.of( emodel.getVariableRoles().stream() ).map( v -> v.getName() ).filter( n -> !observed.contains( n ) );
    }

    public double calculateScore(VariableRole variable, ExperimentData data, Set<String> controlVariables) throws Exception
    {
        double value = variable.getInitialValue();
        boolean constant = variable.isConstant();
        boolean bc = variable.isBoundaryCondition();
        variable.setConstant(true);
        variable.setBoundaryCondition(true);
        variable.setInitialValue(0.0);

//        log.info("Variable "+ variable.getName());
        debug("Variable: " + variable.getName());
        double result = Double.NaN;

        if( data instanceof TimeCourseData )
        {
            ( (JavaSimulationEngine)engine ).setSpan( ( (TimeCourseData)data ).getSpan());
            SimulationResult sr = new SimulationResult(null, "");
            engine.simulate(sr);
            result = ( (TimeCourseData)data ).calculateDiff(sr);
        }
        else if( data instanceof SteadyStateData )
        {
            getParameters().setStartSearchTime(100);
            getParameters().setAbsoluteTolerance(1000);
            getParameters().setRelativeTolerance(1000);
            getParameters().setValidationSize(1);
            Map<String, Double> steadyState = findSteadyState(engine);
            
            if( steadyState == null )
                log.log(Level.SEVERE, "Steady state was not found.");
            else
                result = ( (SteadyStateData)data ).calculateDiff(steadyState, controlVariables);
        }

        variable.setConstant(constant);
        variable.setBoundaryCondition(bc);
        variable.setInitialValue(value);
        return result;
    }

    public ExperimentData initExperimentData(Set<String> genes) throws Exception
    {
        TableDataCollection collection = getParameters().getTable();
        EModel emodel = diagram.getRole(EModel.class);

        String columnName = getParameters().getNameColumn();

        Object[] column = columnName == null || columnName.equals("(none)") || columnName.isEmpty()
                ? StreamEx.of(collection.getNameList()).toArray() : TableDataCollectionUtils.getColumnObjects(collection, columnName);
        Set<String> names = StreamEx.of(column).map(obj -> obj.toString()).toSet();
        Set<String> unmatched = StreamEx.of(names).filter(n -> !emodel.containsVariable(n)).toSet();

        if( !unmatched.isEmpty() )
        {
            names.removeAll(unmatched);
            log.log(Level.SEVERE, "Variables not found in diagram: " + unmatched.size());
        }

        log.info("Variables used for analysis: " + names.size());

        Set<String> varNames = StreamEx.of(names).map(n -> emodel.getVariable(n).getName()).toSet();
        switch( getParameters().getType() )
        {
            case KeyNodesSensitivityAnalysisParameters.TYPE_STEADY_STATE:
            {
                Map<String, Double> steadyState = findSteadyState(engine);
                if( steadyState == null )
                    throw new Exception("Can not reach steady state.");
                return new SteadyStateData(EntryStream.of(steadyState).filter(e -> varNames.contains(e.getKey())).toMap());
            }
            case KeyNodesSensitivityAnalysisParameters.TYPE_INITIAL_VALUE:
            {
                return new SteadyStateData(
                        StreamEx.of(varNames).map(n -> emodel.getVariable(n)).toMap(v -> v.getName(), v -> v.getInitialValue()));
            }
            case KeyNodesSensitivityAnalysisParameters.TYPE_TABLE_SERIES:
            {
                double[][] matrix = TableDataCollectionUtils.getMatrix(collection,
                        TableDataCollectionUtils.getColumnIndexes(collection, getParameters().getDataColumns()));
                return new TimeCourseData(StreamEx.of(names).toArray(String[]::new), matrix,
                        collection.getColumnModel().getColumnIndex(getParameters().getTimeColumn()));
            }
            case KeyNodesSensitivityAnalysisParameters.TYPE_TABLE_VALUE:
            {
                String dataColumn = getParameters().getDataColumns()[0];
                Map<String, Double> data = StreamEx.of(names).toMap(n -> n, n -> {
                    try
                    {
                        return (Double)collection.get(n).getValue(dataColumn);
                    }
                    catch( Exception ex )
                    {
                        return Double.valueOf(0);
                    }
                });
                return new SteadyStateData(data);
            }
            default:
                break;
        }
        return null;
    }

    private class SteadyStateData extends ExperimentData
    {
        Map<String, Double> values;
        public SteadyStateData(Map<String, Double> data)
        {
            values = data;
        }

        public double calculateDiff(Map<String, Double> simulated, Set<String> controlVariables) throws Exception
        {
            double[] errors;
            if( controlVariables == null )
                errors = EntryStream.of(simulated).filter(e -> values.containsKey(e.getKey()))
                        .mapToDouble(e -> calcError(e.getValue(), values.get(e.getKey()))).toArray();
            else
                errors = EntryStream.of(simulated).filter(e -> values.containsKey(e.getKey()) && controlVariables.contains(e.getKey()))
                        .mapToDouble(e -> calcError(e.getValue(), values.get(e.getKey()))).toArray();
            
            return Stat.mean(errors);
        }

        @Override
        public Set<String> getVariableNames()
        {
            return values.keySet();
        }
    }

    private abstract class ExperimentData
    {
        public abstract Set<String> getVariableNames();
        public double calcError(Double simulated, Double experimental)
        {
            debug(simulated.toString() + "\t" + experimental.toString());
            return Math.abs(simulated - experimental)/ Math.max(Math.abs(experimental), Math.abs(simulated));
        }
    }

    private class TimeCourseData extends ExperimentData
    {
        private int timeIndex;
        protected String[] names;
        protected double[][] values;

        public TimeCourseData(String[] names, double[][] matrix, int timeIndex)
        {
            this.names = names;
            this.values = matrix;
            this.timeIndex = timeIndex;
        }

        public Span getSpan()
        {
            return new ArraySpan(values[timeIndex]);
        }

        public double calculateDiff(SimulationResult result) throws Exception
        {
            double[] timePoints = values[timeIndex];
            double[][] interpolated = result.interpolateLinear(timePoints);
            Map<String, Integer> resultIndex = result.getVariableMap();

            double error = 0;
            for( int i = 0; i < timePoints.length; i++ )
            {
                for( int j = 0; j < names.length; j++ )
                {
                    if( j == timeIndex )
                        continue;
                    int index = resultIndex.get(names[j]);
                    double simulatedValue = interpolated[i][index];
                    double experimentalValue = values[i][j];
                    error += calcError(simulatedValue, experimentalValue);
                }
            }
            return error;
        }
        @Override
        public Set<String> getVariableNames()
        {
            return StreamEx.of(names).toSet();
        }
    }

    public void debug(String message)
    {
        if( debug )
            log.info(message);
    }


    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }
    
    public Set<String> findProducingGenes(Diagram diagram)
    {
        return diagram.recursiveStream().select(Node.class)
                .filter(n -> n.getRole() instanceof VariableRole && n.getName().startsWith("G") && isModifier(n))
                .map( n -> n.getRole( VariableRole.class ).getName() ).toSet();
    }

    public Set<String> findGenes(Diagram diagram)
    {
        return diagram.recursiveStream().select( Node.class )
                .filter( n -> n.getRole() instanceof VariableRole && n.getName().startsWith( "G" ) )
                .map( n -> n.getRole( VariableRole.class ).getName() ).toSet();
    }
    
    public static boolean isModifier(Node node)
    {
        return node.edges()
                .anyMatch( e -> e.getKernel() instanceof SpecieReference && ! ( (SpecieReference)e.getKernel() ).isReactantOrProduct() );
    }
}
