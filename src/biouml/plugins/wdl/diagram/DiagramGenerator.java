package biouml.plugins.wdl.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.model.CallInfo;
import biouml.plugins.wdl.model.ConditionalInfo;
import biouml.plugins.wdl.model.ExpressionInfo;
import biouml.plugins.wdl.model.ImportInfo;
import biouml.plugins.wdl.model.InputInfo;
import biouml.plugins.wdl.model.OutputInfo;
import biouml.plugins.wdl.model.ScatterInfo;
import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.model.StructInfo;
import biouml.plugins.wdl.model.TaskInfo;
import biouml.plugins.wdl.model.WorkflowInfo;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPutException;

/**
 * Generates WDL diagram on the base of ScriptInfo object
 */
public class DiagramGenerator
{
    protected static final Logger log = Logger.getLogger( WDLImporter.class.getName() );

    //    private boolean doImportDiagram = false;
    private Map<String, Diagram> imports = new HashMap<>();
    private Map<String, Compartment> importedTasks = new HashMap<>();
    private Map<String, Compartment> tasks = new HashMap<>();
    private int externalPosition = 0;

    public Diagram generateDiagram(ScriptInfo script, DataCollection dc, String name) throws Exception
    {
        Diagram result = new WDLDiagramType().createDiagram( dc, name );
        return generateDiagram( script, result );
    }

    public Diagram generateDiagram(ScriptInfo script, Diagram diagram) throws Exception
    {
        diagram.clear();
        diagram.getAttributes().remove( WDLConstants.IMPORTS_ATTR );
        diagram.getAttributes().remove( WDLConstants.SETTINGS_ATTR );
        diagram.getAttributes().remove( WDLConstants.VERSION_ATTR );
        diagram.getAttributes().remove( WDLConstants.META_ATTR );
        diagram.getAttributes().remove( WDLConstants.PARAMETER_META_ATTR );

        externalPosition = 0;
        imports.clear();
        this.tasks.clear();

        for( ImportInfo importInfo : script.getImports() )
        {
            createImport( diagram, importInfo );
        }

        for( StructInfo structInfo : script.getStructs() )
        {
            createStruct( diagram, structInfo );
        }

        for( String taskName : script.getTaskNames() )
        {
            createTaskNode( diagram, script.getTask( taskName ) );
        }

        for( InputInfo input : script.getInputs() )
        {
            createExternalParameterNode( diagram, input );
        }

        for( String workflowName : script.getWorkflowNames() )
        {
            WorkflowInfo workflow = script.getWorkflow( workflowName );//TODO" create compartment?
            WorkflowUtil.setMeta( diagram, workflow.getMeta() );

            for( InputInfo input : workflow.getInputs() )
                createExternalParameterNode( diagram, input );

            for( OutputInfo output : workflow.getOutputs() )
                createOutputNode( diagram, output );

            for( Object object : workflow.getObjects() )
            {
                if( object instanceof ExpressionInfo )
                {
                    createExpressionNode( diagram, (ExpressionInfo)object );
                }
                else if( object instanceof CallInfo )
                {
                    createCallNode( diagram, (CallInfo)object );
                }
                else if( object instanceof ScatterInfo )
                {
                    createScatterNode( diagram, (ScatterInfo)object );
                }
                else if( object instanceof ConditionalInfo )
                {
                    createConditionalNode( diagram, (ConditionalInfo)object );
                }
            }
        }
        createLinks( diagram );
        addOutputs( diagram );
        //        setInputs(diagram);
        splitInputs( diagram );
        return diagram;
    }

    public void setInputs(Diagram diagram) throws Exception
    {
        List<Node> nodes = diagram.stream( Node.class ).filter( n -> WorkflowUtil.isExpression( n ) ).toList();
        for( Node node : nodes )
        {
            if( node.edges().filter( e -> e.getOutput().equals( node ) ).count() == 0 )
            {
                String name = node.getName();
                Node newNode = new Node( diagram, name, new Stub( null, name, WDLConstants.WORKFLOW_INPUT_TYPE ) );
                WorkflowUtil.setName( newNode, WorkflowUtil.getName( node ) );
                WorkflowUtil.setType( newNode, WorkflowUtil.getType( node ) );
                WorkflowUtil.setExpression( newNode, WorkflowUtil.getExpression( newNode ) );
                newNode.setTitle( name );
                newNode.setShapeSize( new Dimension( 80, 60 ) );

                diagram.remove( node.getName() );
                diagram.put( newNode );

                for( Edge edge : node.getEdges() )
                {
                    edge.setInput( newNode );
                    newNode.addEdge( edge );
                    node.removeEdge( edge );
                }

            }
        }
    }

    public void splitInputs(Diagram diagram)
    {
        List<Node> nodes = diagram.stream( Node.class ).filter( n -> WorkflowUtil.isExternalParameter( n ) && n.getEdges().length > 3 )
                .toList();
        for( Node node : nodes )
        {
            Node newNode = node;
            System.out.println( "Clone " + newNode.getName() );
            Edge[] edges = node.getEdges();
            int length = edges.length;
            int j = 0;
            for( int i = 0; i < length; i++ )
            {
                if( j > 2 )
                {
                    j = 0;
                    newNode = cloneInput( node );
                    System.out.println( "Clone " + newNode.getName() );
                }
                j++;
                Edge edge = edges[i];
                edge.setInput( newNode );
                System.out.println( "Set edge " + newNode.getName() );
                newNode.addEdge( edge );
                node.removeEdge( edge );
            }
        }
    }

    private static Node cloneInput(Node node)
    {
        Diagram diagram = Diagram.getDiagram( node );
        String name = DefaultSemanticController.generateUniqueName( diagram, node.getName() );
        Stub kernel = new Stub( null, name, node.getKernel().getType() );
        Node newNode = new Node( diagram, name, kernel );
        newNode.setTitle( node.getTitle() );
        WorkflowUtil.setName( newNode, WorkflowUtil.getName( node ) );
        WorkflowUtil.setType( newNode, WorkflowUtil.getType( node ) );
        WorkflowUtil.setExpression( newNode, WorkflowUtil.getExpression( newNode ) );
        newNode.setTitle( node.getTitle() );
        newNode.setShapeSize( new Dimension( 80, 60 ) );
        diagram.put( newNode );
        return newNode;
    }

    public void addOutputs(Diagram diagram)
    {
        for( Node node : diagram.recursiveStream().select( Node.class )
                .filter( n -> ( n.getEdges().length == 0 && WorkflowUtil.isOutput( n ) && WorkflowUtil.isCall( n.getCompartment() ) ) ) )
        {

            String expression = WorkflowUtil.getName( node );
            ExpressionInfo expressionInfo = new ExpressionInfo();
            if( expression == null )
            {
                System.out.println( "Error" );
            }
            expressionInfo.setExpression( expression );
            expressionInfo.setName( expression );
            expressionInfo.setType( null );
            Node output = createOutputNode( diagram, expressionInfo );
            createLink( node, output );
        }
    }


    public Node createStruct(Compartment parent, StructInfo structInfo) throws Exception
    {
        String name = structInfo.getName();
        Stub kernel = new Stub( null, name, WDLConstants.STRUCT_TYPE );
        Node node = new Node( parent, name, kernel );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 50, 50 ) );
        parent.put( node );

        Iterable<ExpressionInfo> expressions = structInfo.getExpressions();
        ExpressionInfo[] declarations = StreamEx.of( expressions ).toArray( ExpressionInfo[]::new );
        WorkflowUtil.setStructMembers( node, declarations );
        return node;
    }

    public void createImport(Diagram diagram, ImportInfo importInfo) throws Exception
    {
        Diagram imported = new WDLDiagramType().createDiagram( null, importInfo.getImported().getName() );
        Diagram importedDiagram = new DiagramGenerator().generateDiagram( importInfo.getImported(), imported );
        this.imports.put( importInfo.getImported().getName(), importedDiagram );

        if( importInfo.getTask() != null )
        {
            DiagramElement de = importedDiagram.findDiagramElement( importInfo.getTask() );
            if( de instanceof Compartment && WorkflowUtil.isTask( (Compartment)de ) )
            {
                importedTasks.put( de.getName(), (Compartment)de );
            }
        }
        //        try
        //        {
        ////            DataElementPath dep = diagram.getOrigin().getCompletePath();
        ////            String path = importInfo.getSource();
        ////            String[] parts = path.split( "/" );
        ////            DataElementPath importPath = dep.getChildPath( parts );
        ////            Diagram imported = importPath.getDataElement( Diagram.class );
        //            Diagram imported = new WDLDiagramType().createDiagram( null, importInfo.getAlias() );
        //            Diagram importedDiagram = new DiagramGenerator().generateDiagram( importInfo.getImported(), imported );
        //            imports.put( importInfo.getAlias(), imported );
        //            if( imported == null )
        //                throw new Exception( "Imported diagram " + importInfo.getSource() + " not found!" );
        //            WorkflowUtil.addImport( diagram, imported, importInfo.getAlias() );
        //        }
        //        catch( Exception ex )
        //        {
        //            log.info( "Can not process import " + importInfo.toString() + ": " + ex.getMessage() );
        //        }
    }

    public Node createExternalParameterNode(Compartment parent, ExpressionInfo expressionInfo)
    {
        String name = expressionInfo.getName();
        String title = name;
        if( name.startsWith( "params." ) )
        {
            title = name.substring( name.indexOf( "." ) + 1 );
            name = name.replace( ".", "__" );
        }
        Stub kernel = new Stub( null, name, WDLConstants.WORKFLOW_INPUT_TYPE );
        Node node = new Node( parent, name, kernel );
        WorkflowUtil.setPosition( node, externalPosition++ );
        setDeclaration( node, expressionInfo );
        node.setTitle( title );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public void createLinks(Diagram diagram)
    {
        for( Node node : diagram.recursiveStream().select( Node.class ).without( diagram ) )
        {
            if( WorkflowUtil.isTask( node.getCompartment() ) || WorkflowUtil.isOutput( node ) )
                continue;

            String expression = WorkflowUtil.getExpression( node );
            if( expression == null )
                continue;

            String[] arguments = WorkflowUtil.getArguments( node );
            if( arguments != null )
            {
                for( String arg : arguments )
                {
                    List<Node> sources = WorkflowUtil.findSources( arg, diagram );
                    for( Node source : sources )
                        createLink( source, node );
                }
            }
            else
            {
                List<String> args = WorkflowUtil.findPossibleArguments( expression );
                for( String arg : args )
                {
                    List<Node> sources = WorkflowUtil.findSources( arg, diagram );
                    for( Node source : sources )
                        createLink( source, node );
                    //                Compartment call = WorkflowUtil.findCall( arg, diagram );
                    //                if( call != null )
                    //                {
                    //                    Node source = call.stream( Node.class ).filter( n -> WorkflowUtil.isOutput( n ) ).findAny().orElse( null );
                    //                    if( source != null )
                    //                        createLink( source, node );
                    //                }
                    //                else
                    //                {
                    //                    Node source = WorkflowUtil.findExpressionNode( diagram, arg );
                    //                    if( source != null )
                    //                        createLink( source, node );
                }
            }
        }
    }

    public Node createConditionNode(Compartment parent, String expression)
    {
        String name = WDLSemanticController.uniqName( parent, "condition" );
        Stub kernel = new Stub( null, name, WDLConstants.CONDITION_TYPE );
        Node node = new Node( parent, name, kernel );
        WorkflowUtil.setExpression( node, expression );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public Node createExpressionNode(Compartment parent, ExpressionInfo expressionInfo)
    {
        String name = expressionInfo.getName();
        boolean noName = name == "";
        if( noName )
        {
            name = DefaultSemanticController.generateUniqueName( parent, "expression" );
        }
        Stub kernel = new Stub( null, name, WDLConstants.EXPRESSION_TYPE );
        Node node = new Node( parent, name, kernel );
        setDeclaration( node, expressionInfo );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }


    public Node createOutputNode(Compartment parent, ExpressionInfo expressionInfo)
    {
        String name = expressionInfo.getName();
        if( name == null )
            name = DefaultSemanticController.generateUniqueName( parent, "output" );
        Stub kernel = new Stub( null, name, WDLConstants.WORKFLOW_OUTPUT_TYPE );
        Node node = new Node( parent, name, kernel );
        setDeclaration( node, expressionInfo );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public Compartment createTaskNode(Compartment parent, TaskInfo task)
    {
        String name = task.getName();
        name = WDLSemanticController.uniqName( parent, name );
        Stub kernel = new Stub( null, name, WDLConstants.TASK_TYPE );

        Compartment c = new Compartment( parent, name, kernel );
        WorkflowUtil.setBeforeCommand( c, task.getBeforeCommand().toArray( ExpressionInfo[]::new ) );
        WorkflowUtil.setCommand( c, task.getCommand().getScript() );
        WorkflowUtil.setRuntime( c, task.getRuntime() );
        c.setTitle( name );
        c.setNotificationEnabled( false );
        c.setShapeSize( new Dimension( 200, 0 ) );
        tasks.put( name, c );
        int maxPorts = 0;
        int i = 0;
        for( ExpressionInfo expression : task.getInputs() )
        {
            Node portNode = addPort( WDLSemanticController.uniqName( parent, "input" ), WDLConstants.INPUT_TYPE, i++, c );
            setDeclaration( portNode, expression );
            //            }
            maxPorts = task.getInputs().size();
        }

        i = 0;
        for( ExpressionInfo expression : task.getOutputs() )
        {
            Node portNode = addPort( WDLSemanticController.uniqName( parent, "output" ), WDLConstants.OUTPUT_TYPE, i++, c );
            setDeclaration( portNode, expression );
        }
        maxPorts = Math.max( maxPorts, task.getOutputs().size() );
        int height = Math.max( 50, 24 * maxPorts + 8 );
        c.setShapeSize( new Dimension( 200, height ) );
        c.getAttributes().add( new DynamicProperty( "innerNodesPortFinder", Boolean.class, true ) );
        c.setNotificationEnabled( true );
        parent.put( c );
        return c;
    }

    public void createConditionalNode(Compartment parent, ConditionalInfo conditional) throws Exception
    {
        Node previousNode = null;
        for( String condition : conditional.getConditions() )
        {
            Node conditionNode = createConditionNode( parent, condition );
            String name = WDLSemanticController.uniqName( parent, "block" );
            Compartment c = new Compartment( parent, name, new Stub( null, name, WDLConstants.CONDITIONAL_TYPE ) );
            c.setShapeSize( new Dimension( 700, 700 ) );
            fillCompartment( conditional.get( condition ), c );

            if( previousNode != null )
                createLink( previousNode, conditionNode );

            parent.put( c );
            createLink( conditionNode, c );

            previousNode = conditionNode;
        }

        if( conditional.hasElse() )
        {
            Node conditionNode = createConditionNode( parent, "else" );
            String name = WDLSemanticController.uniqName( parent, "block" );
            Compartment c = new Compartment( parent, name, new Stub( null, name, WDLConstants.CONDITIONAL_TYPE ) );
            c.setShapeSize( new Dimension( 700, 700 ) );
            fillCompartment( conditional.getElse(), c );

            if( previousNode != null )
                createLink( previousNode, conditionNode );
            parent.put( c );
            createLink( conditionNode, c );
        }
    }

    private void fillCompartment(Iterable<Object> objects, Compartment c) throws Exception
    {
        for( Object obj : objects )
        {
            if( obj instanceof ExpressionInfo )
            {
                createExpressionNode( c, (ExpressionInfo)obj );
            }
            else if( obj instanceof CallInfo )
            {
                createCallNode( c, ( (CallInfo)obj ) );
            }
            else if( obj instanceof ScatterInfo )
            {
                createScatterNode( c, (ScatterInfo)obj );
            }
            else if( obj instanceof ConditionalInfo )
            {
                createConditionalNode( c, (ConditionalInfo)obj );
            }
        }
    }

    public Compartment createScatterNode(Compartment parent, ScatterInfo scatter) throws Exception
    {
        String name = "scatter";
        name = WDLSemanticController.uniqName( parent, name );
        Stub kernel = new Stub( null, name, WDLConstants.SCATTER_TYPE );
        Compartment c = new Compartment( parent, name, kernel );
        c.setShapeSize( new Dimension( 500, 300 ) );
        String variable = scatter.getVariable();
        String array = scatter.getExpression();
        Node arrayNode = Diagram.getDiagram( parent ).findNode( array.toString() );

        if( arrayNode == null )
            arrayNode = createExpression( array, "Array[Int]", parent );
        name = WDLSemanticController.uniqName( parent, variable );
        Node variableNode = new Node( c, name, new Stub( null, name, WDLConstants.SCATTER_VARIABLE_TYPE ) );
        WorkflowUtil.setName( variableNode, variable );
        c.put( variableNode );
        createLink( arrayNode, variableNode );
        parent.put( c );
        for( Object obj : scatter.getObjects() )
        {
            if( obj instanceof ExpressionInfo )
            {
                createExpressionNode( c, (ExpressionInfo)obj );
            }
            else if( obj instanceof CallInfo )
            {
                createCallNode( c, (CallInfo)obj );
            }
            else if( obj instanceof ScatterInfo )
            {
                createScatterNode( c, (ScatterInfo)obj );
            }
            else if( obj instanceof ConditionalInfo )
            {
                createConditionalNode( c, (ConditionalInfo)obj );
            }
        }
        return c;
    }

    private Node createExpression(String expression, String type, Compartment parent)
    {
        String name = DefaultSemanticController.generateUniqueName( parent, "expression" );
        Node resultNode = new Node( parent, name, new Stub( null, name, WDLConstants.EXPRESSION_TYPE ) );
        WorkflowUtil.setExpression( resultNode, expression );
        WorkflowUtil.setName( resultNode, name );
        WorkflowUtil.setType( resultNode, type );
        parent.put( resultNode );
        return resultNode;
    }

    private static void setDeclaration(Node node, ExpressionInfo declaration)
    {
        WorkflowUtil.setName( node, declaration.getName() );
        WorkflowUtil.setType( node, declaration.getType() );
        WorkflowUtil.setExpression( node, declaration.getExpression() );
        WorkflowUtil.setArguments( node, declaration.getArguments() );
    }

    public Compartment createCallNode(Compartment parent, CallInfo call) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( parent );
        String name = call.getTaskName();
        Compartment taskСompartment = tasks.get( name );
        if( taskСompartment == null )
            taskСompartment = this.importedTasks.get( name );
        String taskRef = name;
        String diagramRef = null;
        String diagramAlias = null;
        //        boolean externalDiagram = false;
        if( taskСompartment == null )
        {
            taskRef = name;
            if( name.contains( "." ) )
            {
                String[] parts = name.split( "\\." );
                diagramAlias = parts[0];
                name = parts[1];//name.replace( ".", "_" );
                taskRef = parts[1];
                Diagram importedDiagram = imports.get( diagramAlias );
                diagramRef = importedDiagram.getName();

                if( taskRef.equals( WDLConstants.MAIN_WORKFLOW ) )
                {
                    taskСompartment = importedDiagram;
                    //                    externalDiagram = true;
                }
                else
                {
                    DiagramElement de = importedDiagram.get( taskRef );
                    if( ! ( de instanceof Compartment ) )
                        throw new Exception( "Can not resolve call " + call.getTaskName() );
                    taskСompartment = (Compartment)importedDiagram.get( taskRef );
                }
            }
        }
        else
            taskRef = taskСompartment.getName();
        String title = name;
        String alias = call.getAlias();
        if( alias != null )
            title = alias;

        name = DefaultSemanticController.generateUniqueName( diagram, "Call_" + name );
        Stub kernel = new Stub( null, name, WDLConstants.CALL_TYPE );

        Compartment c = new Compartment( parent, name, kernel );
        c.setShapeSize( new Dimension( 200, 0 ) );
        c.setTitle( title );
        WorkflowUtil.setTaskRef( c, taskRef );
        WorkflowUtil.setCallName( c, title );

        if( diagramRef != null )
            WorkflowUtil.setDiagramRef( c, diagramRef );
        if( diagramAlias != null )
            WorkflowUtil.setExternalDiagramAlias( c, diagramAlias );
        c.setNotificationEnabled( false );

        int inputs = 0;
        int outputs = 0;

        Collection<InputInfo> inputsInfo = call.getInputs();
        Set<String> addedInputs = new HashSet<>();//TODO: refactor this
        for( InputInfo symbol : inputsInfo )
        {
            String inputName = symbol.getName();
            addedInputs.add( inputName );
            String expression = symbol.getExpression();

            if( expression == null )
                expression = inputName;

            Node portNode = addPort( inputName, WDLConstants.INPUT_TYPE, inputs++, c );

            if( taskСompartment instanceof Diagram )
            {
                for( Node node : WorkflowUtil.getExternalParameters( (Diagram)taskСompartment ) )
                {
                    String varName = WorkflowUtil.getName( node );
                    if( varName.equals( inputName ) )
                    {
                        WorkflowUtil.setPosition( portNode, WorkflowUtil.getPosition( node ) );
                        Node inputNode = WorkflowUtil.getTarget( node );
                        if( inputNode != null )
                            node = inputNode;
                        WorkflowUtil.setName( portNode, WorkflowUtil.getName( node ) );
                        WorkflowUtil.setType( portNode, WorkflowUtil.getType( node ) );
                        WorkflowUtil.setExpression( portNode, expression );
                    }
                }
            }
            else
            {
                for( Node node : taskСompartment.getNodes() )
                {
                    if( WorkflowUtil.isInput( node ) )
                    {
                        String varName = WorkflowUtil.getName( node );
                        if( varName == null )
                        {
                            System.out.println( "" );
                        }
                        if( varName.equals( inputName ) )
                        {
                            WorkflowUtil.setName( portNode, WorkflowUtil.getName( node ) );
                            WorkflowUtil.setType( portNode, WorkflowUtil.getType( node ) );
                            WorkflowUtil.setExpression( portNode, expression );
                            WorkflowUtil.setPosition( portNode, WorkflowUtil.getPosition( node ) );
                        }
                    }
                }
            }
        }

        if( taskСompartment instanceof Diagram )
        {
            for( Node node : WorkflowUtil.getExternalOutputs( (Diagram)taskСompartment ) )
            {
                Node portNode = addPort( node.getName(), WDLConstants.OUTPUT_TYPE, outputs++, c );
                WorkflowUtil.setName( portNode, WorkflowUtil.getName( node ) );
                WorkflowUtil.setType( portNode, WorkflowUtil.getType( node ) );
                WorkflowUtil.setExpression( portNode, WorkflowUtil.getExpression( node ) );
                WorkflowUtil.setPosition( portNode, WorkflowUtil.getPosition( node ) );
            }
            for( Node node : WorkflowUtil.getExternalParameters( (Diagram)taskСompartment ) )
            {
                if( !addedInputs.contains( WorkflowUtil.getName( node ) ) )
                {
                    Node portNode = addPort( node.getName(), WDLConstants.INPUT_TYPE, inputs++, c );
                    WorkflowUtil.setName( portNode, WorkflowUtil.getName( node ) );
                    WorkflowUtil.setType( portNode, WorkflowUtil.getType( node ) );
                    WorkflowUtil.setExpression( portNode, WorkflowUtil.getExpression( node ) );
                    WorkflowUtil.setPosition( portNode, WorkflowUtil.getPosition( node ) );
                }
            }
        }
        else
        {
            for( Node node : taskСompartment.getNodes() )
            {
                Node portNode = null;
                if( WDLConstants.OUTPUT_TYPE.equals( node.getKernel().getType() ) )
                {
                    portNode = addPort( node.getName(), WDLConstants.OUTPUT_TYPE, outputs++, c );
                    WorkflowUtil.setName( portNode, WorkflowUtil.getName( node ) );
                    WorkflowUtil.setType( portNode, WorkflowUtil.getType( node ) );
                    WorkflowUtil.setExpression( portNode, WorkflowUtil.getExpression( node ) );
                }
                else if( !addedInputs.contains( WorkflowUtil.getName( node ) ) )
                {
                    portNode = addPort( node.getName(), WDLConstants.INPUT_TYPE, inputs++, c );
                    WorkflowUtil.setName( portNode, WorkflowUtil.getName( node ) );
                    WorkflowUtil.setType( portNode, WorkflowUtil.getType( node ) );
                    WorkflowUtil.setPosition( portNode, WorkflowUtil.getPosition( node ) );
                }
            }
        }
        int maxPorts = Math.max( inputs, outputs );
        int height = Math.max( 50, 24 * maxPorts + 8 );
        c.setShapeSize( new Dimension( 200, height ) );
        c.getAttributes().add( new DynamicProperty( "innerNodesPortFinder", Boolean.class, true ) );
        c.setNotificationEnabled( true );
        parent.put( c );
        return c;
    }


    public static Node addPort(String name, String nodeType, int position, Compartment parent) throws DataElementPutException
    {
        Node inNode = new Node( parent, new Stub( parent, name, nodeType ) );
        WorkflowUtil.setPosition( inNode, position );
        inNode.setFixed( true );
        Point parentLoc = parent.getLocation();
        Dimension parentDim = parent.getShapeSize();
        if( WDLConstants.INPUT_TYPE.equals( nodeType ) )
            inNode.setLocation( parentLoc.x + 2, parentLoc.y + position * 24 + 8 );
        else
            inNode.setLocation( parentLoc.x + parentDim.width - 16 - 2, parentLoc.y + position * 24 + 8 );
        parent.put( inNode );
        return inNode;
    }

    public static Edge createLogicalLink(Node input, Node output, String type)
    {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put( WDLConstants.LOGICAL_TYPE_ATTR, type );
        return createLink( input, output, WDLConstants.LOGICAL_LINK_TYPE, new HashMap<>() );
    }

    public static Edge createLink(Node input, Node output)
    {
        return createLink( input, output, WDLConstants.LINK_TYPE, new HashMap<>() );
    }

    public static Edge createLink(Node input, Node output, Map<String, Object> attributes)
    {
        return createLink( input, output, WDLConstants.LINK_TYPE, attributes );
    }

    public static Edge createLink(Node input, Node output, String type, Map<String, Object> attributes)
    {
        if( input.equals( output ) )
            return null;
        String name = input.getName() + "_to_" + output.getName();
        Diagram d = Diagram.getDiagram( input );
        name = DefaultSemanticController.generateUniqueName( d, name );
        Edge e = new Edge( new Stub( null, name, type ), input, output );
        for( Entry<String, Object> entry : attributes.entrySet() )
        {
            e.getAttributes().add( new DynamicProperty( entry.getKey(), entry.getValue().getClass(), entry.getValue() ) );
        }
        e.getCompartment().put( e );
        return e;
    }

}
