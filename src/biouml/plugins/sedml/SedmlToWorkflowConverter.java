package biouml.plugins.sedml;

import java.util.Map;

import org.jlibsedml.Output;
import org.jlibsedml.Plot2D;
import org.jlibsedml.Report;
import org.jlibsedml.SEDMLDocument;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.items.DataElementType;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.standard.type.Type;

import ru.biosoft.jobcontrol.FunctionJobControl;

public class SedmlToWorkflowConverter
{
    private final DataElementPath workflowPath;
    private final SEDMLDocument sedml;
    private final DataElementPath modelCollectionPath;

    private Diagram workflow;
    private WorkflowSemanticController semanticController;

    public SedmlToWorkflowConverter(DataElementPath workflowPath, SEDMLDocument sedml, DataElementPath modelCollectionPath)
    {
        this.workflowPath = workflowPath;
        this.sedml = sedml;
        this.modelCollectionPath = modelCollectionPath;
    }

    public Diagram convertToWorkflow(FunctionJobControl jobControl) throws Exception
    {
        workflow = new WorkflowDiagramType().createDiagram( workflowPath.getParentCollection(), workflowPath.getName(), null );
        semanticController = (WorkflowSemanticController)workflow.getType().getSemanticController();

        addOutputFolder();
        if( jobControl != null )
            jobControl.setPreparedness( 5 );

        ListOfModelsBuilder modelsBuilder = new ListOfModelsBuilder( workflow, semanticController );
        modelsBuilder.setSedml( sedml.getSedMLModel() );
        modelsBuilder.setModelCollectionPath( modelCollectionPath );
        modelsBuilder.build();
        Map<String, Node> modelNodesById = modelsBuilder.getResultingNodes();
        Map<String, Diagram> diagramsById = modelsBuilder.getResultingDiagrams();
        if( jobControl != null )
            jobControl.setPreparedness( 30 );

        ListOfTasksBuilder tasksBuilder = new ListOfTasksBuilder( workflow, semanticController );
        tasksBuilder.setModelNodes( modelNodesById );
        tasksBuilder.setSedml( sedml.getSedMLModel() );
        tasksBuilder.setOutputFolder( "$Output folder$/Simulation results" );
        tasksBuilder.build();
        Map<String, Node> srNodesByTaskId = tasksBuilder.getSimulationResultNodes();
        if( jobControl != null )
            jobControl.setPreparedness( 60 );

        addOutputs( workflow, diagramsById, srNodesByTaskId );
        if( jobControl != null )
            jobControl.setPreparedness( 100 );

        return workflow;
    }

    private void addOutputFolder()
    {
        DataElementPath outputFolderPath = DataElementPath.create( workflowPath + " results" );
        WorkflowParameter parameter = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem( workflow, Type.ANALYSIS_PARAMETER );
        parameter.setName( "Output folder" );
        parameter.setType( VariableType.getType( DataElementPath.class ) );
        parameter.setDataElementType( DataElementType.getType( FolderCollection.class ) );
        parameter.setRole( WorkflowParameter.ROLE_OUTPUT );
        parameter.setDefaultValueString( outputFolderPath.toString() );
        Node result = parameter.getNode();
        workflow.put( result );
    }

    private void addOutputs(Diagram parent, Map<String, Diagram> diagrams, Map<String, Node> srNodesByTaskId) throws Exception
    {
        for( Output output : sedml.getSedMLModel().getOutputs() )
        {
            if( output instanceof Plot2D )
            {
                Plot2DBuilder builder = new Plot2DBuilder( workflow, semanticController );
                builder.setPlot2D( (Plot2D)output );
                builder.setSedml( sedml.getSedMLModel() );
                builder.setDiagrams( diagrams );
                builder.setSimulationResultNodes( srNodesByTaskId );
                builder.build();
            }
            else if( output instanceof Report )
            {
                ReportBuilder builder = new ReportBuilder( workflow, semanticController );
                builder.setReport( (Report)output );
                builder.setSedml( sedml.getSedMLModel() );
                builder.setDiagrams( diagrams );
                builder.setSimulationResultNodes( srNodesByTaskId );
                builder.build();
            }
        }
    }
}
