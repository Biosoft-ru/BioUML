package biouml.plugins.sbml.converters;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.util.EModelHelper;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnCompositeDiagramType;
import biouml.plugins.sbgn.SbgnSemanticController;
import biouml.plugins.sbml.SbmlDiagramTransformer;
import biouml.plugins.sbml.composite.SbmlCompositeSemanticController;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Specie;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.ContactConnectionPort;
import biouml.standard.type.Stub.InputConnectionPort;
import biouml.standard.type.Stub.OutputConnectionPort;
import one.util.streamex.StreamEx;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

public class SBGNCompositeConverter extends SBGNConverterNew
{

    @Override
    protected Object getType()
    {
        return SbgnCompositeDiagramType.class;
    }

    @Override
    protected void postProcess(Diagram diagram, Diagram oldDiagram)
    {
        diagram.getViewOptions().setAutoLayout( true );
        diagram.getAttributes().add(
                new DynamicProperty( SbmlDiagramTransformer.BASE_DIAGRAM_TYPE, String.class, diagram.getType().getClass().getName() ) );
        adjustSubDiagrams( diagram, oldDiagram );
        DiagramUtility.compositeModelPostprocess( diagram );    
    }

    @Override
    protected void postProcessRestore(Diagram sbgnDiagram, Diagram sbmlDiagram) throws Exception
    {
        adjustSubDiagrams( sbgnDiagram, sbmlDiagram );
        DiagramUtility.compositeModelPostprocess( sbmlDiagram );
        createConnections(sbgnDiagram, sbmlDiagram);
    }

    @Override
    protected void createElement(DiagramElement de, Compartment compartment, String name) throws Exception
    {
        DiagramElement newDe = null;
        if( de instanceof SubDiagram )
            newDe = copySubDiagram( compartment, (SubDiagram)de, SBGNConverterNew.convert( ( (SubDiagram)de ).getDiagram() ) );
        else if( de instanceof ModelDefinition )
            newDe = copyModelDefinition( compartment, (ModelDefinition)de, SBGNConverterNew.convert( ( (ModelDefinition)de ).getDiagram() ) );
        else if( de instanceof Node && Util.isPort( de ) )
            newDe = copyPort( compartment, (Node)de );

        if( newDe != null )
            SbgnSemanticController.setNeccessaryAttributes(newDe);
        else
            super.createElement(de, compartment, name);
    }

    @Override
    protected void restoreElements(Compartment sbgnCompartment, Compartment compartment) throws Exception
    {
        for( Node node : sbgnCompartment.stream( Node.class ) )
        {
            if( node instanceof SubDiagram )
            {
                SubDiagram oldSubDiagram = (SubDiagram)node;
                Diagram innerDiagram = oldSubDiagram.getDiagram();
                Diagram innerSBMLDiagram = SBGNConverterNew.restore( innerDiagram );
                innerSBMLDiagram.getAttributes().add(
                        new DynamicProperty( SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME, Diagram.class, innerDiagram ) );
                copySubDiagram( compartment, oldSubDiagram, innerSBMLDiagram );

            }
            else if( node instanceof ModelDefinition )
            {
                ModelDefinition oldModelDefinition = (ModelDefinition)node;
                Diagram innerDiagram = oldModelDefinition.getDiagram();
                Diagram innerSBMLDiagram = SBGNConverterNew.restore( innerDiagram );
                innerSBMLDiagram.getAttributes().add(
                        new DynamicProperty( SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME, Diagram.class, innerDiagram ) );
                copyModelDefinition( compartment, oldModelDefinition, innerSBMLDiagram );
            }
            else if( Util.isPort( node ) )
            {
                copyPort( compartment, node );
            }
        }
        super.restoreElements( sbgnCompartment, compartment );
    }

    @Override
    protected void restoreEdges(Compartment sbgnCompartment, Compartment compartment, Diagram sbmlDiagram) throws Exception
    {
        super.restoreEdges( sbgnCompartment, compartment, sbmlDiagram );
    }

    @Override
    protected void createEdges(Compartment baseCompartment, Compartment compartment, Diagram sbgnDiagram) throws Exception
    {
        super.createEdges( baseCompartment, compartment, sbgnDiagram );
        createConnections( baseCompartment, compartment, sbgnDiagram );
    }

    protected Node copyPort(Compartment compartment, Node node)
    {
        Node portNode = new Node( compartment, node.getName(), node.getKernel() );
        copyAttribute( node, portNode, "portType" );
        copyAttribute( node, portNode, "variableName" );
        copyAttribute( node, portNode, "orientation" );
        copyAttribute( node, portNode, "accessType" );
        if( node.getParent() instanceof SubDiagram )
            copyAttribute( node, portNode, "originalPort" );
        portNode.setLocation( node.getLocation() );
        compartment.put( portNode );
        return portNode;
    }

    protected ModelDefinition copyModelDefinition(Compartment compartment, ModelDefinition oldModelDefinition, Diagram diagram)
            throws Exception
    {
        ModelDefinition newModelDefinition = new ModelDefinition( compartment, diagram, oldModelDefinition.getName() );
        newModelDefinition.setLocation( oldModelDefinition.getLocation() );
        newModelDefinition.setShapeSize( oldModelDefinition.getShapeSize() );
        newModelDefinition.setTitle( oldModelDefinition.getTitle() );
        newModelDefinition.setVisible(oldModelDefinition.isVisible());
        compartment.put( newModelDefinition );
        return newModelDefinition;
    }

    protected SubDiagram copySubDiagram(Compartment compartment, SubDiagram oldSubDiagram, Diagram diagram) throws Exception
    {
        SubDiagram newSubDiagram = new SubDiagram( compartment, diagram, oldSubDiagram.getName() );
        newSubDiagram.setLocation( oldSubDiagram.getLocation() );
        newSubDiagram.setShapeSize( oldSubDiagram.getShapeSize() );
        newSubDiagram.setTitle( oldSubDiagram.getTitle() );
        copyAttribute( oldSubDiagram, newSubDiagram, Util.TIME_SCALE );
        copyAttribute( oldSubDiagram, newSubDiagram, Util.EXTENT_FACTOR );
        newSubDiagram.setVisible(oldSubDiagram.isVisible());
        createElements( oldSubDiagram, newSubDiagram );
        compartment.put( newSubDiagram );
        return newSubDiagram;
    }
    
    protected void createConnections(Compartment compartmentFrom, Compartment compartmentTo, Diagram diagramTo) throws Exception
    {
        for( Edge oldEdge : compartmentFrom.stream( Edge.class ).filter( Util::isConnection ) )
        {
            Node newInput = diagramTo.findNode( oldEdge.getInput().getCompleteNameInDiagram() );
            Node newOutput = diagramTo.findNode( oldEdge.getOutput().getCompleteNameInDiagram() );
            if( newInput == null || newOutput == null )
                continue;
            Edge newEdge = new Edge( compartmentTo, oldEdge.getKernel(), newInput, newOutput );
            if( oldEdge.getRole() != null )
                newEdge.setRole( oldEdge.getRole( Role.class ).clone( newEdge ) );
            copyAttribute( oldEdge, newEdge, "conversionFactor" );
            newEdge.save();
        }
    }
    
    protected void createConnections(Diagram sbgnDiagram, Diagram sbmlDiagram) throws Exception
    {
        Map<Bus, List<Node>> clusters = DiagramUtility.getBuses(sbgnDiagram).groupingBy(n->(Bus)n.getRole());
        
        for( Entry<Bus, List<Node>> cluster : clusters.entrySet() )
        {
            List<Node> nodes = cluster.getValue();

            //all ports connected to buses from cluster
            Set<Node> ports = StreamEx.of( nodes ).flatMap( n -> n.edges().map( e -> e.getOtherEnd( n ) ) ).filter( n -> Util.isPort( n ) )
                    .toSet();

            //top level ports. If undirected maximum one such port can be, otherwise 0-2 ports can be
            List<Node> topPorts = StreamEx.of( ports ).filter( n -> n.getCompartment() instanceof Diagram ).toList();

            ports.removeAll( topPorts );
            
            if( ports.size() == 0 ) //buses are not connected to anything 
                continue;
            
            Node examplePort = ports.iterator().next() ;
            boolean undirected = Util.isContactPort(  examplePort );
            boolean species = isPortSpecies(examplePort);
            
            if( undirected )
            {
                Node mainPort = this.findMainPort( topPorts );                           
                                    
                Node topPort = null;
                if( topPorts.size() == 0 ) 
                {
                    String name = createVariable( sbmlDiagram, species, cluster.getKey().getName() );
                    topPort = createPort( sbmlDiagram, ContactConnectionPort.class, name  ); 
                }
                else
                    topPort = topPorts.get( 0 );

                for( Node port : ports )
                {
                    Node sbmlPort = sbmlDiagram.findNode( port.getCompleteNameInDiagram() );
                    Node sbmlTop = sbmlDiagram.findNode( topPort.getCompleteNameInDiagram() );
                    Edge e = createConnection( sbmlPort, sbmlTop, sbmlDiagram,  "Connection_" + sbmlPort.getName(), true );

                    if( port.equals( mainPort ) )
                        e.getRole( UndirectedConnection.class ).setMainVariableType( MainVariableType.INPUT );
                    else
                        e.getRole( UndirectedConnection.class ).setMainVariableType( MainVariableType.OUTPUT );
                }
            }
            else
            {
                if( topPorts.size() == 1 ) // everything goes from top level to modules
                {
                    Node topPort = topPorts.get( 0 );
                    for( Node port : ports)
                    {
                        Node sbmlPort = sbmlDiagram.findNode( port.getCompleteNameInDiagram() );
                        Node sbmlTop = sbmlDiagram.findNode( topPort.getCompleteNameInDiagram() );
                        createConnection( sbmlTop, sbmlPort, sbmlDiagram,  "Connection_" + port.getName(), false );
                    }
                }
                else if( topPorts.size() == 0 ) //direct connection through bus
                {
                    Node mainPort = StreamEx.of( ports ).filter( n -> Util.isOutputPort( n ) ).findAny().orElse( null );
                    if( mainPort == null )
                        return;//there is no source for this signal, we should ignore it

                    String name = createVariable( sbmlDiagram, species, cluster.getKey().getName() );
                    Node topOutput = createPort( sbmlDiagram, OutputConnectionPort.class, name  );                   
                    Node topInput = createPort( sbmlDiagram, InputConnectionPort.class, name  );
                  
                    ports.remove( mainPort );
                    Node sbmlMainPort = sbmlDiagram.findNode( mainPort.getCompleteNameInDiagram() );
                    createConnection( sbmlMainPort, topInput, sbmlDiagram, "Connection_" + mainPort.getName(), false );
                    
                    for( Node port : ports)    
                    {
                        Node sbmlPort = sbmlDiagram.findNode( port.getCompleteNameInDiagram() );
                        createConnection( topOutput, sbmlPort, sbmlDiagram, "Connection_" + mainPort.getName(), false );       
                    }
                }
            }
            //we don't need to do anything if there are two ports already
        }
        
        for( Edge oldEdge : sbgnDiagram.recursiveStream().select( Edge.class ).filter( Util::isConnection ) )
        {
            if (Util.isSubDiagram( oldEdge.getInput().getCompartment() ) && Util.isSubDiagram( oldEdge.getOutput().getCompartment() ))
                processDirectConnection(oldEdge, sbmlDiagram);   
            else if (Util.isPort( oldEdge.getInput() ) || Util.isPort( oldEdge.getOutput() ))
            {
                Node newInput = sbmlDiagram.findNode( oldEdge.getInput().getCompleteNameInDiagram() );
                Node newOutput = sbmlDiagram.findNode( oldEdge.getOutput().getCompleteNameInDiagram() );
                createConnection( newInput, newOutput, sbmlDiagram, oldEdge.getName(), oldEdge.getRole() instanceof UndirectedConnection );
            }
        }
    }
    
    private Node findMainPort(List<Node> ports)
    {
        for( Node port : ports )
        {
            for( Edge e : port.edges() )
            {
                if( e.getRole() instanceof UndirectedConnection )
                {
                    UndirectedConnection connection = e.getRole( UndirectedConnection.class );
                    MainVariableType type = connection.getMainVariableType();

                    if( type.equals( MainVariableType.INPUT ) )
                        continue;

                    Node mainNode = type.equals( MainVariableType.INPUT ) ? e.getInput() : e.getOutput();
                    if( mainNode.equals( port ) )                                                  
                        return port;                    
                }
            }
        }
            return null;
    }
    
    protected void processDirectConnection(Edge oldEdge, Diagram diagramTo) throws Exception
    {
        Node oldInput = oldEdge.getInput();
        Node oldOutput = oldEdge.getOutput();
        
        boolean species = isPortSpecies(oldInput);
        
        Base oldKernel = oldEdge.getKernel();
        
        boolean undirected = oldKernel instanceof UndirectedConnection;
        Node topLevelPort1 = null;
        Node topLevelPort2 = null;

        Node newOutput = diagramTo.findNode( oldOutput.getCompleteNameInDiagram() );
        Node newInput = diagramTo.findNode( oldInput.getCompleteNameInDiagram() );
        
        String name = createVariable( diagramTo, species,  Util.getPortVariable( newOutput ) );
        
        if( undirected )
        {            
            topLevelPort1 = createPort( diagramTo, OutputConnectionPort.class, name  ); 
            topLevelPort2 = topLevelPort1;
        }
        else
        {
            topLevelPort1 = createPort( diagramTo, InputConnectionPort.class, name  );
            topLevelPort2 = createPort( diagramTo, OutputConnectionPort.class, name  ); 
        }

        createConnection( newInput, topLevelPort1, diagramTo, oldEdge.getName()+"1", undirected );
        createConnection( topLevelPort2, newOutput, diagramTo, oldEdge.getName()+"2", undirected );
    }   
    
    private boolean isPortSpecies(Node port) throws Exception
    {
        Variable var = getVariable(port);
        return var instanceof VariableRole;
    } 
    
    private Variable getVariable(Node port) throws Exception
    {
        if (port.getParent() instanceof SubDiagram)
        {
            SubDiagram subdiagram = (SubDiagram)port.getCompartment();
            String originalName = port.getAttributes().getValueAsString( "originalPort" );
            Node originalPort = (Node)subdiagram.getDiagram().findNode( originalName);        
            return getVariable(originalPort);
        }
        else 
        {
           
            if (Util.isPropagatedPort2( port ))
            {
                Node basePort = Util.getBasePort( port );
                return getVariable(basePort);
            }
            else
            {
                return Diagram.getDiagram( port ).getRole( EModel.class ).getVariable(Util.getPortVariable( port ));
            }
        }
        
    }
    
    
    private Edge createConnection(Node from, Node to, Diagram d, String name, boolean undirected) throws Exception
    {        
        name = DefaultSemanticController.generateUniqueName( d, name );
        Base kernel = null;
        Connection role = null;
        if( undirected )
            kernel = new Stub.UndirectedConnection( null, name );
        else
            kernel = new Stub.DirectedConnection( null, name );

        Edge edge = new Edge( kernel, from, to );

        if( undirected )
            role = new UndirectedConnection( edge );
        else
            role = new DirectedConnection( edge );

        edge.setRole( role );
        
        role.setOutputPort( new Connection.Port( from ) );
        role.setInputPort( new Connection.Port( to ) );
        
        edge.save();
        return edge;
    }

    /**
     * If diagram referenced by subdiagram refers to modeldefinition
     * Then its converted version should refer to converted modelDefinition
     * @param diagram
     */
    protected void adjustSubDiagrams(Diagram diagramFrom, Diagram diagramTo)
    {
        for( SubDiagram subDiagram : diagramTo.stream( SubDiagram.class ) )
        {
            Diagram innerDiagram = subDiagram.getDiagram();
            try
            {
                Option parent = innerDiagram.getParent();
                if( parent != null && parent instanceof ModelDefinition
                        && Diagram.getDiagram( ( (ModelDefinition)parent ) ).equals( diagramFrom ) )
                {
                    Node newModelDefinition = diagramTo.findNode( ( (ModelDefinition)parent ).getName() );
                    if( newModelDefinition != null && newModelDefinition instanceof ModelDefinition )
                        innerDiagram.setOrigin( (ModelDefinition)newModelDefinition );
                }
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE,  "Can not adjust parent for diagram referenced by subdiagram" + subDiagram.getName() );
            }
        }
    }
    
    public static String createVariable(Diagram d, boolean species, String varName)
    {
        SbmlCompositeSemanticController controller = (SbmlCompositeSemanticController)d.getType().getSemanticController();
        String name = varName;
        if( species )
        {
            name = DefaultSemanticController.generateUniqueName( d, varName );
            name = name.replace( ".", "_" ).replace( "$", "" );
            DiagramElementGroup deg = controller.createInstance( d, Specie.class, name, new Point(), null );
            Node specieNode = (Node)deg.get( 0 );
            
            boolean notification = d.isNotificationEnabled();
            d.setNotificationEnabled( true );           
            
            d.put( specieNode ); //check maybe put in createInstance?
            d.setNotificationEnabled( notification );            
            VariableRole role = specieNode.getRole( VariableRole.class );
            name = role.getName();
        }
        else
        {
            name = EModelHelper.generateUniqueVariableName( d.getRole( EModel.class ), varName );
            d.getRole( EModel.class ).declareVariable( name, 0.0 );
        }
        return name;
    }
    
    /**
    * Creates top level species or parameter and connects it to connection chain
    */
    public static Node createPort(Diagram d, Class<? extends Base> portClass, String name)
    {
        SbmlCompositeSemanticController controller = (SbmlCompositeSemanticController)d.getType().getSemanticController();
        PortProperties properties = new PortProperties( d, portClass );
        properties.setAccessType( ConnectionPort.PRIVATE );
        properties.setVarName( name );
        DiagramElementGroup portDeg = controller.createInstance( d, ConnectionPort.class, new Point(), properties );
        for(DiagramElement de: portDeg.nodesStream())
            d.put( de );
        for (DiagramElement e: portDeg.edgesStream())
            d.put( e );
        return portDeg.nodesStream().findAny().orElse( null );
    }
}
