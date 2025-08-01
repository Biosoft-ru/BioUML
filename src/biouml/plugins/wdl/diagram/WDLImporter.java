package biouml.plugins.wdl.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
import biouml.plugins.wdl.WDLUtil;
import biouml.plugins.wdl.parser.AstCall;
import biouml.plugins.wdl.parser.AstDeclaration;
import biouml.plugins.wdl.parser.AstExpression;
import biouml.plugins.wdl.parser.AstImport;
import biouml.plugins.wdl.parser.AstInput;
import biouml.plugins.wdl.parser.AstOutput;
import biouml.plugins.wdl.parser.AstRegularFormulaElement;
import biouml.plugins.wdl.parser.AstScatter;
import biouml.plugins.wdl.parser.AstStart;
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

        for( int i = 0; i < start.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.wdl.parser.Node n = start.jjtGetChild( i );
            if( n instanceof AstVersion )
            {
                String version = ( (AstVersion)n ).jjtGetLastToken().toString();
                WDLUtil.setVersion( result, version );
            }
            else if( n instanceof AstImport )
            {
                createImport( result, (AstImport)n );
            }
            else if( n instanceof AstTask )
            {
                createTaskNode( result, (AstTask)n );
            }
            else if( n instanceof AstWorkflow )
            {
                AstWorkflow workflow = (AstWorkflow)n;
                for( biouml.plugins.wdl.parser.Node child : workflow.getChildren() )
                {
                    if( child instanceof AstInput )
                    {
                        for( AstDeclaration astD : StreamEx.of( ( (AstInput)child ).getChildren() ).select( AstDeclaration.class ) )
                            createExternalParameterNode( result, astD );
                    }
                    else if( child instanceof AstOutput )
                    {
                        for( AstDeclaration astD : StreamEx.of( ( (AstOutput)child ).getChildren() ).select( AstDeclaration.class ) )
                            createExpressionNode( result, astD );
                    }
                    else if( child instanceof AstDeclaration )
                    {
                        createExpressionNode( result, (AstDeclaration)child );
                    }
                }

                for( biouml.plugins.wdl.parser.Node child : workflow.getChildren() )
                {
                    if( child instanceof AstCall )
                    {
                        createCallNode( result, (AstCall)child );
                    }
                    else if( child instanceof AstScatter )
                    {
                        createScatterNode( result, (AstScatter)child );
                    }
                }
            }
        }

        createLinks( result );
        return result;
    }

    public void createImport(Diagram diagram, AstImport astImport)
    {
        try
        {
            Diagram imported = (Diagram)diagram.getOrigin().get( astImport.getSource() );
            imports.put( astImport.getAlias(), imported );
            WDLUtil.addImport( diagram, imported, astImport.getAlias() );
        }
        catch( Exception ex )
        {
            System.out.println( "Can not resolve import " + astImport.toString() );
        }
    }

    public Node createExternalParameterNode(Compartment parent, AstDeclaration declaration)
    {
        String name = declaration.getName();
        Stub kernel = new Stub( null, name, WDLConstants.EXTERNAL_PARAMETER_TYPE );
        Node node = new Node( parent, name, kernel );
        setDeclaration( node, declaration );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public void createLinks(Diagram diagram)
    {
        for( Node node : diagram.stream().select( Node.class ).filter( n -> WDLUtil.getExpression( n ) != null ) )
        {
            Node source = WDLUtil.findExpressionNode( diagram, WDLUtil.getExpression( node ) );
            if( source != null )
                createLink( source, node, WDLConstants.LINK_TYPE );
        }
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

    public Compartment createTaskNode(Compartment parent, AstTask task)
    {
        String name = task.getName();
        name = WDLSemanticController.uniqName( parent, name );
        Stub kernel = new Stub( null, name, WDLConstants.TASK_TYPE );

        Compartment c = new Compartment( parent, name, kernel );
        WDLUtil.setBeforeCommand( c, task.getBeforeCommand().stream().map( d -> new Declaration( d ) ).toArray( Declaration[]::new ) );
        WDLUtil.setCommand( c, task.getCommand() );
        WDLUtil.setRuntime( c, task.getRuntime() );
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

    public Compartment createScatterNode(Compartment parent, AstScatter scatter) throws Exception
    {
        String name = "scatter";
        name = WDLSemanticController.uniqName( parent, name );
        Stub kernel = new Stub( null, name, WDLConstants.SCATTER_TYPE );
        Compartment c = new Compartment( parent, name, kernel );

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
        WDLUtil.setName( variableNode, variable );
        c.put( variableNode );

        createLink( arrayNode, variableNode, WDLConstants.LINK_TYPE );

        parent.put( c );

        Iterable<biouml.plugins.wdl.parser.Node> body = scatter.getBody();
        for( biouml.plugins.wdl.parser.Node n : body )
        {
            if( n instanceof AstDeclaration )
            {
                //                System.out.println( ( (AstDeclaration)n ).getName() );
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
            else
            {
                System.out.println( n.toString() );
            }

        }

        return c;
    }

    private Node createExpression(AstExpression expression, String type, Compartment parent)
    {
        String name = DefaultSemanticController.generateUniqueName( parent, "expression" );
        Node resultNode = new Node( parent, name, new Stub( null, name, WDLConstants.EXPRESSION_TYPE ) );
        WDLUtil.setExpression( resultNode, expression.toString() );
        WDLUtil.setName( resultNode, name );
        WDLUtil.setType( resultNode, type );
        parent.put( resultNode );
        return resultNode;
    }

    private static void setDeclaration(Node node, AstDeclaration declaration)
    {
        WDLUtil.setName( node, declaration.getName() );
        WDLUtil.setType( node, declaration.getAstType().toString() );
        AstExpression expression = declaration.getExpression();
        WDLUtil.setExpression( node, expression == null ? null : expression.toString() );
    }


    public Compartment createCallNode(Compartment parent, AstCall call) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( parent );
        String name = call.getName();
        Compartment taskСompartment = tasks.get( name );
        String taskRef = name;
        String diagramRef = null;
        String diagramAlias = null;
        if( taskСompartment == null )
        {
            taskRef = name;
            if( name.contains( "." ) )
            {
                String[] parts = name.split( "\\." );
                diagramAlias = parts[0];
                taskRef = parts[1];
                Diagram importedDiagram = imports.get( diagramAlias );
                diagramRef = importedDiagram.getName();
                DiagramElement de = importedDiagram.get( taskRef );
                if( ! ( de instanceof Compartment ) )
                    throw new Exception( "Can not resolve call " + call.getName() );
                taskСompartment = (Compartment)importedDiagram.get( taskRef );
            }
        }
        else
            taskRef = taskСompartment.getName();
        String title = name;
        String alias = call.getAlias();
        if( alias != null )
        {
            name = alias;
            title = alias;
        }
        else
            name = "Call_" + name;
        Stub kernel = new Stub( null, name, WDLConstants.CALL_TYPE );

        Compartment c = new Compartment( parent, name, kernel );
        c.setShapeSize( new Dimension( 200, 0 ) );
        c.setTitle( title );
        WDLUtil.setTaskRef( c, taskRef );
        WDLUtil.setCallName( c, title );
        if( diagramRef != null )
            WDLUtil.setDiagramRef( c, diagramRef );
        if( diagramAlias != null )
            WDLUtil.setExternalDiagramAlias( c, diagramAlias );
        c.setNotificationEnabled( false );

        int inputs = 0;
        int outputs = 0;

        AstSymbol[] inputSymbols = call.getInputs();

        for( AstSymbol symbol : inputSymbols )
        {
            String inputName = symbol.getName();
            String expression = inputName;
            AstExpression expr = null;
            if( symbol.getChildren() != null )
            {
                expr = WDLUtil.findChild( symbol, AstExpression.class );
                if( expr != null )
                    expression = expr.toString();
            }

            Node portNode = addPort( inputName, WDLConstants.INPUT_TYPE, inputs++, c );

            for( Node node : taskСompartment.getNodes() )
            {
                String varName = WDLUtil.getName( node );
                if( varName.equals( inputName ) )
                {
                    WDLUtil.setName( portNode, WDLUtil.getName( node ) );
                    WDLUtil.setType( portNode, WDLUtil.getType( node ) );
                    WDLUtil.setExpression( portNode, expression );
                }
            }

            if( expr != null )
            {
                for( String argument : expr.getArguments() )
                {
                    Node expressionNode = WDLUtil.findExpressionNode( diagram, argument );
                    if( expressionNode != null )
                        createLink( expressionNode, portNode, WDLConstants.LINK_TYPE );
                }
            }
        }

        for( Node node : taskСompartment.getNodes() )
        {
            String varName = WDLUtil.getName( node );
            Node portNode = null;
            //            if( WDLConstants.INPUT_TYPE.equals( node.getKernel().getType() ) )
            //            {
            //                portNode = addPort( node.getName(), WDLConstants.INPUT_TYPE, inputs++, c );
            //                Node input = WDLUtil.findExpressionNode( diagram, varName );
            //                if( input != null )
            //                    createLink( input, portNode, WDLConstants.LINK_TYPE );
            //            }
            if( WDLConstants.OUTPUT_TYPE.equals( node.getKernel().getType() ) )
            {
                portNode = addPort( node.getName(), WDLConstants.OUTPUT_TYPE, outputs++, c );
                //                Node output = WDLUtil.findExpressionNode( diagram, varName ); //diagram.findNode( node.getName() );
                //                if( output != null )
                //                    createLink( portNode, output, WDLConstants.LINK_TYPE );
                WDLUtil.setName( portNode, WDLUtil.getName( node ) );
                WDLUtil.setType( portNode, WDLUtil.getType( node ) );
                WDLUtil.setExpression( portNode, WDLUtil.getExpression( node ) );
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
        inNode.getAttributes().add( new DynamicProperty( "position", Integer.class, position ) );
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
        d.put( e );
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
