package biouml.plugins.optimization.analysis;

import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.ParameterConnection;
import biouml.plugins.optimization.ParameterEstimationProblem;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationTaskParameters;
import biouml.standard.type.SpecieReference;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationMethodInfo;
import ru.biosoft.analysis.optimization.OptimizationMethod.OptimizationMethodJobControl;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

@ClassIcon ( "resources/analysis.gif" )
public class ParameterFitting extends AnalysisMethodSupport<ParameterFittingParameters>
{
    private OptimizationMethodJobControl control;
    private PropertyChangeListener methodInfoListener;
    private OptimizationMethodInfo info;
    
    public ParameterFitting(DataCollection<?> origin, String name)
    {
        super(origin, name, new ParameterFittingParameters());
    }

    @Override
    public Diagram justAnalyzeAndPut() throws Exception
    {
        methodInfoListener = evt -> {
            if( evt.getPropertyName().equals(OptimizationMethodInfo.DEVIATION) )
            {
                log.info("Deviation: "+info.getDeviation());
            }
        };
        Diagram input = parameters.getDiagramPath().getDataElement(Diagram.class);
        TableDataCollection experimentTable = parameters.getExperimentPath().getDataElement(TableDataCollection.class);
        DataCollection<?> origin = parameters.getOutputDiagram().getParentCollection();
        String name = parameters.getOutputDiagram().getName();
        Diagram output = input.clone(origin, name);
        EModel emodel = output.getRole(EModel.class);

        TableDataCollection experiment = parameters.getExperimentPath().getDataElement(TableDataCollection.class);

        Set<String> observed = StreamEx.of(experiment.getNameList()).filter(n->emodel.containsVariable(n)).toSet();
        Set<String> fitting = emodel.getVariables().stream().map(v -> v.getName()).filter(n -> !n.startsWith("$") && !observed.contains(n))
                .collect( Collectors.toSet() );

        Set<String> producedGenes = findProducingGenes(output);
        producedGenes.retainAll(observed);
        
        Set<String> nonProducing = new HashSet<>( observed );
        nonProducing.removeAll(producedGenes);
        
        Set<String> nonProducingDegradation = StreamEx.of(nonProducing).map(obs->obs.substring(1)+"_degradation").toSet();
        Set<String> exceptDegradation = new HashSet<>( fitting );
        exceptDegradation.removeAll(nonProducingDegradation);

        switch( getParameters().getRegime() )
        {
            case ParameterFittingParameters.ALL_GENES:
            {
                log.info("First stage");
                optimize(output, experimentTable, producedGenes, exceptDegradation);

                log.info("Second stage");
                if( producedGenes.isEmpty() )
                    optimize(output, experimentTable, nonProducing, fitting); //try to use all parameters of the model
                else
                    optimize(output, experimentTable, nonProducing, nonProducingDegradation);
                break;
            }
            case ParameterFittingParameters.FEEDBACK_GENES:
            {
                log.info("First stage");
                optimize(output, experimentTable, producedGenes, exceptDegradation);
                break;
            }
            case ParameterFittingParameters.NONFEEDBACK_GENES:
            {
                log.info("First stage");
                optimize(output, experimentTable, nonProducing, nonProducingDegradation);
                break;
            }
        }
        return output;
    }


    private void optimize(Diagram output, TableDataCollection experimentTable, Set<String> observed, Set<String> fitting) throws Exception
    {
        log.info("Number of observed variables "+ observed.size());
        log.info("Number of fitting parameters "+ fitting.size());
        
        if (observed.isEmpty() || fitting.isEmpty())
            return;
        
        EModel emodel = output.getRole(EModel.class);
        List<Parameter> parametersToFit = StreamEx.of(fitting).map(p -> emodel.getVariable(p))
                .map(p -> new ru.biosoft.analysis.optimization.Parameter(p.getName(), p.getInitialValue(), 0, p.getInitialValue() * 10))
                .toList();

        OptimizationExperiment optimizationExperiment = new OptimizationExperiment(experimentTable.getName(),
                transformTable(experimentTable, parameters.getDataColumn(), StreamEx.of(observed).toList()));

        List<ParameterConnection> connections = new ArrayList<>();
        int i = 0;
        for( String experimentParameter : observed )
        {
            Variable variable = emodel.getVariable(experimentParameter);
            if( variable != null )
            {
                ParameterConnection connection = new ParameterConnection(optimizationExperiment, i);
                connection.setNameInDiagram(variable.getName());
                connection.setNameInFile(experimentParameter);
                connection.setDiagram(output);
                connection.setWeight(1.0);
                connections.add(connection);
                i++;
            }
        }

        if( i == 0 )
            log.log(Level.SEVERE, "No variables found in table");
        else
            log.info("Number of variables found in table: " + i);

        optimizationExperiment.setParameterConnections(connections);
        optimizationExperiment.setExperimentType("Steady state");

        OptimizationParameters optParams = new OptimizationParameters();
        optParams.setDiagram(output);
        optParams.setFittingParameters(parametersToFit);
        optParams.setOptimizationExperiments(Arrays.asList(optimizationExperiment));

        SimulationTaskParameters stp = optParams.getSimulationTaskParameters().get( optimizationExperiment.getName() );
        SimulationEngine engine = parameters.getEngineWrapper().getEngine();
        engine.setLogLevel( Level.SEVERE );
        stp.setSimulationEngine(engine);

        OptimizationMethod method = getParameters().getAlgorithm().createMethod();
       
        info = method.getOptimizationMethodInfo();
        info.addPropertyChangeListener(methodInfoListener);
        control = method.getJobControl();
        control.begin();

        ParameterEstimationProblem problem = new ParameterEstimationProblem(optParams);
        method.setOptimizationProblem(problem);

        double time = System.currentTimeMillis();
        double[] result = method.getSolution();

        log.info( ( System.currentTimeMillis() - time ) / 1000.0 + " seconds elapsed");
        log.info(DoubleStreamEx.of(result).joining("\t"));

        log.info("Deviation: " + method.getDeviation());
        output.getRole(EModel.class);

        for( int j = 0; j < parametersToFit.size(); j++ )
        {
            Parameter parameter = parametersToFit.get(j);
            output.getRole(EModel.class).getVariable(parameter.getName()).setInitialValue(result[j]);
        }
    }

    private void terminate()
    {
        if( control != null )
            control.terminate();
    }

    private TableDataCollection transformTable(TableDataCollection collection, String dataColumn, List<String> selectedVariables)
            throws Exception
    {
        TableDataCollection t = new StandardTableDataCollection(null, "experiment");

        Double[] values = new Double[selectedVariables.size()];
        for( int i = 0; i < selectedVariables.size(); i++ )
        {
            String var = selectedVariables.get(i);
            values[i] = Math.pow(2, Double.parseDouble(collection.get(var).getValue(dataColumn).toString()));
            t.getColumnModel().addColumn(var, DataType.Float);
        }
        TableDataCollectionUtils.addRow(t, "value", values);
        return t;
    }

    public Set<String> findProducingGenes(Diagram diagram)
    {
        return diagram.recursiveStream().select(Node.class)
                .filter(n -> n.getRole() instanceof VariableRole && n.getName().startsWith("G") && isModifier(n))
                .map(n -> n.getName()).toSet();
    }

    public static boolean isModifier(Node node)
    {
        return node.edges()
                .anyMatch(e -> e.getKernel() instanceof SpecieReference && ! ( (SpecieReference)e.getKernel() ).isReactantOrProduct());
    }

    @Override
    protected AnalysisJobControl createJobControl()
    {
        return new ParameterFittingJobControl(this);
    }

    public static class ParameterFittingJobControl extends AnalysisJobControl
    {
        public ParameterFittingJobControl(AnalysisMethodSupport<?> method)
        {
            super(method);
        }

        @Override
        public void terminate()
        {
            super.terminate();
            ( (ParameterFitting)method ).terminate();
        }
    }

}
