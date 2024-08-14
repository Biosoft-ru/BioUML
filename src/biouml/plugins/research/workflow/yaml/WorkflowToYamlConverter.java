package biouml.plugins.research.workflow.yaml;

import java.util.logging.Level;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.engine.CycleElement;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.items.DataElementType;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.standard.type.Type;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.util.BeanAsMapUtil;

public class WorkflowToYamlConverter
{
    private static final Logger log = Logger.getLogger( WorkflowToYamlConverter.class.getName() );

    public Map<String, Object> convert(Diagram workflow)
    {
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put( "name", workflow.getName() );
        if( workflow.getDescription() != null )
            yaml.put( "description", workflow.getDescription() );
        convertCompartment( workflow, yaml, workflow );
        return yaml;
    }

    private void convertCompartment(Compartment compartment, Map<String, Object> yaml, Diagram diagram)
    {
        Map<String, Object> parameters = new LinkedHashMap<>();
        Map<String, Object> expressions = new LinkedHashMap<>();
        Map<String, Object> tasks = new LinkedHashMap<>();
        Map<String, Object> cycles = new LinkedHashMap<>();
        for( DiagramElement de : compartment )
            if( de instanceof Node )
            {
                Node node = (Node)de;
                String kernelType = de.getKernel().getType();
                switch( kernelType )
                {
                    case Type.ANALYSIS_PARAMETER:
                        WorkflowParameter wfParam = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem( node );

                        LinkedHashMap<String, Object> param = new LinkedHashMap<>();
                        parameters.put( wfParam.getName(), param );

                        String type = wfParam.getType().getName();
                        if( !type.equals( "Data element" ) )
                            param.put( "type", type );
                        if( !wfParam.isDataElementTypeHidden() )
                        {
                            DataElementType dataElementType = wfParam.getDataElementType();
                            if( !dataElementType.getTypeClass().equals( ru.biosoft.access.core.DataElement.class ) )
                                param.put( "elementType", dataElementType.toString() );
                            String role = wfParam.getRole();
                            if( !role.equals( WorkflowParameter.ROLE_INPUT ) )
                                param.put( "role", role );
                        }
                        if( !wfParam.isReferenceTypeHidden() )
                            param.put( "referenceType", wfParam.getReferenceType() );
                        String defaultValue = wfParam.getDefaultValueString();
                        if( defaultValue != null && !defaultValue.isEmpty() )
                            param.put( "defaultValue", defaultValue );
                        break;
                    case Type.ANALYSIS_EXPRESSION:
                        WorkflowExpression wfExpression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem( node );
                        LinkedHashMap<String, Object> expression = new LinkedHashMap<>();
                        expressions.put( wfExpression.getName(), expression );
                        type = wfExpression.getType().toString();
                        if( !type.equals( "Data element" ) )
                            expression.put( "type", type );
                        String expressionString = wfExpression.getExpression();
                        if( expressionString != null && !expressionString.isEmpty() )
                            expression.put( "expression", expressionString );
                        break;
                    case Type.ANALYSIS_METHOD:
                        Map<String, Object> task = new LinkedHashMap<>();
                        tasks.put( node.getName(), task );
                        AnalysisParameters analysisParameters = WorkflowEngine.getAnalysisParametersByNode( node );
                        if(analysisParameters == null)
                        {
                            log.log(Level.SEVERE,  "Can not read parameters for " + node.getName() );
                            break;
                        }
                        
                        AnalysisParameters defaultParameters = AnalysisDPSUtils.getAnalysisMethodByNode( node.getAttributes() )
                                .getParameters();
                        Map<String, Object> nonDefault = BeanAsMapUtil.getNonDefault( analysisParameters, defaultParameters );
                        Map<String, Object> flatNonDefault = BeanAsMapUtil.flattenMap( nonDefault );
                        Map<String, String> assignments = getAssignments( node, diagram );
                        flatNonDefault.putAll( assignments );

                        Map<String, Object> inputs = new LinkedHashMap<>();
                        Map<String, Object> outputs = new LinkedHashMap<>();
                        for( String input : analysisParameters.getInputNames() )
                        {
                            if( !flatNonDefault.containsKey( input ) )
                                continue;
                            inputs.put( input, flatNonDefault.remove( input ) );
                        }
                        for( String output : analysisParameters.getOutputNames() )
                        {
                            if( !flatNonDefault.containsKey( output ) )
                                continue;
                            outputs.put( output, flatNonDefault.remove( output ) );
                        }
                        
                        flatNonDefault = BeanAsMapUtil.convertToDisplayNames( BeanAsMapUtil.expandMap( flatNonDefault ), analysisParameters );
                        inputs = BeanAsMapUtil.convertToDisplayNames( BeanAsMapUtil.expandMap( inputs ), analysisParameters );
                        outputs = BeanAsMapUtil.convertToDisplayNames( BeanAsMapUtil.expandMap( outputs ), analysisParameters );
                        if( !inputs.isEmpty() )
                        {
                            Object in = inputs;
                            if( analysisParameters.getInputNames().length == 1 )
                                in = inputs.values().iterator().next();
                            task.put( "in", in );
                        }
                        if( !outputs.isEmpty() )
                        {
                            Object out = outputs;
                            if( analysisParameters.getOutputNames().length == 1 )
                                out = outputs.values().iterator().next();
                            task.put( "out", out );
                        }
                        if( !flatNonDefault.isEmpty() )
                            task.put( "param", flatNonDefault );
                        break;
                    case Type.ANALYSIS_CYCLE:
                        Compartment cycleCompartment = (Compartment)node;
                        Map<String, Object> cycleYaml = new LinkedHashMap<>();
                        WorkflowCycleVariable cVar = CycleElement.findCycleVariable( cycleCompartment );
                        String cycleType = cVar.getCycleType().getName();
                        cycleYaml.put( "type", cycleType );
                        if(cVar.isParallel())
                            cycleYaml.put("parallel", true);
                        cycleYaml.put( "expression", cVar.getExpression() );
                        convertCompartment( cycleCompartment, cycleYaml, diagram );
                        cycles.put( cVar.getName(), cycleYaml );
                        break;
                    case Type.ANALYSIS_CYCLE_VARIABLE:
                    case Type.ANALYSIS_SCRIPT:
                    case Type.TYPE_NOTE:
                        break;
                    default:
                        log.warning( "Unhandled node type '" + kernelType + "' when converting workflow to YAML" );
                }
            }
        
        if( !parameters.isEmpty() )
            yaml.put( "parameters", parameters);
        if( !expressions.isEmpty() )
            yaml.put( "expressions", expressions );
        if( !tasks.isEmpty() )
            yaml.put( "tasks", tasks );
        if( !cycles.isEmpty() )
            yaml.put( "cycles", cycles );
    }
    
    private static Map<String, String> getAssignments(Node analysisNode, Diagram diagram)
    {
        Map<String, String> assignments = new HashMap<>();
        Compartment scope = analysisNode.getCompartment();
        List<Edge> edges = analysisNode.recursiveStream().select( Node.class ).flatMap( Node::edges ).toList();
        for( Edge edge : edges )
            if( edge.getKernel().getType().equals( Type.TYPE_DIRECTED_LINK ) )
            {
                String variable = edge.getAttributes().getValueAsString(WorkflowSemanticController.EDGE_VARIABLE);
                if( variable != null )
                {
                    Node varNode = edge.nodes().findFirst( node -> node.getName().equals( variable ) )
                            .orElseThrow( () -> new IllegalArgumentException("Invalid variable reference " + variable) );
                    String reference = getReference( varNode, scope, diagram );
                    String analysisProperty = edge.getAttributes().getValueAsString( WorkflowSemanticController.EDGE_ANALYSIS_PROPERTY );
                    String prev = "";
                    if( assignments.containsKey( analysisProperty ) )
                        prev = assignments.get( analysisProperty ) + ";";
                    assignments.put( analysisProperty, prev + "$" + reference + "$" );
                }
                else
                {
                    String inputAnalysisProperty = edge.getAttributes()
                            .getValueAsString( WorkflowSemanticController.EDGE_ANALYSIS_INPUT_PROPERTY );
                    String outputAnalysisProperty = edge.getAttributes()
                            .getValueAsString( WorkflowSemanticController.EDGE_ANALYSIS_OUTPUT_PROPERTY );
                    Compartment inputAnalysisNode = edge.getInput().getCompartment();
                    Compartment outputAnalysisNode = edge.getOutput().getCompartment();
                    if( inputAnalysisNode == analysisNode )
                        assignments.put( inputAnalysisProperty, "$" + getReference( outputAnalysisNode, scope, diagram ) + "/" + outputAnalysisProperty + "$" );
                    else if( outputAnalysisNode == analysisNode )
                        assignments.put( outputAnalysisProperty, "$" + getReference( inputAnalysisNode, scope, diagram ) + "/" + inputAnalysisProperty + "$" );
                }
            }
        return assignments;
    }
    
    private static String getReference(Node node, Compartment scope, Diagram root)
    {
        do
        {
            if(scope.contains( node.getName() ))
            {
                if(scope.get( node.getName() ) == node)
                    return node.getName();
                break;
            }
            if(scope == root)
                break;
            scope = scope.getCompartment();
        } while(scope != null);
        
        String result = "/" + node.getName();
        for(Compartment c = node.getCompartment(); c != root; c = c.getCompartment())
        {
            if(c == null)
                throw new IllegalArgumentException("Node not in diagram");
            WorkflowCycleVariable cycleVar = CycleElement.findCycleVariable( c );
            if(cycleVar == null)
                throw new IllegalArgumentException("Ancestor of a node is not a cycle");
            result = "/" + cycleVar.getName() + result;
        }
        return result;
    }
}
