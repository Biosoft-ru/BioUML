package biouml.plugins.sedml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx;

import org.jlibsedml.Change;
import org.jlibsedml.ChangeAttribute;
import org.jlibsedml.Model;
import org.jlibsedml.SedML;
import org.jlibsedml.XPathTarget;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.CopyDataElement;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethod;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.modelreduction.AlgebraicSteadyStateAnalysis;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowVariable;
import biouml.plugins.sedml.analyses.DownloadModel;
import biouml.plugins.sedml.analyses.DownloadModel.Parameters;
import biouml.plugins.simulation.SimulationAnalysis;
import biouml.plugins.state.analyses.ChangeDiagram;
import biouml.plugins.state.analyses.ChangeDiagramParameters;
import biouml.plugins.state.analyses.StateChange;
import biouml.standard.type.Type;

public class ListOfModelsParser extends WorkflowParser
{
    static final Pattern INITIAL_VALUE_PATTERN = Pattern.compile( "role/vars/([^/]*)/initialValue" );
    
    
    private Map<String, Node> modelNodes = new HashMap<>();
    private int lastModelId = 0;

    public ListOfModelsParser(Diagram workflow, SedML sedml)
    {
        super( workflow, sedml );
    }
    
    public Map<String, Node> getModelNodes()
    {
        return modelNodes;
    }

    @Override
    public void parse() throws Exception
    {
        parseModelsFromDownloadModel();
        parseModelsFromTerminalExpressions( ChangeDiagram.class, "diagramPath" );
        parseModelsFromTerminalExpressions( SimulationAnalysis.class, "modelPath" );
        parseModelsFromTerminalExpressions( AlgebraicSteadyStateAnalysis.class, "inputPath" );
        parseModelsFromTerminalExpressions( CopyDataElement.class, "src" );
    }

    private void parseModelsFromDownloadModel()
    {
        for( Compartment analysisNode : findAnalyses( DownloadModel.class ) )
        {
            DownloadModel.Parameters parameters = (Parameters)AnalysisDPSUtils.readParametersFromAttributes( analysisNode.getAttributes() );

            Node outputNode = (Node)analysisNode.get( "outputPath" );
            List<Change> changes = new ArrayList<>();
            if( outputNode.getEdges().length == 1 )
            {
                Edge edge = outputNode.getEdges()[0];
                if( edge.getInput() != outputNode )
                    continue;
                String outputType = edge.getOutput().getKernel().getType();
                if( outputType.equals( Type.TYPE_DATA_ELEMENT_IN ) )
                {
                    Compartment nextAnalysisNode = (Compartment)edge.getOutput().getOrigin();
                    if( isAnalysisNode( nextAnalysisNode, ChangeDiagram.class ) )
                    {
                        outputNode = (Node)nextAnalysisNode.get( "outputDiagram" );
                        changes = parseChanges( nextAnalysisNode );
                    }
                }
            }
            if( outputNode.getEdges().length == 1 )
            {
                Edge edge = outputNode.getEdges()[0];
                if( edge.getInput() != outputNode )
                    continue;
                if( isWorkflowVariable( edge.getOutput() ) )
                    outputNode = edge.getOutput();
            }

            String modelId = addModel( outputNode, parameters.getSource(), changes );
            parseDependentModels( modelId, outputNode );
        }
    }
    
    private void parseModelsFromTerminalExpressions(Class<? extends AnalysisMethod> analysisClass, String inPortName) throws Exception
    {
        for( Compartment analysisNode : findAnalyses( analysisClass ) )
        {
            Node inPort = (Node)analysisNode.get( inPortName );
            if( inPort.getEdges().length != 1 )
                continue;
            Edge edge = inPort.getEdges()[0];
            if( edge.getOutput() != inPort )
                continue;
            Node inputNode = edge.getInput();
            //Check that inputNode not yet processed
            if( StreamEx.ofValues( modelNodes ).has( inputNode ) )
                continue;
            if( WorkflowItemFactory.getWorkflowItem( inputNode ) == null )
                continue;
            if( inputNode.edges().anyMatch( e -> e.getOutput() == inputNode ) )
                continue;
            String modelId = parseModelFromExpression( inputNode );
            parseDependentModels( modelId, inputNode );
        }
    }

    private String parseModelFromExpression(Node inputNode) throws Exception
    {
        String modelSource = "";
        WorkflowVariable workflowVar = (WorkflowVariable)WorkflowItemFactory.getWorkflowItem( inputNode );
        if( workflowVar != null && workflowVar.getType().equals( VariableType.getType( DataElementPath.class ) ) )
        {
            DataElementPath modelPath = (DataElementPath)workflowVar.getValue();
            modelSource = modelPath.getName();
            if(!modelSource.endsWith( ".xml" ))
                modelSource = modelSource + ".xml";
        }
        return addModel( inputNode, modelSource, Collections.emptyList() );
    }
    
    private void parseDependentModels(String sourceModelId, Node sourceModelNode)
    {
        StreamEx.of( sourceModelNode.getEdges() )
            .filter( e->e.getInput() == sourceModelNode )
            .map( e->e.getOutput() )
            .filter( n->n.getKernel().getType().equals( Type.TYPE_DATA_ELEMENT_IN ) )
            .map( Node::getOrigin ).select( Compartment.class )
            .filter( n->isAnalysisNode( n, ChangeDiagram.class ) && !isDependsOnCycleVariable( n ) )
            .forEach( analysisNode->{
                    Node outputNode = (Node)analysisNode.get( "outputDiagram" );
                    if( outputNode.getEdges().length == 1 )
                    {
                        Edge edge = outputNode.getEdges()[0];
                        if( edge.getInput() == outputNode && isWorkflowVariable( edge.getOutput() ) )
                            outputNode = edge.getOutput();
                    }
                    List<Change> changes = parseChanges( analysisNode );
                    String modelId = addModel( outputNode, sourceModelId, changes );
                    parseDependentModels( modelId, outputNode );
            } );
    }

    private List<Change> parseChanges(Compartment changeDiagramNode)
    {
        List<Change> changes = new ArrayList<>();
        
        ComputeChangeParser ccParser = new ComputeChangeParser( workflow, sedml );
        ccParser.setAnalysisNode( changeDiagramNode );
        ccParser.parse();
        changes.addAll( ccParser.getChanges() );
        
        ChangeDiagramParameters changeDiagramParameters = (ChangeDiagramParameters)AnalysisDPSUtils
                .readParametersFromAttributes( changeDiagramNode.getAttributes() );
        StateChange[] stateChanges = changeDiagramParameters.getChanges();
        for(int i = 0; i < stateChanges.length; i++ )
        {
            if(ccParser.getUsedIndices().contains( i ))
                continue;
            StateChange stateChange = stateChanges[i];
            Matcher matcher = INITIAL_VALUE_PATTERN.matcher( stateChange.getElementProperty() );
            if( matcher.matches() && stateChange.getElementId().isEmpty() )
            {
                String varName = matcher.group( 1 );
                XPathTarget xPath = new XPathTarget( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"" + varName + "\"]/@value" );
                Change change = new ChangeAttribute( xPath, stateChange.getPropertyValue() );
                changes.add( change );
            }
        }
        return changes;
    }

    private String addModel(Node node, String source, List<Change> changes)
    {
        String id;
        String name = null;
        if(node.getKernel().getType().equals( Type.TYPE_DATA_ELEMENT_OUT ))
        {
            //TODO: check that auto generated id doesn't match user id
            id = "model_" + (++lastModelId);
        }
        else
        {
            IdName res = parseTitle( node.getName() );
            id = res.id;
            name = res.name;
            //TODO: check that parsed id doesn't match other ids
        }
        Model model = new Model( id, name, "urn:sedml:language:sbml", source == null ? "" : source );
        for(Change c : changes)
            model.addChange( c );
        sedml.addModel( model );
        modelNodes.put( model.getId(), node );
        return model.getId();
    }
}

