package biouml.plugins.research.workflow.yaml;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.ObjectUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.engine.CycleElement;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.items.CycleType;
import biouml.plugins.research.workflow.items.DataElementType;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.workbench.diagram.DiagramEditorHelper;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.gui.Document;
import ru.biosoft.util.BeanAsMapUtil;
import ru.biosoft.util.TextUtil;

public class WorkflowUpdater
{
    private final Diagram diagram;
    private final ViewEditorPane viewEditor;
    
    public WorkflowUpdater(Diagram diagram)
    {
        this.diagram = diagram;
        this.viewEditor = new ViewEditorPane( new DiagramEditorHelper( diagram ) );
    }
    
    public WorkflowUpdater(Diagram diagram, Document documnet)
    {
        this.diagram = diagram;
        this.viewEditor = (ViewEditorPane)documnet.getViewPane();
    }
    
    public void updateWorkflow(String yamlText)
    {
        Map<String, Object> newYamlModel = new YamlParser().parseYaml( yamlText );
        if( newYamlModel == null )
            return;
        WorkflowToYamlConverter converter = new WorkflowToYamlConverter();
        Map<String, Object> oldYamlModel = converter.convert( diagram );
        //TODO:update name
        updateDescription( newYamlModel, oldYamlModel );
        updateCompartment( newYamlModel, oldYamlModel, diagram );
        updateDependencies( newYamlModel, oldYamlModel );
    }
    
    public void updateCompartment(Map<String, Object> newYamlModel, Map<String, Object> oldYamlModel, Compartment parent)
    {
        updateParameters( newYamlModel, oldYamlModel, parent );
        updateExpressions( newYamlModel, oldYamlModel, parent );
        updateTasks( newYamlModel, oldYamlModel, parent );
        updateCycles( newYamlModel, oldYamlModel, parent);
    }
    
    private void updateCycles(Map<String, Object> newYamlModel, Map<String, Object> oldYamlModel, Compartment parent)
    {
        Map<String, Object> newCycles = getChild( newYamlModel, "cycles" );
        Map<String, Object> oldCycles = getChild( oldYamlModel, "cycles" );
        for(String cycleName : newCycles.keySet())
            if(oldCycles.containsKey( cycleName ))
            {
                Compartment cycleCompartment = findCycle( cycleName, parent );
                updateCycle(getChild( newCycles, cycleName ), getChild( oldCycles, cycleName ), cycleCompartment);
            }
            else
                addCycle(cycleName, getChild( newCycles, cycleName ), parent);
        for( String cycleName : oldCycles.keySet())
            if(!newCycles.containsKey( cycleName ))
                removeDiagramElement( findCycle( cycleName, parent ) );
    }
    
    private void updateCycle(Map<String, Object> newYamlModel, Map<String, Object> oldYamlModel, Compartment cycleCompartment)
    {
        WorkflowCycleVariable cycleVariable = CycleElement.findCycleVariable( cycleCompartment );
        String cycleType = (String)newYamlModel.get( "type" );
        if(!Objects.equals( cycleType, oldYamlModel.get( "type" ) ))
            cycleVariable.setCycleType( WorkflowCycleVariable.getCycleTypeByName( cycleType ) );
        String expression = (String)newYamlModel.get( "expression" );
        if(!Objects.equals( expression, oldYamlModel.get( "expression" ) ))
            cycleVariable.setExpression( expression );
        updateCompartment( newYamlModel, oldYamlModel, cycleCompartment );
    }

    private Compartment findCycle(String name, Compartment parent)
    {
        return parent.stream( Compartment.class )
            .filter( c->c.getKernel() != null && Type.ANALYSIS_CYCLE.equals( c.getKernel().getType() )  )
            .filter( c->name.equals( CycleElement.findCycleVariable( c ).getName() ) ).findAny().orElse( null );
    }

    private void addCycle(String cycleName, Map<String, Object> yamlModel, Compartment parent)
    {
        WorkflowSemanticController semanticController = (WorkflowSemanticController)diagram.getType().getSemanticController();
        Compartment cycleCompartment = semanticController.createCycleNode( parent );
        
        WorkflowCycleVariable cycleVariable = (WorkflowCycleVariable)WorkflowItemFactory.getWorkflowItem( cycleCompartment, Type.ANALYSIS_CYCLE_VARIABLE );
        cycleVariable.setName( cycleName );
        String cycleTypeStr = (String)yamlModel.get( "type" );
        CycleType cycleType = WorkflowCycleVariable.getCycleTypeByName( cycleTypeStr );
        cycleVariable.setCycleType( cycleType );
        cycleVariable.setExpression( (String)yamlModel.get( "expression" ) );
        cycleVariable.setType( VariableType.getType( Double.class ) );//
        
        cycleCompartment.put( cycleVariable.getNode() );
        updateCompartment( yamlModel, Collections.emptyMap(), cycleCompartment );
        addNode( cycleCompartment, parent );
    }

    private ViewEditorPane getViewEditor()
    {
        return viewEditor;
    }

    private static Map<String, Object> getChild(Map<String, ?> parent, String name)
    {
        @SuppressWarnings ( "unchecked" )
        Map<String, Object> map = (Map<String, Object>)parent.get( name );
        return map == null ? Collections.<String, Object>emptyMap() : map;
    }
    
    @SuppressWarnings ( "unchecked" )
    private static Map<String, Object> getChild(Map<String, ?> parent, String name, String[] paramNames)
    {
        Object val = parent.get( name );
        if( val == null )
            return Collections.emptyMap();
        if( val instanceof Map )
            return (Map<String, Object>)val;
        if(val instanceof List)
        {
            Map<String, Object> result = new LinkedHashMap<>();
            List<?> positionalParameters = (List<?>)val;
            for(int i = 0; i < Math.min( positionalParameters.size(), paramNames.length ); i++)
                result.put( paramNames[i], positionalParameters.get( i ) );
            return result;
        }
        if(paramNames.length == 0)
            return Collections.emptyMap();
        return Collections.singletonMap( paramNames[0], val );
    }
    
    private void updateDescription(Map<String, Object> newYamlModel, Map<String, Object> oldYamlModel)
    {
        if( !ObjectUtils.equals( oldYamlModel.get( "description" ), newYamlModel.get( "description" ) ) )
            diagram.setDescription( (String)newYamlModel.get( "description" ) );
    }

    private void updateParameters(Map<String, Object> newYamlModel, Map<String, Object> oldYamlModel, Compartment parent)
    {
        Map<String, Object> oldParameters = getChild(oldYamlModel, "parameters" );
        Map<String, Object> newParameters = getChild(newYamlModel, "parameters" );
        for( Map.Entry<String, Object> e : newParameters.entrySet() )
            if( !oldParameters.containsKey( e.getKey() ) )
            {
                WorkflowParameter parameter = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem( parent, Type.ANALYSIS_PARAMETER );
                parameter.setName( e.getKey() );

                Map<String, String> properties = (Map<String, String>)e.getValue();

                String type = properties.get( "type" );
                parameter.setType( VariableType.getType( type ) );

                String description = properties.get( "description" );
                if( description != null )
                    parameter.setDescription( description );

                String defaultValue = properties.get( "defaultValue" );
                if( defaultValue != null )
                    parameter.setDefaultValueString( defaultValue );

                String role = properties.get( "role" );
                if( role != null )
                    parameter.setRole( role );

                String deTypeString = properties.get( "elementType" );
                if( deTypeString != null )
                    parameter.setDataElementType( DataElementType.getType( deTypeString ) );

                String dropDownOptions = properties.get( "dropDownOptions" );
                if( dropDownOptions != null )
                    parameter.setDropDownOptions( dropDownOptions );

                addNode( parameter.getNode(), parent );
            }
            else
            {
                Node node = (Node)parent.get( e.getKey() );
                WorkflowParameter parameter = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem( node, getViewEditor() );
                Map<String, String> oldProperties = (Map<String, String>)oldParameters.get( e.getKey() );
                Map<String, String> newProperties = (Map<String, String>)e.getValue();
                
                String oldType = oldProperties.get( "type" );
                if( oldType == null )
                    oldType = "Data element";
                String newType = newProperties.get( "type" );
                if( newType == null )
                    newType = "Data element";
                if( !oldType.equals( newType ) )
                    parameter.setType( VariableType.getType( newProperties.get( "type" ) ) );
                
                if( !Objects.equals( oldProperties.get( "description" ), newProperties.get( "description" ) ) )
                    parameter.setDescription( newProperties.get( "description" ) );
                if( !Objects.equals( oldProperties.get( "defaultValue" ), newProperties.get( "defaultValue" ) ) )
                    parameter.setDefaultValueString( newProperties.get( "defaultValue" ) );
                
                String oldRole = oldProperties.get( "role" );
                if(oldRole == null)
                    oldRole = WorkflowParameter.ROLE_INPUT;
                String newRole = newProperties.get( "role" );
                if(newRole == null)
                    newRole = WorkflowParameter.ROLE_INPUT;
                if( !oldRole.equals( newRole ) )
                    parameter.setRole( newRole );
                
                String oldEType = oldProperties.get( "elementType" );
                if( oldEType == null )
                    oldEType = "(any)";
                String newEType = newProperties.get( "elementType" );
                if( newEType == null )
                    newEType = "(any)";
                if( !Objects.equals( oldEType, newEType ) )
                {
                    DataElementType newETypeObj = DataElementType.getTypeOrNull( newEType );
                    if(newETypeObj != null)
                        parameter.setDataElementType( newETypeObj );
                }
                if( !Objects.equals( oldProperties.get( "dropDownOptions" ), newProperties.get( "dropDownOptions" ) ) )
                    parameter.setDropDownOptions( newProperties.get( "dropDownOptions" ) );
            }
        for( String key : oldParameters.keySet() )
            if( !newParameters.containsKey( key ) )
            {
                removeDiagramElement( key, parent );
            }
    }
    
    private void updateExpressions(Map<String, Object> newYamlModel, Map<String, Object> oldYamlModel, Compartment parent)
    {
        Map<String, Object> oldExpressions = getChild(oldYamlModel, "expressions" );
        Map<String, Object> newExpressions = getChild(newYamlModel, "expressions" );
        for( Map.Entry<String, Object> e : newExpressions.entrySet() )
            if( !oldExpressions.containsKey( e.getKey() ) )
            {
                WorkflowExpression expression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem( parent, Type.ANALYSIS_EXPRESSION );
                expression.setName( e.getKey() );
                Map<String, String> properties = (Map<String, String>)e.getValue();
                expression.setType( VariableType.getType( properties.get( "type" ) ) );
                String expressionString = properties.get( "expression" );
                if( expressionString != null )
                    expression.setExpression( expressionString );
                addNode( expression.getNode(), parent );
            }
            else
            {
                Node node = (Node)parent.get( e.getKey() );
                WorkflowExpression expression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem( node, getViewEditor() );
                Map<String, String> oldProperties = (Map<String, String>)oldExpressions.get( e.getKey() );
                Map<String, String> newProperties = (Map<String, String>)e.getValue();
                String oldType = oldProperties.get( "type" );
                if( oldType == null )
                    oldType = "Data element";
                String newType = newProperties.get( "type" );
                if( newType == null )
                    newType = "Data element";
                if( !oldType.equals( newType ) )
                    expression.setType( VariableType.getType( newType ) );
                if( !Objects.equals( oldProperties.get( "expression" ), newProperties.get( "expression" ) ) )
                    expression.setExpression( newProperties.get( "expression" ) );
            }
        StreamEx.ofKeys( oldExpressions ).remove( newExpressions::containsKey ).forEach( k->removeDiagramElement(k,parent) );
    }

    private void updateDependencies(Map<String, Object> newYamlModel, Map<String, Object> oldYamlModel)
    {
        // TODO Auto-generated method stub

    }

    private void updateTasks(Map<String, Object> newYamlModel, Map<String, Object> oldYamlModel, Compartment parent)
    {
        Map<String, Object> oldTasks = getChild(oldYamlModel, "tasks" );
        Map<String, Object> newTasks = getChild(newYamlModel, "tasks" );
        for( Map.Entry<String, Object> entry : newTasks.entrySet() )
            if( !oldTasks.containsKey( entry.getKey() ) )
            {
                String callName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> analysisValue = (Map<String, Map<String, Object>>) entry.getValue();
                addAnalysis( callName, analysisValue, parent );
            }
        for( Map.Entry<String, Object> entry : newTasks.entrySet() )
            if(!oldTasks.containsKey( entry.getKey() ) || !entry.getValue().equals( oldTasks.get( entry.getKey() )))
                updateAnalysis( entry.getKey(), (Map<String, Map<String, Object>>)entry.getValue(), parent );
        for( Map.Entry<String, Object> entry : oldTasks.entrySet() )
            if( !newTasks.containsKey( entry.getKey() ) )
            {
                String analysisName = entry.getKey();
                removeDiagramElement( analysisName, parent );
            }
    }

    private void updateAnalysis(String callName, Map<String, Map<String, Object>> analysisValue, Compartment parent)
    {
        Compartment analysisNode = (Compartment)parent.get( callName );
        updateAnalysis( analysisNode, analysisValue );
    }

    private void updateAnalysis(Compartment analysisNode, Map<String, Map<String, Object>> analysisValue)
    {
        AnalysisParameters analysisParameters = WorkflowEngine.getAnalysisParametersByNode( analysisNode );
        if(analysisParameters == null)
        {
            return;
        }
        Map<String, Object> parametersMap = getChild(analysisValue, "param" );
        BeanAsMapUtil.readBeanFromHierarchicalMap( BeanAsMapUtil.convertFromDisplayNames( parametersMap, analysisParameters, true ), analysisParameters );
        AnalysisDPSUtils.writeParametersToNodeAttributes( null, analysisParameters, analysisNode.getAttributes() );
        WorkflowSemanticController semanticController = (WorkflowSemanticController)diagram.getType().getSemanticController();
        semanticController.updateAnalysisNode( analysisNode );
        
        Map<String, Object> inputMap = getChild( analysisValue, "in", analysisParameters.getInputNames() );
        updateAnalysisEdges( analysisNode, analysisParameters, inputMap, true );
        
        Map<String, Object> outputMap = getChild( analysisValue, "out", analysisParameters.getOutputNames() );
        updateAnalysisEdges( analysisNode, analysisParameters, outputMap, false );
    }

    private void removeDiagramElement(String name, Compartment parent)
    {
        DiagramElement node = parent.get( name );
        removeDiagramElement(node);
    }
    
    private void removeDiagramElement(DiagramElement element)
    {
        getViewEditor().getSelectionManager().selectModel( element, true );
        getViewEditor().remove();
    }
    
    private Compartment createAnalysisNode(String callName, Compartment parent)
    {
        String analysisName = callName.replaceFirst( "\\(.*\\)$", "" ).trim();
        WorkflowSemanticController semanticController = (WorkflowSemanticController)diagram.getType().getSemanticController();
        Compartment node;
        try
        {
            DynamicPropertySet attributes = new DynamicPropertySetAsMap();
            attributes.add(new DynamicProperty(AnalysisDPSUtils.PARAMETER_ANALYSIS_FULLNAME, String.class, analysisName));
            node = semanticController.createAnalysisNode(parent, analysisName, attributes);
            return node.clone( parent, callName );
        }
        catch( ParameterNotAcceptableException ex )
        {
            Stub kernel = new Stub(null, "Unknown analysis ("+analysisName+")", Type.ANALYSIS_METHOD);
            Compartment anNode = new Compartment(parent, callName, kernel);
            anNode.setShapeSize( new Dimension( 200, 50 ) );
            return anNode;
        }
    }

    private void addAnalysis(String callName, Map<String, Map<String, Object>> analysisValue, Compartment parent)
    {
        if(parent.contains( callName ))
            return;
        
        Compartment analysisNode = createAnalysisNode( callName, parent );
        updateAnalysis( analysisNode, analysisValue );
        
        addNode( analysisNode, parent );
    }

    private void addNode(Node node, Compartment parent)
    {
        Rectangle viewPortBounds = getViewEditor().getViewportBounds();
        Point to = new Point((int)viewPortBounds.getCenterX(), (int)viewPortBounds.getCenterY());
        
        View[] parentView = getViewEditor().getView( parent );
        if(parentView != null && parentView.length > 0)
        {
            Rectangle parentBounds = parentView[0].getBounds();
            if(parentBounds.intersects( viewPortBounds ))
                parentBounds = parentBounds.intersection( parentBounds );
            to = new Point((int)parentBounds.getCenterX(), (int)parentBounds.getCenterY());
        }
        getViewEditor().add( node, to );
    }
    
    private Node resolveReference(String reference, Compartment scope)
    {
        if( reference.isEmpty() )
            return null;
        if( reference.charAt( 0 ) == '/' )
        {
            scope = diagram;
            reference = reference.substring( 1 );
            if( reference.isEmpty() )
                return null;
        }
        String[] path = TextUtil.split( reference, '/' );
        int i = 0;
        while( i < path.length )
        {
            String name = path[i];
            Optional<Compartment> next = scope.stream( Compartment.class )
                    .filter( c -> c.getKernel() != null && Type.ANALYSIS_CYCLE.equals( c.getKernel().getType() ) )
                    .filter( c -> CycleElement.findCycleVariable( c ).getName().equals( name ) ).findAny();
            if( !next.isPresent() )
                break;
            scope = next.get();
            i++;
        }
        if( i == path.length - 1 )
        {
            DiagramElement res = scope.get( path[i] );
            if( ! ( res instanceof Node ) )
                return null;
            return (Node)res;
        }
        if( i <= path.length - 2 )
        {
            DiagramElement analysisElement = scope.get( path[i] );
            if( ! ( analysisElement instanceof Compartment ) )
                return null;
            Compartment analysis = (Compartment)analysisElement;
            StringJoiner joiner = new StringJoiner( ":" );
            for( int j = i + 1; j < path.length; j++ )
                joiner.add( path[j] );
            String portName = joiner.toString();
            DiagramElement port = analysis.get( portName );
            if( ! ( port instanceof Node ) )
                return null;
            return (Node)port;
        }
        return null;
    }
    
    private void updateAnalysisEdges(Compartment analysisNode, AnalysisParameters analysisParameters, Map<String, Object> parametersMap, boolean isInput)
    {
        parametersMap = BeanAsMapUtil.convertFromDisplayNames( parametersMap, analysisParameters, false );
        parametersMap = BeanAsMapUtil.flattenMap( parametersMap );
        Set<Edge> requiredEdges = Collections.newSetFromMap( new IdentityHashMap<Edge, Boolean>() );
        for( Map.Entry<String, Object> paramEntry : parametersMap.entrySet() )
        {
            if( paramEntry.getValue() == null )
                continue;
            String paramStringValue = paramEntry.getValue().toString();
            if( paramStringValue.length() > 2 && paramStringValue.charAt( 0 ) == '$'
                    && paramStringValue.charAt( paramStringValue.length() - 1 ) == '$' )
            {
                for( String reference : paramStringValue.substring( 1, paramStringValue.length() - 1 ).split( "[$] *; *[$]" ) )
                {
                    Node variableNode = resolveReference( reference, analysisNode.getCompartment() );
                    if( variableNode == null )
                        continue;
                    String portName = paramEntry.getKey().replace( '/', ':' );
                    DiagramElement parameterPort = analysisNode.get( portName );
                    if( ! ( parameterPort instanceof Node ) )
                        continue;

                    boolean edgeExists = false;
                    for( Edge existingEdge : variableNode.getEdges() )
                    {
                        Node existingPort = isInput ? existingEdge.getOutput() : existingEdge.getInput();
                        if( existingPort == parameterPort )
                        {
                            edgeExists = true;
                            requiredEdges.add( existingEdge );
                        }
                    }
                    if( edgeExists )
                        continue;

                    WorkflowSemanticController semanticController = (WorkflowSemanticController)diagram.getType().getSemanticController();
                    Stub kernel = new Stub( null, WorkflowSemanticController.generateUniqueNodeName( diagram, "edge" ),
                            Base.TYPE_DIRECTED_LINK );
                    Edge edge = isInput ? new Edge( kernel, variableNode, (Node)parameterPort )
                            : new Edge( kernel, (Node)parameterPort, variableNode );
                    semanticController.annotateEdge( edge );
                    getViewEditor().add( edge, new Point( 10, 10 ) );
                    requiredEdges.add( edge );
                }
            }
        }
        for( Node port : analysisNode.getNodes() )
        {
            String kernelType = port.getKernel().getType();
            if( ( isInput && kernelType.equals( Type.TYPE_DATA_ELEMENT_IN ) )
                    || ( !isInput && kernelType.equals( Type.TYPE_DATA_ELEMENT_OUT ) ) )
                for( Edge edge : port.getEdges() )
                    if( !requiredEdges.contains( edge ) )
                    {
                        removeDiagramElement( edge );
                    }
        }
    }
}
