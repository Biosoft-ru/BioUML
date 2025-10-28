package biouml.plugins.wdl.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.Declaration;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.parser.AstCall;
import biouml.plugins.wdl.parser.AstConditional;
import biouml.plugins.wdl.parser.AstDeclaration;
import biouml.plugins.wdl.parser.AstExpression;
import biouml.plugins.wdl.parser.AstImport;
import biouml.plugins.wdl.parser.AstInput;
import biouml.plugins.wdl.parser.AstMeta;
import biouml.plugins.wdl.parser.AstOutput;
import biouml.plugins.wdl.parser.AstRegularFormulaElement;
import biouml.plugins.wdl.parser.AstScatter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.AstStruct;
import biouml.plugins.wdl.parser.AstSymbol;
import biouml.plugins.wdl.parser.AstTask;
import biouml.plugins.wdl.parser.AstVersion;
import biouml.plugins.wdl.parser.AstWorkflow;
import biouml.plugins.wdl.parser.WDLParser;
import biouml.standard.type.Stub;
import biouml.workbench.graph.DiagramToGraphTransformer;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.bean.BeanInfoEx2;


public class WDLImporter implements DataElementImporter
{
    private WDLImportProperties properties = null;
    protected static final Logger log = Logger.getLogger( WDLImporter.class.getName() );
    private boolean doImportDiagram = false;
    private Map<String, Diagram> imports = new HashMap<>();
    private Map<String, Compartment> tasks = new HashMap<>();
    private int externalPosition = 0;

    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent.isAcceptable( Diagram.class ) )
            if( file == null )
                return ACCEPT_HIGHEST_PRIORITY;
            else
            {
                String lcname = file.getName().toLowerCase();
                if( lcname.endsWith( ".wdl" ) )
                    return ACCEPT_HIGHEST_PRIORITY;
            }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(DataCollection<?> parent, File file, String diagramName, FunctionJobControl jobControl, Logger log)
            throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();
        doImportDiagram = true;

        try (FileInputStream in = new FileInputStream( file );
                InputStreamReader reader = new InputStreamReader( in, StandardCharsets.UTF_8 ))
        {
            if( properties.getDiagramName() == null || properties.getDiagramName().isEmpty() )
                throw new Exception( "Please specify diagram name." );

            String text = ApplicationUtils.readAsString( file );
            text = text.replace( "<<<", "{" ).replace( ">>>", "}" );//TODO: fix parsing <<< >>>

            AstStart start = new WDLParser().parse( new StringReader( text ) );
            Diagram diagram = generateDiagram( start, parent, properties.getDiagramName() );

            if( jobControl != null )
                jobControl.functionFinished();

            layout( diagram );
            CollectionFactoryUtils.save( diagram );

            return diagram;
        }
        catch( Exception e )
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError( e );
            throw e;
        }
    }


    @Override
    public Object getProperties(DataCollection<?> parent, File file, String elementName)
    {
        if( properties == null )
            properties = new WDLImportProperties();
        if( elementName != null )
            properties.setDiagramName( elementName );
        else if( file != null )
            properties.setDiagramName( file.getName() );
        return properties;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return Diagram.class;
    }

    @Override
    public boolean init(Properties arg0)
    {
        return true;
    }

    public static class WDLImportProperties extends Option
    {
        private String diagramName;

        @PropertyName ( "Diagram name" )
        public String getDiagramName()
        {
            return diagramName;
        }
        public void setDiagramName(String diagramName)
        {
            this.diagramName = diagramName;
        }
    }

    public static class WDLImportPropertiesBeanInfo extends BeanInfoEx2<WDLImportProperties>
    {
        public WDLImportPropertiesBeanInfo()
        {
            super( WDLImportProperties.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "diagramName" );
        }
    }

    public Diagram generateDiagram(AstStart start, DataCollection dc, String name) throws Exception
    {
        Diagram result = new WDLDiagramType().createDiagram( dc, name, null );
        return generateDiagram( start, result );
    }

    public Diagram generateDiagram(AstStart start, Diagram diagram) throws Exception
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

        for( int i = 0; i < start.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.wdl.parser.Node n = start.jjtGetChild( i );
            if( n instanceof AstVersion )
            {
                String version = ( (AstVersion)n ).jjtGetLastToken().toString();
                WorkflowUtil.setVersion( diagram, version );
            }
            else if( n instanceof AstImport )
            {
                createImport( diagram, (AstImport)n );
            }
            else if( n instanceof AstTask )
            {
                createTaskNode( diagram, (AstTask)n );
            }
            else if( n instanceof AstStruct )
            {
                createStruct( diagram, (AstStruct)n );
            }
        }

        for( int i = 0; i < start.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.wdl.parser.Node n = start.jjtGetChild( i );
            if( n instanceof AstWorkflow )
            {
                AstWorkflow workflow = (AstWorkflow)n;
                for( biouml.plugins.wdl.parser.Node child : workflow.getChildren() )
                {
                    if( child instanceof AstInput )
                    {
                        for( AstDeclaration astD : StreamEx.of( ( (AstInput)child ).getChildren() ).select( AstDeclaration.class ) )
                            createExternalParameterNode( diagram, astD );
                    }
                    else if( child instanceof AstOutput )
                    {
                        for( AstDeclaration astD : StreamEx.of( ( (AstOutput)child ).getChildren() ).select( AstDeclaration.class ) )
                            createOutputNode( diagram, astD );
                    }
                    else if( child instanceof AstDeclaration )
                    {
                        createExpressionNode( diagram, (AstDeclaration)child );
                    }
                    else if( child instanceof AstMeta )
                    {
                        WorkflowUtil.setMeta( diagram, (AstMeta)child );
                    }
                }

                for( biouml.plugins.wdl.parser.Node child : workflow.getChildren() )
                {
                    if( child instanceof AstCall )
                    {
                        createCallNode( diagram, (AstCall)child );
                    }
                    else if( child instanceof AstScatter )
                    {
                        createScatterNode( diagram, (AstScatter)child );
                    }
                    else if( child instanceof AstConditional )
                    {
                        createConditionalNode( diagram, (AstConditional)child );
                    }
                }
            }
        }

        createLinks( diagram );
        return diagram;
    }

    public Node createStruct(Compartment parent, AstStruct astStruct) throws Exception
    {
        String name = astStruct.getName();
        Stub kernel = new Stub( null, name, WDLConstants.STRUCT_TYPE );
        Node node = new Node( parent, name, kernel );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 50, 50 ) );
        parent.put( node );

        List<AstDeclaration> astDeclarations = astStruct.getDeclarations();
        Declaration[] declarations = StreamEx.of( astDeclarations ).map( ast -> new Declaration( ast ) ).toArray( Declaration[]::new );
        WorkflowUtil.setStructMembers( node, declarations );
        return node;
    }

    public void createImport(Diagram diagram, AstImport astImport) throws Exception
    {
        try
        {
            DataElementPath dep = diagram.getOrigin().getCompletePath();
            String path = astImport.getSource();
            String[] parts = path.split( "/" );
            DataElementPath importPath = dep.getChildPath( parts );
            Diagram imported = importPath.getDataElement( Diagram.class );
            imports.put( astImport.getAlias(), imported );
            if( imported == null )
                throw new Exception( "Imported diagram " + astImport.getSource() + " not found!" );
            WorkflowUtil.addImport( diagram, imported, astImport.getAlias() );
        }
        catch( Exception ex )
        {
            log.info( "Can not process import " + astImport.toString() + ": " + ex.getMessage() );
        }
    }

    public Node createExternalParameterNode(Compartment parent, AstDeclaration declaration)
    {
        String name = declaration.getName();
        Stub kernel = new Stub( null, name, WDLConstants.EXTERNAL_PARAMETER_TYPE );
        Node node = new Node( parent, name, kernel );
        WorkflowUtil.setPosition( node, externalPosition++ );
        setDeclaration( node, declaration );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public void createLinks(Diagram diagram)
    {
        for( Node node : diagram.recursiveStream().select( Node.class ) )
        {
            String expression = WorkflowUtil.getExpression( node );
            if (expression == null)
                continue;
            
            if (WorkflowUtil.isOutput( node ))
                continue;

            List<String> args = WorkflowUtil.findPossibleArguments( expression );
            for( String arg : args )
            {
                Node source = WorkflowUtil.findExpressionNode( diagram, arg );
                if( source != null )
                    createLink( source, node, WDLConstants.LINK_TYPE );
            }
        }
    }

    public Node createConditionNode(Compartment parent, AstExpression expression)
    {
        String name = WDLSemanticController.uniqName( parent, "condition" );
        Stub kernel = new Stub( null, name, WDLConstants.CONDITION_TYPE );
        Node node = new Node( parent, name, kernel );
        WorkflowUtil.setExpression( node, expression == null ? null : expression.toString() );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public Node createExpressionNode(Compartment parent, AstDeclaration declaration)
    {
        String name = declaration.getName();
        Stub kernel = new Stub( null, name, WDLConstants.EXPRESSION_TYPE );
        Node node = new Node( parent, name, kernel );
        setDeclaration( node, declaration );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }


    public Node createOutputNode(Compartment parent, AstDeclaration declaration)
    {
        String name = declaration.getName();
        Stub kernel = new Stub( null, name, WDLConstants.WORKFLOW_OUTPUT_TYPE );
        Node node = new Node( parent, name, kernel );
        setDeclaration( node, declaration );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public Compartment createTaskNode(Compartment parent, AstTask task)
    {
        String name = task.getName();
        name = WDLSemanticController.uniqName( parent, name );
        Stub kernel = new Stub( null, name, WDLConstants.TASK_TYPE );

        Compartment c = new Compartment( parent, name, kernel );
        WorkflowUtil.setBeforeCommand( c, task.getBeforeCommand().stream().map( d -> new Declaration( d ) ).toArray( Declaration[]::new ) );
        WorkflowUtil.setCommand( c, task.getCommand() );
        WorkflowUtil.setRuntime( c, task.getRuntime() );
        c.setTitle( name );
        c.setNotificationEnabled( false );
        c.setShapeSize( new Dimension( 200, 0 ) );
        tasks.put( name, c );
        AstInput input = task.getInput();
        int maxPorts = 0;
        int i = 0;
        if( input != null )
        {
            for( AstDeclaration declaration : input.getDeclarations() )
            {
                Node portNode = addPort( WDLSemanticController.uniqName( parent, "input" ), WDLConstants.INPUT_TYPE, i++, c );
                setDeclaration( portNode, declaration );
            }
            maxPorts = input.getDeclarations().size();
        }

        AstOutput output = task.getOutput();
        i = 0;
        if( output != null )
        {
            for( AstDeclaration declaration : output.getDeclarations() )
            {
                Node portNode = addPort( WDLSemanticController.uniqName( parent, "output" ), WDLConstants.OUTPUT_TYPE, i++, c );
                setDeclaration( portNode, declaration );
            }
            maxPorts = Math.max( maxPorts, output.getDeclarations().size() );
        }

        int height = Math.max( 50, 24 * maxPorts + 8 );
        c.setShapeSize( new Dimension( 200, height ) );
        c.getAttributes().add( new DynamicProperty( "innerNodesPortFinder", Boolean.class, true ) );
        c.setNotificationEnabled( true );
        parent.put( c );
        return c;
    }

    public Compartment createConditionalNode(Compartment parent, AstConditional conditional) throws Exception
    {
        String name = WDLSemanticController.uniqName( parent, "conditional" );
        Stub kernel = new Stub( null, name, WDLConstants.CONDITIONAL_TYPE );
        Compartment c = new Compartment( parent, name, kernel );
        c.setShapeSize( new Dimension( 700, 700 ) );

        for( biouml.plugins.wdl.parser.Node n : conditional.getChildren() )
        {
            if( n instanceof AstExpression )
            {
                Node conditionNode = createConditionNode( parent, (AstExpression)n );
                createLink( conditionNode, c, WDLConstants.LINK_TYPE );
            }
        }

        parent.put( c );

        for( biouml.plugins.wdl.parser.Node n : conditional.getChildren() )
        {
            if( n instanceof AstDeclaration )
            {
                createExpressionNode( c, (AstDeclaration)n );
            }
            else if( n instanceof AstCall )
            {
                createCallNode( c, (AstCall)n );
            }
            else if( n instanceof AstScatter )
            {
                createScatterNode( c, (AstScatter)n );
            }
            else if( n instanceof AstConditional )
            {
                createConditionalNode( c, (AstConditional)n );
            }
        }
        return c;
    }

    public Compartment createScatterNode(Compartment parent, AstScatter scatter) throws Exception
    {
        String name = "scatter";
        name = WDLSemanticController.uniqName( parent, name );
        Stub kernel = new Stub( null, name, WDLConstants.SCATTER_TYPE );
        Compartment c = new Compartment( parent, name, kernel );
        c.setShapeSize( new Dimension( 500, 300 ) );
        String variable = scatter.getVarible();
        AstExpression array = scatter.getArrayExpression();

        Node arrayNode = null;
        if( array.getChildren().length == 1 && array.getChildren()[0] instanceof AstRegularFormulaElement )
        {
            arrayNode = Diagram.getDiagram( parent ).findNode( array.toString() );
        }
        else
        {
            arrayNode = createExpression( array, "Array[Int]", parent );
        }
        name = WDLSemanticController.uniqName( parent, variable );
        Node variableNode = new Node( c, name, new Stub( null, name, WDLConstants.SCATTER_VARIABLE_TYPE ) );
        WorkflowUtil.setName( variableNode, variable );
        c.put( variableNode );

        createLink( arrayNode, variableNode, WDLConstants.LINK_TYPE );

        parent.put( c );

        Iterable<biouml.plugins.wdl.parser.Node> body = scatter.getBody();
        for( biouml.plugins.wdl.parser.Node n : body )
        {
            if( n instanceof AstDeclaration )
            {
                createExpressionNode( parent, (AstDeclaration)n );
            }
            else if( n instanceof AstCall )
            {
                createCallNode( c, (AstCall)n );
            }
            else if( n instanceof AstScatter )
            {
                createScatterNode( c, (AstScatter)n );
            }
            else if( n instanceof AstConditional )
            {
                createConditionalNode( c, (AstConditional)n );
            }
        }
        return c;
    }

    private Node createExpression(AstExpression expression, String type, Compartment parent)
    {
        String name = DefaultSemanticController.generateUniqueName( parent, "expression" );
        Node resultNode = new Node( parent, name, new Stub( null, name, WDLConstants.EXPRESSION_TYPE ) );
        WorkflowUtil.setExpression( resultNode, expression.toString() );
        WorkflowUtil.setName( resultNode, name );
        WorkflowUtil.setType( resultNode, type );
        parent.put( resultNode );
        return resultNode;
    }

    private static void setDeclaration(Node node, AstDeclaration declaration)
    {
        WorkflowUtil.setName( node, declaration.getName() );
        WorkflowUtil.setType( node, declaration.getAstType().toString() );
        AstExpression expression = declaration.getExpression();
        WorkflowUtil.setExpression( node, expression == null ? null : expression.toString() );
    }


    public Compartment createCallNode(Compartment parent, AstCall call) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( parent );
        String name = call.getName();

        Compartment taskСompartment = tasks.get( name );
        String taskRef = name;
        String diagramRef = null;
        String diagramAlias = null;
        boolean externalDiagram = false;
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
                    externalDiagram = true;
                }
                else
                {
                    DiagramElement de = importedDiagram.get( taskRef );
                    if( ! ( de instanceof Compartment ) )
                        throw new Exception( "Can not resolve call " + call.getName() );
                    taskСompartment = (Compartment)importedDiagram.get( taskRef );
                }
            }
        }
        else
            taskRef = taskСompartment.getName();
        String title = name;
        String alias = call.getAlias();
        if( alias != null )
        {
            //            name = alias;
            title = alias;
        }

        name = DefaultSemanticController.generateUniqueName( diagram, "Call_" + name );
        Stub kernel = new Stub( null, name, WDLConstants.CALL_TYPE );

        Compartment c = new Compartment( parent, name, kernel );
        c.setShapeSize( new Dimension( 200, 0 ) );
        c.setTitle( title );
        WorkflowUtil.setTaskRef( c, taskRef );
        WorkflowUtil.setCallName( c, title );

        for( AstMeta meta : WorkflowUtil.findChild( call, AstMeta.class ) )
            WorkflowUtil.setMeta( diagram, meta );

        if( diagramRef != null )
            WorkflowUtil.setDiagramRef( c, diagramRef );
        if( diagramAlias != null )
            WorkflowUtil.setExternalDiagramAlias( c, diagramAlias );
        c.setNotificationEnabled( false );

        int inputs = 0;
        int outputs = 0;

        AstSymbol[] inputSymbols = call.getInputs();
        Set<String> addedInputs = new HashSet<>();//TODO: refactor this
        for( AstSymbol symbol : inputSymbols )
        {
            String inputName = symbol.getName();
            addedInputs.add( inputName );
            String expression = inputName;
            AstExpression expr = null;
            if( symbol.getChildren() != null )
            {
                List<AstExpression> exprs = WorkflowUtil.findChild( symbol, AstExpression.class );
                if( !exprs.isEmpty() )
                {
                    expr = exprs.get( 0 );
                    expression = expr.toString();
                }
            }

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
                    String varName = WorkflowUtil.getName( node );
                    if( varName.equals( inputName ) )
                    {
                        WorkflowUtil.setName( portNode, WorkflowUtil.getName( node ) );
                        WorkflowUtil.setType( portNode, WorkflowUtil.getType( node ) );
                        WorkflowUtil.setExpression( portNode, expression );
                        WorkflowUtil.setPosition( portNode, WorkflowUtil.getPosition( node ) );
                    }
                }
            }

            if( expr != null )
            {
                for( String argument : expr.getArguments() )
                {
                    Node expressionNode = WorkflowUtil.findExpressionNode( diagram, argument );
                    if( expressionNode != null )
                        createLink( expressionNode, portNode, WDLConstants.LINK_TYPE );
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
                    //WDLUtil.setPosition( portNode, WDLUtil.getPosition( node ) );
                }
                else if( !addedInputs.contains( WorkflowUtil.getName( node ) ) )
                {
                    portNode = addPort( node.getName(), WDLConstants.INPUT_TYPE, inputs++, c );
                    WorkflowUtil.setName( portNode, WorkflowUtil.getName( node ) );
                    WorkflowUtil.setType( portNode, WorkflowUtil.getType( node ) );
                    //WDLUtil.setExpression( portNode, WDLUtil.getExpression( node ) );
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


    private Node addPort(String name, String nodeType, int position, Compartment parent) throws DataElementPutException
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

    public static Edge createLink(Node input, Node output, String type)
    {
        String name = input.getName() + "_to_" + output.getName();
        Diagram d = Diagram.getDiagram( input );
        name = DefaultSemanticController.generateUniqueName( d, name );
        Edge e = new Edge( new Stub( null, name, type ), input, output );
        e.getCompartment().put( e );
        return e;
    }

    public void layout(Diagram diagram)
    {
        HierarchicLayouter layouter = new HierarchicLayouter();
        layouter.setHoistNodes( true );
        layouter.getSubgraphLayouter().layerDeltaY = 50;
        ru.biosoft.graph.Graph graph = DiagramToGraphTransformer.generateGraph( diagram, null );
        PathwayLayouter pathwayLayouter = new PathwayLayouter( layouter );
        pathwayLayouter.doLayout( graph, null );
        DiagramToGraphTransformer.applyLayout( graph, diagram );
        diagram.setView( null );
        diagram.getViewOptions().setAutoLayout( true );
    }

}
