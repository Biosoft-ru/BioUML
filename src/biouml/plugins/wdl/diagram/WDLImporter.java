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
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WDLUtil;
import biouml.plugins.wdl.parser.AstCall;
import biouml.plugins.wdl.parser.AstDeclaration;
import biouml.plugins.wdl.parser.AstExpression;
import biouml.plugins.wdl.parser.AstInput;
import biouml.plugins.wdl.parser.AstOutput;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.AstTask;
import biouml.plugins.wdl.parser.AstVersion;
import biouml.plugins.wdl.parser.AstWorkflow;
import biouml.plugins.wdl.parser.WDLParser;
import biouml.standard.type.Stub;
import biouml.workbench.graph.DiagramToGraphTransformer;
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
            if( n instanceof AstTask )
            {
                createTaskNode( result, (AstTask)n );
            }
            else if( n instanceof AstWorkflow )
            {
                Compartment c = null;
                for( biouml.plugins.wdl.parser.Node child : ( (AstWorkflow)n ).getChildren() )
                {
                    if( child instanceof AstCall )
                    {
                        c = createCallNode( result, (AstCall)child );
                    }
                }
                for( biouml.plugins.wdl.parser.Node child : ( (AstWorkflow)n ).getChildren() )
                {
                    if( child instanceof AstInput )
                    {
                        String inputName = "input";
                        for( biouml.plugins.wdl.parser.Node cc : ( (AstInput)child ).getChildren() )
                        {
                            if( cc instanceof AstDeclaration )
                            {
                                inputName = ( (AstDeclaration)cc ).getName();
                                Node parameterNode = createExternalParameterNode( result, inputName );
                                Node port = c.findNode( inputName );
                                WDLUtil.setName( parameterNode, ( (AstDeclaration)cc ).getName() );
                                WDLUtil.setType( parameterNode, ( (AstDeclaration)cc ).getAstType().toString() );
                                AstExpression expression = ( (AstDeclaration)cc ).getExpression();
                                WDLUtil.setExpression( parameterNode, expression == null ? null : expression.toString() );
                                createLink( parameterNode, port, WDLConstants.LINK_TYPE );
                            }
                        }

                    }
                    else if( child instanceof AstOutput )
                    {
                        for( biouml.plugins.wdl.parser.Node cc : ( (AstOutput)child ).getChildren() )
                        {
                            if( cc instanceof AstDeclaration )
                            {
                                String outputName = ( (AstDeclaration)cc ).getName();
                                Node parameterNode = createExpressionNode( result, outputName );
                                Node port = c.findNode( outputName );
                                WDLUtil.setName( parameterNode, ( (AstDeclaration)cc ).getName() );
                                WDLUtil.setType( parameterNode, ( (AstDeclaration)cc ).getAstType().toString() );
                                AstExpression expression = ( (AstDeclaration)cc ).getExpression();
                                WDLUtil.setExpression( parameterNode, expression == null ? null : expression.toString() );
                                createLink( port, parameterNode, WDLConstants.LINK_TYPE );
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private Map<String, Compartment> tasks = new HashMap<>();

    public Node createExternalParameterNode(Compartment parent, String name)
    {
        Stub kernel = new Stub( null, name, WDLConstants.EXTERNAL_PARAMETER_TYPE );
        Node node = new Node( parent, name, kernel );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public Node createEpressionNode(Compartment parent, String name)
    {
        Stub kernel = new Stub( null, name, WDLConstants.EXPRESSION_TYPE );
        Node node = new Node( parent, name, kernel );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }


    public Node createExpressionNode(Compartment parent, String name)
    {
        Stub kernel = new Stub( null, name, WDLConstants.EXPRESSION_TYPE );
        Node node = new Node( parent, name, kernel );
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
                Node portNode = addPort( declaration.getName(), WDLConstants.INPUT_TYPE, i++, c );
                WDLUtil.setName( portNode, declaration.getName() );
                WDLUtil.setType( portNode, declaration.getAstType().toString() );
                AstExpression expression = declaration.getExpression();
                WDLUtil.setExpression( portNode, expression == null ? null : expression.toString() );
            }
            maxPorts = input.getDeclarations().size();
        }

        AstOutput output = task.getOutput();
        i = 0;
        if( output != null )
        {
            for( AstDeclaration declaration : output.getDeclarations() )
            {
                Node portNode = addPort( declaration.getName(), WDLConstants.OUTPUT_TYPE, i++, c );
                WDLUtil.setName( portNode, declaration.getName() );
                WDLUtil.setType( portNode, declaration.getAstType().toString() );
                AstExpression expression = declaration.getExpression();
                WDLUtil.setExpression( portNode, expression == null ? null : expression.toString() );
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

    public Compartment createCallNode(Compartment parent, AstCall call)
    {
        String name = call.getName();
        Compartment taskСompartment = tasks.get( name );
        name = "Call_" + name;
        Stub kernel = new Stub( null, name, WDLConstants.CALL_TYPE );

        Compartment c = new Compartment( parent, name, kernel );

        c.setTitle( name );
        c.setNotificationEnabled( false );
        c.setShapeSize( taskСompartment.getShapeSize() );
        int inputs = 0;
        int outputs = 0;
        for( Node node : taskСompartment.getNodes() )
        {
            Node portNode = null;
            if( WDLConstants.INPUT_TYPE.equals( node.getKernel().getType() ) )
                portNode = addPort( node.getName(), WDLConstants.INPUT_TYPE, inputs++, c );
            else
                portNode = addPort( node.getName(), WDLConstants.OUTPUT_TYPE, outputs++, c );

            WDLUtil.setName( portNode, WDLUtil.getName( node ) );
        }
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

    private Edge createLink(Node input, Node output, String type)
    {
        String name = input.getName() + "_to_" + output.getName();
        Diagram d = Diagram.getDiagram( input );
        name = DefaultSemanticController.generateUniqueName( d, name );
        Edge e = new Edge( new Stub( null, name, type ), input, output );
        d.put( e );
        return e;
    }

    //    public Compartment createCallNode(Compartment parent, AstCall astCall)//,  Map<Object, Node> wdlToBioUML)
    //    {
    //        String name = astCall.getName();//fixName(wdlNode.localName());
    //        name = WDLSemanticController.uniqName( parent, name );
    ////        Stub kernel = new Stub( null, name, NodeType.COMMAND_CALL.name() );
    //
    ////        Compartment anNode = new Compartment( parent, name, kernel );
    //
    //        anNode.setTitle( name );
    //        anNode.setNotificationEnabled( false );
    //
    //        AstSymbol[] inputs = astCall.getInputs();
    //        //      List<InputPort> inputPorts = toJavaList( wdlNode.inputPorts() );
    //        //      List<OutputPort> outputPorts = toJavaList( wdlNode.outputPorts() );
    //
    //        //      int maxPorts = Math.max(inputPorts.size(), outputPorts.size());
    //        //      int height = Math.max(50, 24 * maxPorts + 8);
    //        //      anNode.setShapeSize(new Dimension(200, height));
    //        //      anNode.getAttributes().add(new DynamicProperty("innerNodesPortFinder", Boolean.class, true));
    //        //      for( int i = 0; i < inputPorts.size(); i++ )
    //        //      {
    //        //          InputPort in = inputPorts.get( i );
    //        //          Node node = addInOutNode( in.name(), NodeType.INPUT_PORT.name(), i, anNode, true);
    //        //          wdlToBioUML.put( in, node );
    //        //      }
    //        //
    //        //      for( int i = 0; i < outputPorts.size(); i++ )
    //        //      {
    //        //          OutputPort out = outputPorts.get( i );
    //        //          Node node = addInOutNode( out.internalName(), NodeType.OUTPUT_PORT.name(), i, anNode, false);
    //        //          wdlToBioUML.put( out, node );
    //        //      }
    //        //      anNode.setNotificationEnabled(true);
    //        //      
    //        //      wdlToBioUML.put( wdlNode, anNode );
    //        return anNode;
    //    }

    private static void layout(Diagram diagram)
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
