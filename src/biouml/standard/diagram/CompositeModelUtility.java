package biouml.standard.diagram;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.View;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.Connection.Port;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.SpecieReference;

public class CompositeModelUtility
{
    private static HashMap<Variable, HashSet<Variable>> generateConnectedMap(Diagram compositeDiagram)
    {
        HashMap<Variable, HashSet<Variable>> result = new HashMap<>();
        for( DiagramElement de : compositeDiagram )
        {
            Role role = de.getRole();
            if( de instanceof Edge && role instanceof Connection )
            {
                if( role instanceof MultipleConnection )
                {
                    for( Connection connection : ( (MultipleConnection)role ).getConnections() )
                    {
                        readConnection(connection, result);
                    }
                }
                else
                {
                    readConnection((Connection)role, result);
                }
            }
        }
        return result;
    }


    private static void readConnection(Connection con, HashMap<Variable, HashSet<Variable>> connectionMap)
    {
        Edge edge = (Edge)con.getDiagramElement();
        Diagram inputDiagram = getDiagram(edge.getInput());
        Diagram outputDiagram = getDiagram(edge.getOutput());
        Variable inputVar = inputDiagram.getRole( EModel.class ).getVariable( con.getInputPort().getVariableName() );
        Variable outputVar = outputDiagram.getRole( EModel.class ).getVariable( con.getOutputPort().getVariableName() );

        if( inputVar instanceof VariableRole && outputVar instanceof VariableRole )
        {
            biouml.model.Node inputNode = (biouml.model.Node) ( (VariableRole)inputVar ).getDiagramElement();

            Variable oldKey = null;

            HashSet<Variable> newVariables = new HashSet<>();

            for( Map.Entry<Variable, HashSet<Variable>> entry : connectionMap.entrySet() )
            {
                HashSet<Variable> variables = entry.getValue();
                if( variables.contains(inputVar) || variables.contains(outputVar) )
                {
                    newVariables.addAll(variables);
                    oldKey = entry.getKey();
                    break;
                }
            }

            newVariables.add(inputVar);
            newVariables.add(outputVar);

            if( oldKey != null )
                connectionMap.remove(oldKey);

            //new key - try to avoid buses as keys
            Variable newKey = ( Util.isBus(inputNode) ) ? outputVar : inputVar;


            connectionMap.put(newKey, newVariables);
        }
    }
    /**
     * returns list of all connected reactions.<br>
     * (two reactions are connected if they have equal sets of species and all those species are connected with undirected connections)
     * @param compositeDiagram
     * @return
     */
    public static HashSet<HashSet<biouml.model.Node>> getConnectedReactions(Diagram compositeDiagram) throws Exception
    {
        HashSet<HashSet<biouml.model.Node>> connectedReactions = new HashSet<>();

        HashMap<Variable, HashSet<Variable>> connectionMap = generateConnectedMap(compositeDiagram);

        HashSet<Diagram> checkedDiagrams = new HashSet<>();

        HashMap<Diagram, Set<biouml.model.Node>> diagramReactions = new HashMap<>();
        for( SubDiagram subDiagram : getSubDiagrams2(compositeDiagram) )
        {
            diagramReactions.put(subDiagram.getDiagram(), getReactions(subDiagram));
        }

        for( Map.Entry<Diagram, Set<biouml.model.Node>> entry1 : diagramReactions.entrySet() )
        {
            checkedDiagrams.add(entry1.getKey());
            for( biouml.model.Node reaction1 : entry1.getValue() )
            {
                for( Map.Entry<Diagram, Set<biouml.model.Node>> entry2 : diagramReactions.entrySet() )
                {
                    if( checkedDiagrams.contains(entry2.getKey()) )
                        continue;
                    for( biouml.model.Node reaction2 : entry2.getValue() )
                    {
                        if( isConnected(reaction1, reaction2, connectionMap) )
                        {
                            HashSet<Node> reactions = StreamEx.of(connectedReactions)
                                .findFirst( set -> set.contains( reaction1 ) || set.contains( reaction2 ) )
                                .orElseGet( () -> {
                                    HashSet<biouml.model.Node> set = new HashSet<>();
                                    connectedReactions.add(set);
                                    return set;
                                });
                            reactions.add(reaction1);
                            reactions.add(reaction2);
                        }
                    }
                }
            }
        }

        return connectedReactions;
    }
    /**
     * Check whether reactions are connected vial all their species and may be considered as one reaction
     * @param reaction1
     * @param diagram1
     * @param reaction2
     * @param diagram2
     * @return
     */
    private static boolean isConnected(biouml.model.Node reactionNode1, biouml.model.Node reactionNode2,
            HashMap<Variable, HashSet<Variable>> connectionMap) throws Exception
    {
        Reaction reaction1 = (Reaction)reactionNode1.getKernel();
        Reaction reaction2 = (Reaction)reactionNode2.getKernel();

        Diagram d1 = getDiagram(reactionNode1);
        Diagram d2 = getDiagram(reactionNode2);

        SpecieReference[] refs1 = reaction1.getSpecieReferences();
        SpecieReference[] refs2 = reaction2.getSpecieReferences();

        if( refs1.length != refs2.length )
            return false;

        for( SpecieReference reference1 : refs1 )
        {
            VariableRole var1 = reference1.getSpecieVariableRole(d1);
            boolean isConnected = false;
            for( SpecieReference reference2 : refs2 )
            {
                VariableRole var2 = reference2.getSpecieVariableRole(d2);
                isConnected = reference1.getType().equals(reference2.getType()) && isConnected(var1, var2, connectionMap);
                if( isConnected )
                    break;
            }
            if( !isConnected )
                return false;
        }
        return true;
    }

    /**
     * Check connection of specie references
     * @param sr1
     * @param d1
     * @param sr2
     * @param d2
     * @return
     */
    private static boolean isConnected(Variable var1, Variable var2, HashMap<Variable, HashSet<Variable>> connectionMap)
    {
        for( HashSet<Variable> variables : connectionMap.values() )
        {
            if( variables.contains(var1) && variables.contains(var2) )
                return true;
        }
        return false;
    }

    /**
     * Returns all subdiagrams (see {@link SubDiagram}) inside composite diagram
     * @param compositeDiagram
     * @return array of subdiagrams
     */
    private static SubDiagram[] getSubDiagrams2(Diagram compositeDiagram)
    {
        return compositeDiagram.stream( SubDiagram.class ).toArray( SubDiagram[]::new );
    }

    /**
     * Return all reactions from compartment
     * @param compartment
     * @return
     */
    private static Set<biouml.model.Node> getReactions(Compartment compartment)
    {
        return compartment.recursiveStream().select( biouml.model.Node.class )
                .filter( node -> node.getKernel() instanceof Reaction ).toSet();
    }


    public static Base cloneKernel(Base kernel, DataCollection<?> newCollection, String newName)
    {
        Base result = null;
        try
        {
            result = kernel.getClass().getConstructor(DataCollection.class, String.class, String.class).newInstance(newCollection, newName,
                    kernel.getType());
        }
        catch( Exception ex )
        {

        }

        if( result == null )
        {
            try
            {
                result = kernel.getClass().getConstructor(DataCollection.class, String.class).newInstance(newCollection, newName);
            }
            catch( Exception ex )
            {

            }
        }
        if( result == null )
            return kernel;

        for (DynamicProperty dp: kernel.getAttributes())
            result.getAttributes().add(dp);
        
        if( result instanceof Referrer )
        {
            ( (Referrer)result ).setDatabaseReferences( ( (Referrer)kernel ).getDatabaseReferences());
            ( (Referrer)result ).setLiteratureReferences( ( (Referrer)kernel ).getLiteratureReferences());
            ( (Referrer)result ).setComment(( (Referrer)kernel ).getComment());
        }
        return result;
    }

    public static void replaceNode(biouml.model.Node toReplace, biouml.model.Node replacement) throws Exception
    {
        Compartment parent = toReplace.getCompartment();

        for( Edge edge : Util.getEdges(toReplace) )
        {
            if( edge.getInput().equals(toReplace) )
            {
                toReplace.removeEdge(edge);
                replacement.addEdge(edge);
                edge.setInput(replacement);
            }
            if( edge.getOutput().equals(toReplace) )
            {
                toReplace.removeEdge(edge);
                replacement.addEdge(edge);
                edge.setOutput(replacement);
            }
            //            biouml.model.Node anotherNode = edge.getInput().equals( toReplace )? edge.getOutput(): edge.getInput();
        }
        parent.remove(toReplace.getName());

        parent.put(replacement);

    }
    
    /**
     * Method removes subdiagram and replaces it with another one trying to keep all connections
     * @param toReplace
     * @param replacement
     * @throws Exception
     */
    public static void replaceSubDiagram(SubDiagram toReplace, SubDiagram replacement) throws Exception
    {
        Diagram parent = Diagram.getDiagram( toReplace );
        boolean in = false;

        for( Edge edge : Util.getEdges( toReplace ) )
        {
            if( edge.getInput().equals( toReplace ) )
            {
                in = true;
                edge.setInput( replacement );
                replacement.addEdge( edge );
            }
            else if( edge.getOutput().equals( toReplace ) )
            {
                edge.setOutput( replacement );
                replacement.addEdge( edge );
            }
            else if( edge.getInput().getParent().equals( toReplace ) )
            {
                in = true;
                String portName = edge.getInput().getName(); //in subdiagram port can be only on the top level, because no other compartment can not be in subdiagram element
                biouml.model.Node newPort = replacement.findNode( portName );
                edge.setInput( newPort );
                newPort.addEdge( edge );
            }
            else if( edge.getOutput().getParent().equals( toReplace ) )
            {
                String portName = edge.getOutput().getName();
                biouml.model.Node newPort = replacement.findNode( portName );
                edge.setOutput( newPort );
                newPort.addEdge( edge );
            }

            if( edge.getRole() instanceof Connection )
            {
                Connection connection = edge.getRole( Connection.class );
                Port port;
                String varName;
                if( in )
                {
                    varName = Util.getPortVariable( edge.getInput() );
                    port = connection.getInputPort();

                }
                else
                {
                    varName = Util.getPortVariable( edge.getOutput() );
                    port = connection.getOutputPort();
                }
                port.setVariableName( varName );
                port.setVariableTitle( varName );
            }
        }
        
        boolean notif = parent.isNotificationEnabled();
        parent.setNotificationEnabled(false);
//        State state = parent.getCurrentState();
//        parent.restore();

        parent.remove(toReplace.getName());
        replacement.save();

//        if( state != null )
//            parent.setStateEditingMode(state);

        parent.setNotificationEnabled(notif);
    }
    

    /**
     * Method returns diagram containing node.
     * Special cases are when node is subDiagram module or is element of subDiagram module
     * In these cases we should derive diagram from subDiagram module and return it
     * @param node
     * @return
     */
    public static Diagram getDiagram(biouml.model.Node node)
    {
        if( Util.isPort(node) && node.getParent() instanceof SubDiagram )
        {
            return ( (SubDiagram)node.getParent() ).getDiagram();
        }
        else if( node instanceof SubDiagram )
        {
            return ( (SubDiagram)node ).getDiagram();
        }
        return Diagram.getDiagram(node);
    }
    
    public static void createView(biouml.model.Node node)
    {
        if( node.getView() != null )
            return;
        View view;

        Diagram diagram = Diagram.getDiagram(node);
        DiagramViewBuilder diagramViewBuilder = diagram.getType().getDiagramViewBuilder();
        Graphics g = ApplicationUtils.getGraphics();
        if( node instanceof Diagram )
        {
            view = diagramViewBuilder.createDiagramView((Diagram)node, g);
        }
        else if( node instanceof Compartment )
        {
            view = diagramViewBuilder.createCompartmentView((Compartment)node, diagram.getViewOptions(), g);
        }
        else
        {
            view = diagramViewBuilder.createNodeView(node, diagram.getViewOptions(), g);
        }
        node.setView(view);
    }
    
    public static Edge clone(Edge oldEdge, Compartment newParent, Base newKernel, biouml.model.Node newInput, biouml.model.Node newOutput)
    {
        Edge edge = new Edge(newParent, newKernel, newInput, newOutput);
        Role role = oldEdge.getRole();
        if( role != null )
            edge.setRole(role.clone(edge));

        copyAttributes(oldEdge, edge);

        return edge;
    }
    

    public static void copyAttributes(DiagramElement from, DiagramElement to)
    {
        for( DynamicProperty dp : from.getAttributes() )
        {
            try
            {
                to.getAttributes().add(DynamicPropertySetSupport.cloneProperty(dp));
            }
            catch( Exception ex )
            {

            }
        }
    }

    public static boolean isFreePort(biouml.model.Node node)
    {
        for( Edge e : node.getEdges() )
        {
            if( Util.isDirectedConnection(e) || Util.isUndirectedConnection(e) )
                return false;
        }
        return true;
    }

    public static class NodeComparator implements Comparator<biouml.model.Node>
    {
        boolean xComparator;

        protected NodeComparator(boolean xComparator)
        {
            this.xComparator = xComparator;
        }

        @Override
        public int compare(biouml.model.Node n1, biouml.model.Node n2)
        {
            int v1;
            int v2;
            if( xComparator )
            {
                v1 = n1.getLocation().x;
                v2 = n2.getLocation().x;
            }
            else
            {
                v1 = n1.getLocation().y;
                v2 = n2.getLocation().y;
            }
            return ( v1 < v2 ) ? -1 : (v1 > v2) ? 1: 0;
        }
    }
    
    //TODO: more efficient algorithm may be implemented here
    public static HashMap<biouml.model.Node, Point> generateShiftMap(Diagram compositeDiagram)
    {
        HashMap<biouml.model.Node, Point> newLocations = new HashMap<>();

        biouml.model.Node[] nodes = compositeDiagram.getNodes();
        NodeComparator xComparator = new NodeComparator(true);
        Arrays.sort(nodes, xComparator);

        for( int i = 0; i < nodes.length - 1; i++ )
        {
            biouml.model.Node subDiagram = nodes[i];
            if( subDiagram instanceof SubDiagram )
            {
                Diagram innerDiagram = ( (SubDiagram)subDiagram ).getDiagram();
                CompositeModelUtility.createView(subDiagram);
                CompositeModelUtility.createView(innerDiagram);

                Rectangle moduleRec = subDiagram.getView().getBounds();
                int moduleWidth = moduleRec.width;

                Rectangle diagramRec = new Rectangle(innerDiagram.getView().getBounds());
                int diagramWidth = diagramRec.width;
                diagramRec.setLocation(moduleRec.getLocation());

                for( int j = i + 1; j < nodes.length; j++ ) //those nodes are located to the right from the subDiagram and should be shifted due to its expansion
                {
                    biouml.model.Node node = nodes[j];
                    Rectangle nodeRec = node.getView().getBounds();
                    Point location = nodeRec.getLocation();

                    if (nodeRec.y > moduleRec.y + moduleRec.height)
                        continue;

                    if( node instanceof SubDiagram )
                        node = ( (SubDiagram)node ).getDiagram();

                    Point newLocation = newLocations.containsKey(node) ? newLocations.get(node) : location;

                    if( diagramRec.intersects(nodeRec) )
                        newLocation.translate(diagramWidth - moduleWidth, 0);
 
                    newLocations.put(node, newLocation);
                }
            }
        }

        NodeComparator yComparator = new NodeComparator(false);
        Arrays.sort(nodes, yComparator);

        for( int i = 0; i < nodes.length - 1; i++ )
        {
            biouml.model.Node subDiagram = nodes[i];
            if( subDiagram instanceof SubDiagram )
            {
                Diagram innerDiagram = ( (SubDiagram)subDiagram ).getDiagram();
                CompositeModelUtility.createView(subDiagram);
                CompositeModelUtility.createView(innerDiagram);
                Rectangle diagramRec = new Rectangle(innerDiagram.getView().getBounds());
                Rectangle moduleRec = subDiagram.getView().getBounds();
                int moduleHeight = moduleRec.height;
                diagramRec.setLocation(moduleRec.getLocation());
                int diagramHeight = diagramRec.height;
                
                for( int j = i + 1; j < nodes.length; j++ ) //those nodes are located lower than the subDiagram and should be shifted due to its expansion
                {
                    biouml.model.Node node = nodes[j];
                    Rectangle nodeRec = new Rectangle(node.getView().getBounds());
                    Point location = nodeRec.getLocation();

                    if (nodeRec.x > moduleRec.x + moduleRec.width)
                        continue;
                    
                    CompositeModelUtility.createView(node);

                    if( node instanceof SubDiagram )
                        node = ( (SubDiagram)node ).getDiagram();

                    Point newLocation = newLocations.containsKey(node) ? newLocations.get(node) : location;
                    nodeRec.setLocation(newLocation);
                    if( diagramRec.intersects(nodeRec) )
                        newLocation.translate(0, diagramHeight - moduleHeight);

                    if( node instanceof SubDiagram )
                        node = ( (SubDiagram)node ).getDiagram();
                    newLocations.put(node, newLocation);
                }
            }
        }
        
        for(  biouml.model.Node node: nodes )
        {
            if (node instanceof SubDiagram)
            {
                Diagram d = ((SubDiagram)node).getDiagram();
                if (!newLocations.containsKey(d))
                    newLocations.put(d, node.getLocation());
            }
        }
        return newLocations;
    }

    /**
     * Finds all chains of undirected connections in diagram
     */
    public static Set<ConnectionChain> findUndirectedConnections(Diagram diagram)
    {
        Set<ConnectionChain> result = new HashSet<>();
        
        Set<Node> processed = new HashSet<>();
        
        for (Node port: diagram.recursiveStream().select(Node.class).filter(de->Util.isPort(de)))
        {
            if (processed.contains(port))
                continue;
            ConnectionChain chain = new ConnectionChain();
            findUndirectedConnections(port, chain);
            result.add(chain);
            processed.addAll(chain.nodes);
        }
            
        return result;
    }
    
    public static void findUndirectedConnections(Node node, ConnectionChain chain)
    {
        //TODO: implement for not-port connections
        if( Util.isPort(node) )
        {
            chain.nodes.add(node);
            for (Edge e: node.edges().filter(edge -> Util.isUndirectedConnection(edge) && edge.getRole() != null))//exclude propagation
            {
                UndirectedConnection connection = e.getRole( UndirectedConnection.class );
                if( !connection.getMainVariableType().equals(MainVariableType.NOT_SELECTED) )
                    chain.mainNode = connection.getMainVariableType().equals(MainVariableType.INPUT) ? e.getInput() : e.getOutput();

                Node otherEnd = e.getOtherEnd(node);
                if( !chain.nodes.contains(otherEnd) )
                    findUndirectedConnections(otherEnd, chain);
            }
        }
    }
    
    /**
     * Returns lowest port in chain of propagated ports<br>
     * Parent of this port is diagram
     */
    public static Variable getPropagatedVariable(Node node) throws Exception
    {
        if( node.getParent() instanceof SubDiagram )
            node = getInnerPort(node);

        while( Util.isPropagatedPort(node) )
            node = getInnerPort(Util.getBasePort(node));

        Diagram diagram = Diagram.getDiagram(node);
        EModel emodel = diagram.getRole(EModel.class);
        String varName = Util.getPortVariable(node);
        return emodel.getVariable(varName);
    }
    
    public static Node getInnerPort(Node node)
    {
        return ((SubDiagram)node.getParent()).getDiagram().findNode(node.getName());
    }


    public static class ConnectionChain
    {
        private final Set<Node> nodes = new HashSet<>();
        private Node mainNode = null;
        
        public Node getMainNode()
        {
            return mainNode;
        }
        
        public Set<Node> getNodes()
        {
            return nodes;
        }
    }
}
