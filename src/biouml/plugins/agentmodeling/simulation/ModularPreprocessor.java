package biouml.plugins.agentmodeling.simulation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.Connection.Port;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine2;
import biouml.plugins.agentmodeling.AgentSimulationEngineWrapper;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.Util;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub.ConnectionPort;

import java.util.Map.Entry;

import one.util.streamex.StreamEx;

public class ModularPreprocessor
{
    private SimulationEngine engine;
    private Diagram diagram;

    public Diagram getDiagram()
    {
        return diagram;
    }

    public SimulationEngine getSimulationEngine()
    {
        return engine;
    }

    /**
     * Method merges all subdiagrams with JavaSimulationEngines into one submodel
     * @param diagram
     * @param engines
     * @return
     * @throws Exception
     */
    public void flatSimilarSubdiagrams(Diagram diagram, ModuleGroup[] moduleGroups) throws Exception
    {
        //Simplest case - agent simulation is not needed
        //TODO: also check that there are not averagers!
        if( moduleGroups.length == 1 )
        {
            engine = moduleGroups[0].getEngine();
            this.diagram = diagram;
            return;
        }

        this.diagram = diagram.clone( null, diagram.getName() );

        engine = new AgentModelSimulationEngine2();

        for( ModuleGroup moduleGroup : moduleGroups )
        {
            String[] subDiagramNames = moduleGroup.getSubdiagrams();

            SubDiagram[] subDiagrams = StreamEx.of( subDiagramNames ).without( ModuleGroup.TOP_LEVEL )
                    .map( s -> (SubDiagram)this.diagram.findNode( s ) ).toArray( SubDiagram[]::new );

            boolean includeTopLevel = StreamEx.of( subDiagramNames ).toSet().contains( ModuleGroup.TOP_LEVEL );

            SubDiagram newSubDiagram;
            if( subDiagrams.length != 1 )
            {
                newSubDiagram = flatSubModels( moduleGroup.getName(), subDiagrams, this.diagram );
                this.diagram.put( newSubDiagram );
            }
            else
            {
                newSubDiagram = subDiagrams[0];
            }
            //Creating correspondent simualtion engine
            SimulationEngine moduleEngine = moduleGroup.getEngine();
            AgentSimulationEngineWrapper engineWrapper = new AgentSimulationEngineWrapper();
            engineWrapper.setEngine( moduleEngine );
            engineWrapper.setSubDiagram( newSubDiagram );
            ( (AgentModelSimulationEngine2)engine ).addEngine( engineWrapper );
        }
    }

    private SubDiagram flatSubModels(String name, SubDiagram[] subDiagrams, Diagram diagram) throws Exception
    {
        Set<Node> buses = DiagramUtility.getBuses( diagram ).toSet();
        Set<Node> nodesToAdd = StreamEx.of( buses ).append( subDiagrams ).toSet();

        Set<Edge> connectionEdges = diagram.stream( Edge.class ).filter( e -> Util.isConnection( e ) ).toSet();
        Set<Edge> innerConnections = StreamEx.of( connectionEdges )
                .filter( e -> contains( nodesToAdd, e.getInput() ) && contains( nodesToAdd, e.getOutput() ) ).toSet();

        Set<Edge> outerConnections = StreamEx.of( connectionEdges ).filter( e -> !innerConnections.contains( e ) ).toSet();

        //intermediate composite diagram which will be flattened
        Diagram newDiagram = diagram.getType().createDiagram( null, name, new DiagramInfo( name ) );

        //put all selected subdiagrams into it (and remove from original one) 
        for( SubDiagram subDiagram : subDiagrams )
        {
            subDiagram.setOrigin( newDiagram );
            newDiagram.put( subDiagram );
            diagram.remove( subDiagram.getCompleteNameInDiagram() );
        }

        //move all connections between selected elements to new intermediate composite diagram
        for( Edge e : innerConnections )
        {
            diagram.remove( e.getCompleteNameInDiagram() );
            newDiagram.put( e );
            e.setOrigin( newDiagram );
        }

        HashMap<Edge, String> connectionToNewNode = new HashMap<>();

        //All connections leading from elements which will be flatten to other are redirected to propagated ports or new subDiagram
        for( Edge e : outerConnections )
        {
            Node oldNode = contains( nodesToAdd, e.getInput() ) ? e.getInput() : e.getOutput();
            if( Util.isPort( oldNode ) )
            {
                String portName = DefaultSemanticController.generateUniqueNodeName( newDiagram, oldNode.getName() + "_propagated" );
                connectionToNewNode.put( e, portName );
                Node propagatedPort = oldNode.clone( newDiagram, portName );
                Util.setPropagated( propagatedPort, oldNode );
                newDiagram.put( propagatedPort );
                DiagramUtility.createPropagatedPortEdge( propagatedPort );
            }
            else if( Util.isBus( oldNode ) )
            {
                String portName = DefaultSemanticController.generateUniqueNodeName( newDiagram, oldNode.getName() + "_public" );
                connectionToNewNode.put( e, portName );
                VariableRole varRole = oldNode.getRole( VariableRole.class );
                PortProperties properties = new PortProperties( newDiagram, "contact" );
                properties.setVarName( varRole.getName() );
                properties.setName( portName );
                properties.setAccessType( ConnectionPort.PUBLIC );
                SemanticController controller = newDiagram.getType().getSemanticController();
                controller.createInstance( newDiagram, ConnectionPort.class, new Point(), properties );
            }
        }
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();

        //flattened diagram encapsulated into result subdiagram
        Diagram flatDiagram = preprocessor.preprocess( newDiagram, null, name );

        //here we store information about how variable paths were changed to keep mapping between initial variable path and index in result model
        String path = DiagramUtility.generatPath( diagram );
        String newPath = DiagramUtility.generatPath( newDiagram );
        Map<String, String> varPathToOriginalPath = new HashMap<>();
        for( Entry<String, String> entry : preprocessor.getVarPathToOldPath().entrySet() )
            varPathToOriginalPath.put( newPath + "\\" + entry.getKey(), path + "\\" + entry.getValue() );

        //Result subdiagram
        SubDiagram newSubDiagram = new SubDiagram( diagram, flatDiagram, newDiagram.getName() );
        diagram.put( newSubDiagram );

        //correcting outer connections after preprocessing: we need to replace variable names in connection itself
        for( Edge e : outerConnections )
        {
            boolean input = contains( nodesToAdd, e.getInput() );
            Node oldNode = input ? e.getInput() : e.getOutput();
            Node newNode = oldNode instanceof SubDiagram ? newSubDiagram : newSubDiagram.findNode( connectionToNewNode.get( e ) );
            redirectEdge( e, oldNode, newNode );
            Connection role = e.getRole( Connection.class );

            if( role instanceof MultipleConnection )
            {
                for( Connection innerConnection : ( (MultipleConnection)role ).getConnections() )
                {
                    Port port = input ? innerConnection.getInputPort() : innerConnection.getOutputPort();
                    String varName = port.getVariableName();
                    Node oldSubDiagram = oldNode instanceof SubDiagram ? oldNode
                            : oldNode.getParent() instanceof SubDiagram ? (SubDiagram)oldNode.getParent() : null;
                    String key = oldSubDiagram != null ? oldSubDiagram.getCompleteNameInDiagram() : "";
                    String newName = preprocessor.getNewVariableName( varName, key );
                    port.setVariableName( newName );
                }
            }
            Port port = input ? role.getInputPort() : role.getOutputPort();
            String varName = port.getVariableName();
            Node oldSubDiagram = oldNode instanceof SubDiagram ? oldNode
                    : oldNode.getParent() instanceof SubDiagram ? (SubDiagram)oldNode.getParent() : null;
            String key = oldSubDiagram != null ? oldSubDiagram.getCompleteNameInDiagram() : "";
            String newName = preprocessor.getNewVariableName( varName, key );
            port.setVariableName( newName );
        }
        return newSubDiagram;
    }

    /**
     * Method creates agent diagram where all elements which are at the top of input <b>diagram</b> are placed in additional subDiagram
     */
    //    private Diagram generateMainAgent(Diagram diagram, boolean processBuses) throws Exception
    //    {
    //        Diagram result = diagram.clone( null, diagram.getName() );
    //
    //        //create new subDiagram
    //        Diagram innerDiagram = diagram.clone( null, diagram.getName() );
    //        SubDiagram subdiagram = new SubDiagram( result, innerDiagram, innerDiagram.getName() );
    //        result.put( subdiagram );
    //        innerDiagram = subdiagram.getDiagram();
    //
    //        if( processBuses )
    //        {
    //            //redirect all connections to bus on the top level to new buses inside subdiagram
    //            for( Node bus : result.recursiveStream().select( Node.class ).filter( node -> Util.isBus( node ) ).toList() )
    //            {
    //                String fullName = bus.getCompleteNameInDiagram();
    //                Node newBus = (Node)innerDiagram.get( fullName );
    //                for( Edge e : bus.getEdges() )
    //                    Util.redirect( e, bus, newBus );
    //                result.remove( fullName );
    //            }
    //        }
    //
    //        Util.getPorts( innerDiagram ).filter( n -> Util.isPrivatePort( n ) ).forEach( n -> Util.setPublic( n ) );
    //        for( Node port : Util.getPorts( result ) )
    //        {
    //            Node newPort;
    //
    //            if( Util.isPublicPort( port ) )
    //            {
    //                result.remove( port.getName() );
    //                PortProperties properties = new PortProperties( result, port.getKernel().getClass() );
    //                properties.setAccessType( ConnectionPort.PROPAGATED );
    //                properties.setModuleName( subdiagram.getName() );
    //                properties.setBasePortName( port.getName() );
    //                properties.setName( port.getName() );
    //                newPort = (Node)properties.createElements( result, new Point(), null ).getElement( Util::isPort );
    //            }
    //            else
    //                newPort = (Node)subdiagram.get( port.getName() );
    //
    //            for( Edge e : port.getEdges() )
    //                Util.redirect( e, port, newPort );
    //            result.remove( port.getName() );
    //        }
    //
    //        for( AgentSimulationEngineWrapper engine : engines )
    //        {
    //            if( ! ( engine.getEngine() instanceof AgentModelSimulationEngine ) && DiagramUtility.containModules( engine.getDiagram() ) )
    //            {
    //                SubDiagram subDiagram = (SubDiagram)result.findDiagramElement( engine.de.getCompleteNameInDiagram() );
    //                new CompositeModelPreprocessor().processCompositeSubDiagram( result, subDiagram );
    //            }
    //        }
    //
    //        for( DiagramElement de : innerDiagram.recursiveStream().toSet() )
    //        {
    //            if( de instanceof DiagramContainer || Util.isPlot( de ) || Util.isSwitch( de ) || Util.isAverager( de ) || Util.isConstant( de )
    //                    || de.getKernel() != null && de.getKernel().getType().equals( "ScriptAgent" ) || Util.isConnection( de ) )
    //                innerDiagram.remove( de.getCompleteNameInDiagram() );
    //            else
    //                result.remove( de.getCompleteNameInDiagram() );
    //        }
    //
    //        processSwitches( result );
    //        return result;
    //    }

    public static boolean contains(Set<Node> nodes, Node node)
    {
        return nodes.contains( node ) || nodes.contains( node.getParent() );
    }

    public static void redirectEdge(Edge edge, Node node, Node newNode)
    {
        boolean input = node.equals( edge.getInput() );
        boolean output = node.equals( edge.getOutput() );
        if( input )
            edge.setInput( newNode );
        if( output )
            edge.setOutput( newNode );

        if( input || output )
            newNode.addEdge( edge );
    }

    private void processSwitches(Diagram compositeDiagram) throws Exception
    {
        List<Node> toRemove = new ArrayList<>();
        List<Node> toAdd = new ArrayList<>();
        for( Node node : compositeDiagram.getNodes() )
        {
            if( !Util.isSwitch( node ) || ! ( node instanceof Compartment ) )
                continue;

            SubDiagram subDiagram = CompositeModelPreprocessor.processSwitch( (Compartment)node );
            subDiagram.getAttributes().add( node.getAttributes().getProperty( Util.INITIAL_TIME ) );
            subDiagram.getAttributes().add( node.getAttributes().getProperty( Util.TIME_INCREMENT ) );
            subDiagram.getAttributes().add( node.getAttributes().getProperty( Util.COMPLETION_TIME ) );
            //            subDiagram.getAttributes().add(new DynamicProperty(ADDITIONAL, Boolean.class, true));
            toRemove.add( node );
            toAdd.add( subDiagram );
        }

        for( Node node : toRemove )
            compositeDiagram.remove( node.getName() );

        for( Node node : toAdd )
            compositeDiagram.put( node );
    }
}