package biouml.plugins.sedml;

import org.jlibsedml.AbstractTask;
import org.jlibsedml.Algorithm;
import org.jlibsedml.OneStep;
import org.jlibsedml.SedML;
import org.jlibsedml.Simulation;
import org.jlibsedml.SteadyState;
import org.jlibsedml.UniformTimeCourse;

import biouml.model.Compartment;
import biouml.model.Node;
import biouml.plugins.modelreduction.AlgebraicSteadyStateAnalysis;
import biouml.plugins.modelreduction.AlgebraicSteadyStateParameters;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.simulation.SimulationAnalysis;
import biouml.plugins.simulation.SimulationAnalysisParameters;
import biouml.plugins.simulation.SimulationEngineRegistry;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.stochastic.StochasticSimulationEngine;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;

public class SimulationTaskBuilder extends WorkflowBuilder
{
    private AbstractTask task;
    private SedML sedml;

    private Node inputNode;
    private Node outputNode;
    
    private String outputFolder;

    public SimulationTaskBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }

    public void setTask(AbstractTask task)
    {
        this.task = task;
    }
    
    public void setSedml(SedML sedml)
    {
        this.sedml = sedml;
    }

    public void setInputNode(Node inputNode)
    {
        this.inputNode = inputNode;
    }

    public void setOutputFolder(String outputFolder)
    {
        this.outputFolder = outputFolder;
    }

    public Node getOutputNode()
    {
        return outputNode;
    }

    @Override
    public void build()
    {
        Simulation simulation = sedml.getSimulation( task.getSimulationReference() );
        if(simulation instanceof SteadyState)
            buildSteadyState();
        else
            buildTimeCourse();
    }
    
    private void buildSteadyState()
    {
        AlgebraicSteadyStateAnalysis analysis = AnalysisMethodRegistry.getAnalysisMethod( AlgebraicSteadyStateAnalysis.class );
        AlgebraicSteadyStateParameters parameters = analysis.getParameters();
        parameters.setOutputType( AlgebraicSteadyStateParameters.OUTPUT_SIMULATION_RESULT_TYPE );
        parameters.setSolverName( AlgebraicSteadyStateParameters.KIN_SOLVER );
        Compartment analysisNode = addAnalysis( analysis );
        
        createOutputNode();

        addDirectedEdge( parent, inputNode, (Node)analysisNode.get( "inputPath" ) );
        addDirectedEdge( parent, (Node)analysisNode.get( "outputSimulationResult" ), outputNode );
    }

    public void buildTimeCourse()
    {
        SimulationAnalysis simulationAnalysis = AnalysisMethodRegistry.getAnalysisMethod( SimulationAnalysis.class );
        fillSimulationAnalysisParameters( simulationAnalysis.getParameters() );
        Compartment simulationNode = addAnalysis( simulationAnalysis );

        createOutputNode();

        addDirectedEdge( parent, inputNode, (Node)simulationNode.get( "modelPath" ) );
        addDirectedEdge( parent, (Node)simulationNode.get( "simulationResultPath" ), outputNode );
    }

    private void createOutputNode()
    {
        String title = getTitleForSedmlElement( task );
        outputNode = addDataElementNode( title, outputFolder + "/" + title );
    }

    private void fillSimulationAnalysisParameters(SimulationAnalysisParameters parameters)
    {
        Simulation simulation = sedml.getSimulation( task.getSimulationReference() );
        JavaSimulationEngine engine = new JavaSimulationEngine();
        Algorithm algorithm = simulation.getAlgorithm();
        if(algorithm != null)
        {
            
            String kisaoID = algorithm.getKisaoID();
            if(kisaoID != null && kisaoID.equals( "KISAO:0000241" ))
            {
                StochasticSimulationEngine stochasticEngine = new StochasticSimulationEngine();
                stochasticEngine.setSimulationNumber( 1 );
                engine = stochasticEngine;
            }
        }

        if( simulation instanceof UniformTimeCourse )
        {
            UniformTimeCourse uniformTimeCourse = (UniformTimeCourse)simulation;
            double inc = ( uniformTimeCourse.getOutputEndTime() - uniformTimeCourse.getOutputStartTime() )
                    / uniformTimeCourse.getNumberOfPoints();
            engine.setInitialTime( uniformTimeCourse.getInitialTime() );
            engine.setCompletionTime( uniformTimeCourse.getOutputEndTime() );
            engine.setTimeIncrement( inc );
            if( uniformTimeCourse.getOutputStartTime() > uniformTimeCourse.getInitialTime() )
                parameters.setOutputStartTime( uniformTimeCourse.getOutputStartTime() );
            // TODO: support KISAO algorithms
        }
        else if( simulation instanceof OneStep)
        {
            OneStep oneStep = (OneStep)simulation;
            engine.setInitialTime( 0 );
            engine.setCompletionTime( oneStep.getStep() );
            engine.setTimeIncrement( oneStep.getStep() );
            parameters.setSkipPoints( 1 );
        }
        else
            throw new RuntimeException( "Unsupported simulation type " + simulation.getClass().getSimpleName() );

        String engineName = SimulationEngineRegistry.getSimulationEngineName( engine );
        parameters.setSimulationEngineName( engineName );
        parameters.setSimulationEngine( engine );
    }
}
