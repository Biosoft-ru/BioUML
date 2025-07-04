package biouml.plugins.wdl.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;

/**
 * Semantic controller for workflow diagrams
 */
public class WDLSemanticController extends DefaultSemanticController
{
    @Override
    public DiagramElementGroup createInstance(Compartment compartment, Object type, Point point, Object properties)
    {
        DiagramElement de = null;
        //        NodeType nodeType = (NodeType)type;
        //        switch(nodeType)
        //        {
        //            case COMMAND_CALL:
        //                //de = createCommandCallNode(compartment, properties);
        //                break;
        //            case WORKFLOW_CALL:
        //                break;
        //            case INPUT:
        //                break;
        //            case OUTPUT:
        //                break;
        //            case SCATTER:
        //                break;
        //            case CONDITIONAL:
        //                break;
        //        }
        if( de == null )
            return DiagramElementGroup.EMPTY_EG;
        return new DiagramElementGroup( de );
    }

    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        Dimension result = super.move( de, newParent, offset, oldBounds );
        if( WDLConstants.INPUT_TYPE.equals( de.getKernel().getType() ) || WDLConstants.OUTPUT_TYPE.equals( de.getKernel().getType() )
                && WDLConstants.TASK_TYPE.equals( de.getCompartment().getKernel().getType() ) )
            movePort( (Node)de );
        
        return result;
    }
    
    public static void movePort(Node node)
    {
        boolean input = WDLConstants.INPUT_TYPE.equals( node.getKernel().getType() );
        Compartment parent = node.getCompartment();
        Point p = node.getLocation();
        if( input )
            p.x = parent.getLocation().x;
        else
            p.x = parent.getLocation().x + parent.getView().getBounds().width - node.getView().getBounds().width;
        node.setLocation( p );
    }


    //    static <T> List<T> toJavaList(scala.collection.immutable.Iterable<T> scalaCollection)
    //    {
    //        List<T> result = new ArrayList<>();
    //        Iterator<T> it = scalaCollection.iterator();
    //        while(it.hasNext())
    //            result.add( it.next() );
    //        return result;
    //    }

    //    public Compartment createScatterNode(Compartment parent, ScatterNode wdlNode,  Map<Object, Node> wdlToBioUML)
    //    {
    //        String name = fixName(wdlNode.localName());
    //        name = uniqName( parent, name );
    //        Stub kernel = new Stub(null, name, NodeType.SCATTER.name());
    //        
    //        Compartment anNode = new Compartment(parent, name, kernel);
    //        
    //        anNode.setTitle(wdlNode.fullyQualifiedName());
    //        anNode.setNotificationEnabled(false);
    //        
    //        /*
    //        List<InputPort> inputPorts = toJavaList( wdlNode.inputPorts() );
    //        List<OutputPort> outputPorts = toJavaList( wdlNode.outputPorts() );
    //        
    //        anNode.setShapeSize(new Dimension(600, 300));
    //        anNode.getAttributes().add(new DynamicProperty("innerNodesPortFinder", Boolean.class, true));
    //        for( int i = 0; i < inputPorts.size(); i++ )
    //        {
    //            InputPort in = inputPorts.get( i );
    //            Node node = addInOutNode( in.name(), NodeType.INPUT_PORT.name(), i, anNode, true);
    //            wdlToBioUML.put( in, node );
    //        }
    //
    //        for( int i = 0; i < outputPorts.size(); i++ )
    //        {
    //            OutputPort out = outputPorts.get( i );
    //            Node node = addInOutNode( out.internalName(), NodeType.OUTPUT_PORT.name(), i, anNode, false);
    //            wdlToBioUML.put( out, node );
    //        }
    //        */
    //        anNode.setNotificationEnabled(true);
    //        
    //        Graph innerGraph = wdlNode.innerGraph();
    //        handleGraph( innerGraph, anNode, wdlToBioUML );
    //        
    //        wdlToBioUML.put( wdlNode, anNode );
    //        return anNode;
    //    }


    public static String uniqName(Compartment parent, String name)
    {
        return DefaultSemanticController.generateUniqueName( parent, name );
//        if( !parent.contains( name ) )
//            return name;
//        String base = name;
//        int i = 1;
//        while( parent.contains( name ) )
//            name = base + "_" + ( i++ );
//        return name;
    }
    //
    //    public Compartment createCallNode(Compartment parent, CallNode wdlNode,  Map<Object, Node> wdlToBioUML)
    //    {
    //        String name = fixName(wdlNode.localName());
    //        name = uniqName( parent, name );
    //        Stub kernel = new Stub(null, name, NodeType.COMMAND_CALL.name());
    //        
    //        Compartment anNode = new Compartment(parent, name, kernel);
    //        
    //        anNode.setTitle(wdlNode.fullyQualifiedName());
    //        anNode.setNotificationEnabled(false);
    //        
    //        List<InputPort> inputPorts = toJavaList( wdlNode.inputPorts() );
    //        List<OutputPort> outputPorts = toJavaList( wdlNode.outputPorts() );
    //        
    //        int maxPorts = Math.max(inputPorts.size(), outputPorts.size());
    //        int height = Math.max(50, 24 * maxPorts + 8);
    //        anNode.setShapeSize(new Dimension(200, height));
    //        anNode.getAttributes().add(new DynamicProperty("innerNodesPortFinder", Boolean.class, true));
    //        for( int i = 0; i < inputPorts.size(); i++ )
    //        {
    //            InputPort in = inputPorts.get( i );
    //            Node node = addInOutNode( in.name(), NodeType.INPUT_PORT.name(), i, anNode, true);
    //            wdlToBioUML.put( in, node );
    //        }
    //
    //        for( int i = 0; i < outputPorts.size(); i++ )
    //        {
    //            OutputPort out = outputPorts.get( i );
    //            Node node = addInOutNode( out.internalName(), NodeType.OUTPUT_PORT.name(), i, anNode, false);
    //            wdlToBioUML.put( out, node );
    //        }
    //        anNode.setNotificationEnabled(true);
    //        
    //        wdlToBioUML.put( wdlNode, anNode );
    //        return anNode;
    //    }
    //
    //    private Node addInOutNode(String name, String nodeType, int position, Compartment parent, boolean isInput) throws DataElementPutException
    //    {
    //        name = fixName(name);
    //        name = uniqName( parent, name );
    //        Node inNode = new Node( parent, new Stub( parent, name , nodeType ) );
    //        inNode.getAttributes().add( new DynamicProperty( "position", Integer.class, position ) );
    //        inNode.setFixed( true );
    //
    //        Point parentLoc = parent.getLocation();
    //        Dimension parentDim = parent.getShapeSize();
    //        if(isInput)
    //        {
    //            inNode.setLocation( parentLoc.x + 2, parentLoc.y + position*24 + 8 );
    //        }else
    //        {
    //            inNode.setLocation( parentLoc.x + parentDim.width - 16 - 2, parentLoc.y + position*24 + 8 );
    //        }
    //
    //        //TODO: icon depending on womType
    //        //String iconId = DataElementPathEditor.getIconId( property );
    //        //inNode.getAttributes().add( new DynamicProperty( "iconId", String.class, iconId ) );
    //
    //        parent.put( inNode );
    //        return inNode;
    //    }
    //    
    //    public Node createInput(Compartment parent, GraphInputNode wdlNode)
    //    {
    //        String name = fixName(wdlNode.localName());
    //        name = uniqName( parent, name );
    //        //TODO: save womType into node to show differently
    //        WomType womType = wdlNode.womType();
    //        return new Node(parent, new Stub(null, name, NodeType.INPUT.name()));
    //    }
    //    
    //    public Node createOutput(Compartment parent, GraphOutputNode wdlNode)
    //    {
    //        String name = fixName(wdlNode.localName());
    //        name = uniqName( parent, name );
    //        //TODO: save womType into node to show differently
    //        WomType womType = wdlNode.womType();
    //        return new Node(parent, new Stub(null, name, NodeType.OUTPUT.name()));
    //    }
    //    
    //    public Node createExpression(Compartment parent, ExpressionNode wdlNode)
    //    {
    //        String name = fixName(wdlNode.localName());
    //        name = uniqName( parent, name );
    //        //TODO: save womType into node to show differently
    //        WomType womType = wdlNode.womType();
    //        return new Node(parent, new Stub(null, name, NodeType.EXPRESSION.name()));
    //    }

    @Override
    public Edge createEdge(Node fromNode, Node toNode, String edgeType, Compartment compartment)
    {
        String name = uniqName( compartment, Base.TYPE_DIRECTED_LINK );
        Stub edgeStub = new Stub( null, name, Base.TYPE_DIRECTED_LINK );
        return new Edge( edgeStub, fromNode, toNode );
    }

    //    public static String fixName(String name)
    //    {
    //        return name.replace( '/', ':' ).replace( '.', '>' );
    //    }
    //    
    //    private Node handleDefault(GraphNode wdlNode, Compartment parent, Map<Object, Node> wdlToBiouml)
    //    {
    //        String name = fixName(wdlNode.localName());
    //        name = uniqName( parent, name );
    //        Node bioumlNode=  new Node(parent, new Stub(null, name, NodeType.DEFAULT.name()));
    //        parent.put( bioumlNode );
    //        wdlToBiouml.put( wdlNode, bioumlNode );
    //        return bioumlNode;
    //    }
    //
    //    private Node handleInput(GraphInputNode wdlNode, Compartment diagram)
    //    {
    //        Node de = createInput( diagram, wdlNode );
    //        diagram.put(de);
    //        return de;
    //    }
    //    
    //    private Node handleOutput(GraphOutputNode wdlNode, Compartment diagram)
    //    {
    //        Node de = createOutput( diagram, wdlNode );
    //        diagram.put(de);
    //        return de;
    //    }
    //    
    //    private Node handleExpression(ExpressionNode wdlNode, Compartment diagram)
    //    {
    //        Node de = createExpression( diagram, wdlNode );
    //        diagram.put(de);
    //        return de;
    //    }
    //
    //    private Node handleConditional(ConditionalNode c, Compartment diagram)
    //    {
    //        // TODO Auto-generated method stub
    //        return null;
    //    }
    //
    //    private Node handleScatter(ScatterNode s, Compartment diagram, Map<Object, Node> wdlToBioUML)
    //    {
    //        Compartment de = createScatterNode( diagram, s, wdlToBioUML );
    //        diagram.put(de);
    //        return de;
    //    }
    //
    //    private Compartment handleCall(CallNode cn, Compartment diagram, Map<Object, Node> wdlToBioUML)
    //    {
    //        Compartment de = createCallNode( diagram, cn, wdlToBioUML );
    //        diagram.put(de);
    //        return de;
    //    }

    //    public void handleGraph(Graph graph, Compartment diagram, Map<Object, Node> wdlToBiouml)
    //    {
    //
    //        Iterator<GraphNode> it = graph.nodes().iterator();
    //        
    //        while(it.hasNext())
    //        {
    //            GraphNode wdlNode = it.next();
    //            if(wdlNode instanceof CallNode)
    //            {
    //                CallNode ccn = (CallNode) wdlNode;
    //                handleCall(ccn, diagram, wdlToBiouml);
    //            } else if(wdlNode instanceof ScatterNode)
    //            {
    //                ScatterNode s = (ScatterNode) wdlNode;
    //                handleScatter(s, diagram, wdlToBiouml);
    //                //handleDefault( wdlNode, diagram, wdlToBiouml );
    //            } else if(wdlNode instanceof ConditionalNode)
    //            {
    //                ConditionalNode c = (ConditionalNode) wdlNode;
    //                //handleConditional(c, diagram);
    //                handleDefault( wdlNode, diagram, wdlToBiouml );
    //            } else if(wdlNode instanceof GraphInputNode)
    //            {
    //                //GraphInputNode
    //                //-ExternalGraphInputNode
    //                //--OptionalGraphInputNode
    //                //--RequiredGraphInputNode
    //                //--OptionalGraphInputNodeWithDefault
    //                //-OuterGraphInputNode
    //                //--ScatterVariableNode
    //                Node node = handleInput((GraphInputNode)wdlNode, diagram);
    //                wdlToBiouml.put( wdlNode, node );
    //            } else if(wdlNode instanceof GraphOutputNode)
    //            {
    //                //Either PortBasedGraphOutputNode or ExpressionBasedGraphOutputNode
    //                Node node = handleOutput((GraphOutputNode)wdlNode, diagram);
    //                wdlToBiouml.put( wdlNode, node );
    //            } else if(wdlNode instanceof ExpressionNode)
    //            {
    //                Node node = handleExpression((ExpressionNode)wdlNode, diagram);
    //                wdlToBiouml.put( wdlNode, node );
    //            }
    //            else
    //            {
    //                //log.warning( "Ignoring WDL node of class " + wdlNode.getClass() );
    //                handleDefault(wdlNode, diagram, wdlToBiouml);
    //            }
    //            //
    //            
    //            
    //        }
    //        
    //        it = graph.nodes().iterator();
    //        while(it.hasNext())
    //        {
    //            GraphNode wdlNode = it.next();
    //            Iterator<InputPort> itPorts = wdlNode.inputPorts().iterator();
    //            while(itPorts.hasNext())
    //            {
    //                InputPort inPort = itPorts.next();
    //                OutputPort upstreamPort = inPort.upstream();
    //                
    //                Node inBioUMLNode = wdlToBiouml.get( inPort );
    //                if(inBioUMLNode == null)
    //                    inBioUMLNode = wdlToBiouml.get( inPort.graphNode() );
    //                if(inBioUMLNode == null)
    //                {
    //                    log.severe( "Can not find node for " + inPort );
    //                    continue;
    //                }
    //                
    //                Node outBioUMLNode = wdlToBiouml.get( upstreamPort );
    //                if(outBioUMLNode == null)
    //                    outBioUMLNode = wdlToBiouml.get( upstreamPort.graphNode() );
    //                if(outBioUMLNode == null)
    //                {
    //                    log.severe( "Can not find node for " + upstreamPort );
    //                    continue;
    //                }
    //
    //                Edge e = createEdge(outBioUMLNode, inBioUMLNode, Base.TYPE_DIRECTED_LINK, diagram );
    //                diagram.put( e );
    //            }
    //        }
    //    }


}
