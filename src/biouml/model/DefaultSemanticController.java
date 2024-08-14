package biouml.model;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.undo.Transaction;

import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.util.EModelHelper;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.undo.DataCollectionAddUndo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.access.support.IdGenerator;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.ViewEditorPane;

/**
 * Default implementation of <code>SemanticController</code> interface
 */
public class DefaultSemanticController implements SemanticController
{
    protected Logger log = Logger.getLogger( DefaultSemanticController.class.getName() );

    protected static final MessageBundle messageBundle = new MessageBundle();

    public static final String ERROR_CAN_NOT_CLONE_NODE = messageBundle.getResourceString( "ERROR_CAN_NOT_CLONE_NODE" );
    public static final String ERROR_NODE_IS_DUPLICATED = messageBundle.getResourceString( "ERROR_NODE_IS_DUPLICATED" );

    /**
     * Special function for type checkout - it return true only if
     * <code>base</code> parameter has the same java type as
     * <code>type</code> parameter and (moreover) if the type,
     * which returns by <code>Base.getType</code> function
     * has equal role
     */
    public static boolean checkType(Object type, Base base) throws Exception
    {
        if( type instanceof Class )
            return ( (Class<?>)type ).isInstance( base );
        if( type instanceof String )
            return base.getType().equals( type );
        return false;
    }

    /**
     * Default implementation which don't use additional criteria,
     * and check only data type for nodes and check edges input
     * and output
     */
    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        try
        {
            if( de != null && de.getKernel() != null )
            {
                Diagram diagram = Diagram.getDiagram( compartment );

                if( de instanceof Node )
                {
                    if( diagram.getType() == null )
                        return true;//all possible for unknown diagram type
                    for( Object nodeType : diagram.getType().getNodeTypes() )
                    {
                        if( checkType( nodeType, de.getKernel() ) )
                            return true;
                    }
                }
                else if( de instanceof Edge )
                {
                    for( Object edgeType : diagram.getType().getEdgeTypes() )
                    {
                        if( checkType( edgeType, de.getKernel() ) )
                        {
                            Edge edge = (Edge)de;
                            return edge.nodes().allMatch( n -> Diagram.getDiagram( n ) == diagram && CollectionFactory
                                    .getDataElement( CollectionFactory.getRelativeName( n, diagram ), diagram ) != null );
                        }
                    }
                }
            }
        }
        catch( Throwable e )
        {
            log.log( Level.SEVERE, "Error during type checkout", e );
        }
        return false;
    }

    /**
     * @returns whether a specified diagram element can be resized.
     */
    @Override
    public boolean isResizable(DiagramElement diagramElement)
    {
        if( diagramElement.getKernel() instanceof Stub.Note )
            return true;

        return diagramElement instanceof Compartment && ( ! ( diagramElement instanceof Diagram ) )
                && ! ( diagramElement instanceof EquivalentNodeGroup );
    }
    /**
     * Removes the diagram element and all related edges.
     *
     * @pending if compartment is removed we should remove outer edges for
     *          compartment nodes
     */
    @Override
    public boolean remove(DiagramElement de) throws Exception
    {
        if( de instanceof Diagram )
            return false;

        // remove edges
        if( de instanceof Node )
        {
            Node node = (Node)de;

            for( Edge edge : Util.getEdges( node ) )
                edge.getOrigin().remove( edge.getName() );

            if( de instanceof Compartment )
                ( (Compartment)de ).clear();
        }

        // remove diagramElement
        de.getOrigin().remove( de.getName() );
        return true;
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, Point point, ViewEditorPane viewEditor)
    {
        throw new UnsupportedOperationException( "DefaultSemanticController.createInstance method should be defined in subclasses." );
    }

    /**
     * Creates new instance of <code>DiagramElement</code> with specific kernel type and properties
     * without using any interface dialogs.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, Point point, Object properties)
    {
        throw new UnsupportedOperationException( "DefaultSemanticController.createInstance method should be defined in subclasses." );
    }

    /**
     * Gets the default properties of <code>DiagramElement</code> by its type for the correct element creation
     * by createInstance method
     */
    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        return null;
    }

    /**
     * Moves the specified diagram element to the specified position. If necessary
     * compartment can be changed.
     *
     * @returns the actual distance on which the diagram node was moved.
     */
    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if( de instanceof Diagram )
        {
            if( ! ( de.getOrigin() instanceof DiagramElement ) )
            {
                Diagram diagram = (Diagram)de;
                Point location = diagram.getLocation();
                location.translate( offset.width, offset.height );
                diagram.setLocation( location );
            }
        }
        else if( de instanceof Node )
        {
            Node node = (Node)de;

            Point location = node.getLocation();
            location.translate( offset.width, offset.height );

            Compartment parent = (Compartment)node.getOrigin();
            if( node == newParent )
                newParent = (Compartment)node.getOrigin();

            if( newParent != parent )
            {
                if( newParent.get( node.getName() ) != null )
                    throw new Exception( ERROR_NODE_IS_DUPLICATED );
                else
                {
                    if( canAccept( newParent, node ) )
                        node = changeNodeParent( node, newParent );
                }
            }

            node.setLocation( location );
            if( node instanceof Compartment )
                moveInCompartment( node, (Compartment)node, offset );

            for( Edge edge : node.getEdges() )
            {
                if( edge.isFixedInOut() )
                {
                    Path oldPath = edge.getPath();
                    Path newPath = new Path( oldPath.xpoints, oldPath.ypoints, oldPath.pointTypes, oldPath.npoints );
                    if( edge.getInput().equals( node ) )
                    {
                        newPath.xpoints[0] += offset.width;
                        newPath.ypoints[0] += offset.height;
                    }
                    else if( edge.getOutput().equals( node ) )
                    {
                        newPath.xpoints[newPath.npoints - 1] += offset.width;
                        newPath.ypoints[newPath.npoints - 1] += offset.height;
                    }
                    edge.setPath( newPath );
                }
                recalculateEdgePath( edge );
            }
        }
        else if( de instanceof Edge )
        {
            Edge edge = (Edge)de;
            Path oldPath = edge.getPath();
            if( oldPath != null )
            {
                Path newPath = new Path();
                List<Integer> movedVertex = new ArrayList<>();
                Rectangle bounds = null;
                if( oldBounds != null )
                    bounds = new Rectangle( oldBounds.x, oldBounds.y, oldBounds.width + 1, oldBounds.height + 1 );

                for( int i = 0; i < oldPath.npoints; i++ )
                {
                    newPath.addPoint( oldPath.xpoints[i], oldPath.ypoints[i], oldPath.pointTypes[i] );
                    if( bounds == null || bounds.contains( oldPath.xpoints[i], oldPath.ypoints[i] ) )
                    {
                        if( ( i == 0 || i == oldPath.npoints - 1 ) && edge.isFixedInOut() )
                        {
                            Node node = i == 0 ? edge.getInput() : edge.getOutput();
                            Point location = new Point( oldPath.xpoints[i], oldPath.ypoints[i] );
                            location.translate( offset.width, offset.height );
                            Point p = Diagram.getDiagram( de ).getType().getDiagramViewBuilder().getNearestNodePoint( location, node );
                            newPath.xpoints[i] = p.x;
                            newPath.ypoints[i] = p.y;
                            movedVertex.add( i );
                        }
                        else
                        {
                            newPath.xpoints[i] += offset.width;
                            newPath.ypoints[i] += offset.height;
                            movedVertex.add( i );
                        }
                    }
                }
                //delete vertex if the distance between vertexes is small
                for( int pos = movedVertex.size(); pos > 0; pos-- )
                {
                    for( int i = 0; i < newPath.npoints; i++ )
                    {
                        if( ( i != pos ) && ( Math.abs( newPath.xpoints[pos] - newPath.xpoints[i] ) < 7
                                && Math.abs( newPath.ypoints[pos] - newPath.ypoints[i] ) < 7 ) )
                        {
                            newPath.removePoint( pos );
                            break;
                        }
                    }
                }
                edge.setPath( newPath );
            }
            recalculateEdgePath( edge );
        }
        return offset;
    }


    /**
     * Move all inner nodes and edges assuming that user moved compartment<br>
     * In that case we should not use default method because we need only to change coordinates of nodes<br>
     * However if node is compartment - method should be applied recursively
     * Edges which connects nodes which lies inside moving compartment should be just translated
     * Edges which connects node from the inside of the moving compartment and node from the outside should be recalculated
     * @param de
     * @param movingCompartment - compartment that user is moving
     * @param offset
     */
    public void moveInCompartment(Node de, Compartment movingCompartment, Dimension offset)
    {
        if( de instanceof Compartment )
        {
            for( DiagramElement innerDe : (Compartment)de )
            {
                if( innerDe instanceof Node )
                {
                    Node innerNode = (Node)innerDe;
                    Point location = innerNode.getLocation();
                    location.translate( offset.width, offset.height );
                    innerNode.setLocation( location );

                    for( Edge e : innerNode.getEdges() )
                    {
                        Node otherNode = e.getOtherEnd( innerNode );
                        if( !movingCompartment.equals( Compartment.findCommonOrigin( innerNode, otherNode ) ) )
                            recalculateEdgePath( e );
                    }

                    moveInCompartment( innerNode, movingCompartment, offset );
                }
                else if( innerDe instanceof Edge )
                {
                    Path path = ( (Edge)innerDe ).getPath();
                    if( path != null )
                    {
                        Path newPath = path.clone();
                        newPath.translate( offset.width, offset.height );
                        ( (Edge)innerDe ).setPath( newPath );
                    }
                }
            }
        }
    }

    @Override
    public void recalculateEdgePath(Edge edge)
    {
        Diagram diagram = Diagram.getDiagram( edge );
        DiagramType diagramType = diagram.getType();
        if( diagramType == null )
            return;

        DiagramViewOptions viewOptions = diagram.getViewOptions();
        if( !edge.isFixed() && viewOptions != null && viewOptions.isAutoLayout() && diagramType.needAutoLayout( edge ) )
        {
            if( DiagramToGraphTransformer.layoutSingleEdge( edge, viewOptions.getPathLayouter(), getFilter() ) )
            {
                edge.setView( diagram.getType().getDiagramViewBuilder().createEdgeView( edge, diagram.getViewOptions(),
                        ApplicationUtils.getGraphics() ) );
                return;
            }
        }

        Point in = new Point();
        Point out = new Point();

        Path path = edge.getPath();
        if( !edge.isFixedInOut() || path == null || path.npoints < 2 )
        {
            diagramType.getDiagramViewBuilder().calculateInOut( edge, in, out );
        }
        else
        {
            in = new Point( path.xpoints[0], path.ypoints[0] );
            out = new Point( path.xpoints[path.npoints - 1], path.ypoints[path.npoints - 1] );
        }
        if( path == null || path.npoints <= 2 )
        {
            // don't change anything if path wasn't changed
            // this also prevents event bubbling in edge.setPath
            if( path != null && path.npoints == 2 && path.xpoints[0] == in.x && path.xpoints[1] == out.x && path.ypoints[0] == in.y
                    && path.ypoints[1] == out.y )
            {
                edge.setView( diagram.getType().getDiagramViewBuilder().createEdgeView( edge, diagram.getViewOptions(),
                        ApplicationUtils.getGraphics() ) );
                return;
            }
            edge.setPath( new Path( new int[] {in.x, out.x}, new int[] {in.y, out.y}, 2 ) );
        }
        else
        {
            double bestDistIn = Double.MAX_VALUE, bestDistOut = Double.MAX_VALUE;
            int bestPointIn = 0, bestPointOut = 0;
            if( Math.abs( in.x - path.xpoints[0] ) + Math.abs( in.y - path.ypoints[0] ) < 3 )
            {
                bestPointIn = 1;
                bestDistIn = 0;
            }
            if( Math.abs( out.x - path.xpoints[path.npoints - 1] ) + Math.abs( out.y - path.ypoints[path.npoints - 1] ) < 3 )
            {
                bestPointOut = path.npoints - 2;
                bestDistOut = 0;
            }
            int[] tempXpoints = path.xpoints.clone();
            int[] tempYpoints = path.ypoints.clone();
            tempXpoints[0] = in.x;
            tempYpoints[0] = in.y;
            tempXpoints[tempXpoints.length - 1] = out.x;
            tempYpoints[tempYpoints.length - 1] = out.y;
            for( int i = 1; i < path.npoints - 1; i++ )
            {
                double distIn = ( in.x - tempXpoints[i] ) * ( in.x - tempXpoints[i] )
                        + ( in.y - tempYpoints[i] ) * ( in.y - tempYpoints[i] );
                double distOut = ( out.x - tempXpoints[i] ) * ( out.x - tempXpoints[i] )
                        + ( out.y - tempYpoints[i] ) * ( out.y - tempYpoints[i] );
                if( distIn < bestDistIn )
                {
                    bestDistIn = distIn;
                    bestPointIn = i;
                }
                if( distOut < bestDistOut )
                {
                    bestDistOut = distOut;
                    bestPointOut = i;
                }
            }
            int newLength = Math.max( 0, bestPointOut - bestPointIn ) + 3;
            int[] newXpoints = new int[newLength];
            int[] newYpoints = new int[newLength];
            int[] newPointTypes = new int[newLength];
            newXpoints[0] = in.x;
            newYpoints[0] = in.y;
            newPointTypes[0] = path.pointTypes[0];
            newXpoints[newXpoints.length - 1] = out.x;
            newYpoints[newYpoints.length - 1] = out.y;
            newPointTypes[newPointTypes.length - 1] = Path.LINE_TYPE;
            System.arraycopy( tempXpoints, bestPointIn, newXpoints, 1, newLength - 2 );
            System.arraycopy( tempYpoints, bestPointIn, newYpoints, 1, newLength - 2 );
            System.arraycopy( path.pointTypes, bestPointIn, newPointTypes, 1, newLength - 2 );
            Path newPath = new Path( newXpoints, newYpoints, newPointTypes, newLength );
            edge.setPath( newPath );
            if( !edge.isFixedInOut() )
                diagramType.getDiagramViewBuilder().calculateInOut( edge, in, out );
            newPath.xpoints[0] = in.x;
            newPath.ypoints[0] = in.y;
            newPath.xpoints[newPath.npoints - 1] = out.x;
            newPath.ypoints[newPath.npoints - 1] = out.y;
            edge.setPath( newPath );
        }
        edge.setView( diagram.getType().getDiagramViewBuilder().createEdgeView( edge, diagram.getViewOptions(),
                ApplicationUtils.getGraphics() ) );
    }

    private @Nonnull Node translateNode(@Nonnull Node oldNode, @Nonnull Node oldParent, @Nonnull Node newParent)
    {
        if( oldNode == oldParent )
            return newParent;
        if( ! ( oldParent instanceof Compartment ) || ! ( newParent instanceof Compartment ) )
            return oldNode;
        DataCollection<?> parent = oldNode.getOrigin();
        String path = oldNode.getName();
        while( parent != null && ! ( parent instanceof Diagram ) )
        {
            if( parent == oldParent )
                return CollectionFactory.getDataElement( path, (DataCollection<?>)newParent, Node.class );
            path = parent.getName() + "/" + path;
            parent = parent.getOrigin();
        }
        return oldNode;
    }

    /**
     * Clones the node in another compartment recreating all edges as well
     * @param node
     * @param newParent
     * @return newly created node
     * @throws Exception
     */
    protected Node changeNodeParent(Node oldNode, Compartment newParent) throws Exception
    {
        Set<Edge> edges = oldNode.recursiveStream().select( Node.class ).flatMap( Node::edges ).toSet();
        for( Edge edge : edges )
            edge.getOrigin().remove( edge.getName() );

        Node newNode = oldNode.clone( newParent, oldNode.getName() );

        Diagram diagram = Diagram.getDiagram( newNode );
        
        //TODO: move this to cloning
        if( oldNode.getRole() instanceof VariableRole )
        {
            VariableRole role = oldNode.getRole( VariableRole.class );
            newNode.setRole( role.clone( newNode, VariableRole.createName( newNode, false ) ) );
        }

        newNode.save();
        for( Edge edge : edges )
        {
            Node inNode = edge.getInput();
            Node outNode = edge.getOutput();
            Node newInNode = translateNode( inNode, oldNode, newNode );
            Node newOutNode = translateNode( outNode, oldNode, newNode );
            Edge newEdge = new Edge( edge.getName(), edge.getKernel(), newInNode, newOutNode );
            newEdge.setPropagationEnabled( false );
            newEdge.setTitle( edge.getTitle() );
            Role role = edge.getRole();
            if( role != null )
                newEdge.setRole( role.clone( newEdge ) );

            newEdge.setPath( edge.getPath() );
            newEdge.setInPort( edge.getInPort() );
            newEdge.setOutPort( edge.getOutPort() );
            DynamicPropertySet attributes = edge.getAttributes();
            
            if( attributes != null )
            {
                for( DynamicProperty oldProp : attributes )
                {
                    DynamicProperty prop = null;
                    try
                    {
                        prop = DynamicPropertySetSupport.cloneProperty( oldProp );
                    }
                    catch( Exception e )
                    {
                        prop = oldProp;
                    }
                    newEdge.getAttributes().add( prop );
                }
            }
            newEdge.setPropagationEnabled( edge.isPropagationEnabled() );
            newEdge.save();
        }

        //if compartment was changed and node contains variable then variable name was also changed
        //so we need to change all reference to this variable in the diagram
        Role role = diagram.getRole();
        if( role instanceof EModel )
        {
            Role oldRole = oldNode.getRole();
            if( oldRole instanceof VariableRole )
            {
                String oldName = ( (VariableRole)oldRole ).getName();
                String newName = newNode.getRole( VariableRole.class ).getName();
                EModelHelper helper = new EModelHelper( (EModel)role );
                helper.renameVariable( oldName, newName, true );
            }
        }
        
        if( newNode instanceof Compartment )
        {
            for( Node node : newNode.recursiveStream().without( newNode ).select( Node.class )
                    .filter( n -> n.getRole() instanceof VariableRole ) )
            {
                VariableRole innerRole = node.getRole( VariableRole.class );
                node.setRole( innerRole.clone( node, VariableRole.createName( node, false ) ) );
                EModelHelper helper = new EModelHelper( (EModel)role );
                ( (EModel)role ).getVariableRoles().put( (VariableRole)node.getRole() );
                helper.renameVariable( innerRole.getName(), node.getRole( VariableRole.class ).getName(), true ); //TODO: rename variables in batch (or do not rename at all)

            }
        }

        //finally remove old node. Possible issue in future here could be that new node have the same name as old but in another compartment
        remove( oldNode );
    
        return newNode;
    }


    protected void moveEdges(Node oldNode, Node newNode) throws Exception
    {

        if( oldNode instanceof Compartment && newNode instanceof Compartment )
        {
            for( Node childNode : ( (Compartment)oldNode ).getNodes() )
            {
                DataElement newChildNode = ( (Compartment)newNode ).get( childNode.getName() );
                if( newChildNode instanceof Node )
                    moveEdges( childNode, (Node)newChildNode );
            }
        }
    }

    protected Node findNewNode(Node oldNode, @Nonnull Compartment oldCompartment, Compartment newCompartment)
    {
        String name = CollectionFactory.getRelativeName( oldNode, oldCompartment );
        return CollectionFactory.getDataElement( name, newCompartment, Node.class );
    }

    protected void moveEdge(Edge oldEdge, Node inNode, Node outNode) throws Exception
    {
        oldEdge.getOrigin().remove( oldEdge.getName() );
        Edge newEdge = new Edge( oldEdge.getKernel(), inNode, outNode );
        Role oldRole = oldEdge.getRole();
        if( oldRole != null )
            newEdge.setRole( oldRole.clone( newEdge ) );
        newEdge.setInPort( null );
        newEdge.setOutPort( null );
        newEdge.save();
    }

    /**
     * Fills set with names of all diagram elements added to diagram by given edit     
     */
    private static void fillAddedElementNames(UndoableEdit edit, Set<String> names)
    {
        if( edit instanceof DataCollectionAddUndo )
            names.add( ( (DataCollectionAddUndo)edit ).getDataElement().getName() );

        else if( edit instanceof Transaction )
        {
            for( UndoableEdit innerEdit : ( (Transaction)edit ).getEdits() )
                fillAddedElementNames( innerEdit, names );
        }
    }
    
    public static String generateUniqueName(Compartment compartment, String baseName)
    {
        return generateUniqueName(compartment, baseName, true);
    }
    
    /**
     * Method to generate unique name of diagram element inside the diagram (considering all nested compartments)
     * @param diagram - given diagram
     * @param baseName - name which should be transformed to unique by appending suffix
     * @param tryNoIndex - if true then method will try baseName without index at first
     * @return string of type "baseName_i" where i - integer index
     */
    public static String generateUniqueName(Compartment compartment, String baseName, boolean tryNoIndex)
    {
        Diagram diagram = Diagram.getDiagram( compartment );

        //        Set<String> names = diagram.recursiveStream().map( de -> de.getName() ).toSet(); //names of all current elements inside the diagram
        Set<String> names = new HashSet<>();
        diagram.states().flatCollection( state -> state.getStateUndoManager().getEdits() ).forEach( edit->fillAddedElementNames(edit, names) ); //names of all the elements added by all states of the diagram

        int index = 1;
        
        if( tryNoIndex && !diagram.containsRecursively( baseName ) && !names.contains( baseName ) )
            return baseName;

        String result = baseName + "_" + index;
        while( diagram.containsRecursively( result ) && !names.contains( baseName ))
        {
            index++;
            result = baseName + "_" + index;
        }
        return result;
    }

    public static String generateUniqueNodeName(Compartment compartment, String baseName)
    {
        return generateUniqueNodeName( compartment, baseName, true );
    }

    public static boolean isNodeNameUnique(Compartment compartment, String id)
    {
        return isNodeNameUnique( compartment, id, true );
    }

    public static String generateUniqueNodeName(Compartment compartment, String id, boolean includeSubCompartments)
    {
        return generateUniqueNodeName( compartment, id, includeSubCompartments, "_" );
    }

    /**
     * Generate unique node name
     */
    public static String generateUniqueNodeName(Compartment compartment, String baseName, boolean includeSubCompartments, String delimiter)
    {
        // find top level compartment that is diagram
        while( compartment.getOrigin() instanceof Compartment && ! ( compartment instanceof Diagram ) )//TODO: replace this by Diagram.getDiagram
            compartment = (Compartment)compartment.getOrigin();

        if( isNodeNameUnique( compartment, baseName, includeSubCompartments ) )
            return baseName;

        String id = baseName + delimiter;
        int n = 1;

        while( !isNodeNameUnique( compartment, id + n, includeSubCompartments ) )
            n++;

        return id + n;
    }

    public static boolean isNodeNameUnique(Compartment compartment, String id, boolean includeSubCompartments)
    {
        if( compartment.contains( id ) )
            return false;

        if( compartment instanceof Diagram )
        {
            if( ( (Diagram)compartment ).states().flatCollection( state -> state.getStateUndoManager().getEdits() )
                    .anyMatch( edit -> !isNameUnique( edit, id ) ) )
                return false;
        }

        if( !includeSubCompartments )
            return true;

        for( DiagramElement de : compartment )
        {
            if( de instanceof Compartment && !isNodeNameUnique( (Compartment)de, id, true ) )
                return false;
        }

        return true;
    }

    protected static boolean isNameUnique(UndoableEdit edit, String name)
    {
        if( edit instanceof DataCollectionAddUndo && name.equals( ( (DataCollectionAddUndo)edit ).getDataElement().getName() ) )
            return false;
        else if( edit instanceof Transaction )
        {
            for( UndoableEdit innerEdit : ( (Transaction)edit ).getEdits() )
            {
                if( !isNameUnique( innerEdit, name ) )
                    return false;
            }
        }
        return true;
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de) throws Exception
    {
        //Nothing to do by default
        return validate( compartment, de, false );
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception
    {
        //Nothing to do by default
        return de;
    }

    @Override
    public DiagramElementGroup addInstanceFromElement(Compartment compartment, DataElement dataElement, Point point,
            ViewEditorPane viewEditor) throws Exception
    {
        Node node = compartment.findNode( dataElement.getName() );
        if( node != null )
        {
            log.info( "Element " + dataElement.getName() + " already exists in compartment " + compartment.getName() );
            return new DiagramElementGroup( node );
        }
        DiagramElementGroup result = createInstanceFromElement( compartment, dataElement, point, viewEditor );
        for( DiagramElement de : result.nodesStream() )
            if( de != null )
                viewEditor.add( de, point );
        for( DiagramElement de : result.edgesStream() )
            if( de != null )
                viewEditor.add( de, point );
        return result;
    }

    public DiagramElementGroup createInstanceFromElement(Compartment compartment, DataElement element, Point point,
            ViewEditorPane viewEditor) throws Exception
    {
        String name = element.getName();
        int i = 2;
        while( compartment.contains( name ) )
            name = element.getName() + "_" + ( i++ );
        DiagramElement result = null;
        if( element instanceof Node )
            result = ( (DiagramElement)element ).clone( compartment, name );
        else if( element instanceof Edge )
        {
            Edge edge = (Edge)element;
            DiagramElement input = compartment.get( edge.getInput().getName() );
            Point inputPos = null, outputPos = null;
            if( input == null || ! ( input instanceof Node ) || input.getKernel() != edge.getInput().getKernel() )
            {
                inputPos = new Point( point.x, point.y - 50 );
                input = createInstanceFromElement( compartment, edge.getInput(), inputPos, viewEditor ).getElement();
                if( input == null )
                    throw new Exception( "Cannot create input node for edge" );
            }
            DiagramElement output = compartment.get( edge.getOutput().getName() );
            if( output == null || ! ( output instanceof Node ) || output.getKernel() != edge.getOutput().getKernel() )
            {
                outputPos = new Point( point.x, point.y + 50 );
                output = createInstanceFromElement( compartment, edge.getOutput(), outputPos, viewEditor ).getElement();
                if( output == null )
                    throw new Exception( "Cannot create output node for edge" );
            }
            if( !compartment.contains( input.getName() ) && inputPos != null )
                viewEditor.add( input, inputPos );
            if( !compartment.contains( output.getName() ) && outputPos != null )
                viewEditor.add( output, outputPos );
            result = new Edge( compartment, edge.getKernel(), (Node)input, (Node)output );
        }
        else if( element instanceof Base )
            result = new Node( compartment, name, (Base)element );
        if( result != null && canAccept( compartment, result ) )
            return new DiagramElementGroup( result );
        return DiagramElementGroup.EMPTY_EG;
    }

    /**
     * Generate unique name for reaction
     */
    public static String generateReactionName(DataCollection<?> reactionDC)// throws Exception
    {
        DecimalFormat formatter = Reaction.NAME_FORMAT;
        if( reactionDC != null )
        {
            String idFormat = reactionDC.getInfo().getProperty( DataCollectionConfigConstants.ID_FORMAT );
            if( idFormat != null )
                formatter = new DecimalFormat( idFormat );
        }
        int startInd = reactionDC instanceof Diagram ? (int) ( (Diagram)reactionDC ).recursiveStream().count() + 1 : 0;
        return IdGenerator.generateUniqueName( reactionDC, formatter, startInd );
    }

    @Override
    public Filter<DiagramElement> getFilter()
    {
        // TODO Support filters
        return null;
    }

    @Override
    public Dimension resize(DiagramElement de, Dimension sizeChange)
    {
        return sizeChange; //by default
    }

    @Override
    public Node cloneNode(@Nonnull Node node, String newName, Point location)
    {
        Node newNode = node.clone( node.getCompartment(), newName );
        if ( newNode.getKernel() instanceof biouml.standard.type.Compartment )
            ((Compartment)newNode).clear();
        newNode.setRole( node.getRole() );
        newNode.setLocation( location );
        return newNode;
    }

    @Override
    public Node copyNode(@Nonnull Node node, @Nonnull String newName, Compartment newParent, Point location)
    {
        Base kernel = node.getKernel();
        if( kernel instanceof BaseSupport )
        {
            BaseSupport newKernel = ( (BaseSupport)kernel ).clone( kernel.getOrigin(), newName );
            Node result = node.clone( newParent, newName, newKernel );
            Role newRole = copyRole( node.getRole(), result );
            result.setRole( newRole );
            result.setLocation( location );
            return result;
        }
        else
        {
            throw new IllegalArgumentException( "Diagram element " + node.getName() + " can not be copied!" );
        }
    }

    @Override
    public Edge createEdge(@Nonnull Node fromNode, @Nonnull Node toNode, String edgeType, Compartment compartment)
    {
        Compartment origin = Node.findCommonOrigin( toNode, fromNode );
        Base edgeKernel = null;
        String edgeName = "From " + fromNode.getName() + " to " + toNode.getName();
        Node specieNode = toNode;
        Reaction reaction = getReactionByNode( fromNode );
        if( reaction == null )
        {
            reaction = getReactionByNode( toNode );
            specieNode = fromNode;
        }

        if( RelationType.SEMANTIC.equals( edgeType ) )
        {
            edgeKernel = new SemanticRelation( null, edgeName );
        }
        else if( RelationType.NOTE_LINK.equals( edgeType ) )
        {
            edgeKernel = new Stub.NoteLink( reaction, edgeName );
        }
        else
        {
            edgeKernel = new SpecieReference( reaction, edgeName, edgeType );
            ( (SpecieReference)edgeKernel ).setSpecie( specieNode.getCompleteNameInDiagram() );
            reaction.put( (SpecieReference)edgeKernel );
        }
        DataElement oldEdge = findEdge( fromNode, toNode, edgeKernel );
        if( oldEdge == null )
        {
            Edge edge = new Edge( origin, edgeKernel, fromNode, toNode );
            return edge;
        }
        return null;
    }

    protected static Reaction getReactionByNode(Node node)
    {
        Base kernel = node.getKernel();
        if( kernel != null )
        {
            if( kernel instanceof Reaction )
                return (Reaction)kernel;
            else if( kernel instanceof Stub && kernel.getType().equals( Type.TYPE_REACTION ) )
            {
                Object reactionNameObj = node.getAttributes().getValue( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY );
                if( reactionNameObj != null )
                {
                    DataElement reactionDe = CollectionFactory.getDataElement( reactionNameObj.toString() );
                    if( reactionDe instanceof Reaction )
                    {
                        return (Reaction)reactionDe;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Search for edge between nodes with the same kernel type (and same Role in case of SpecieReference)
     * Is used to check whether edge already exists
     */
    @Override
    public Edge findEdge(Node from, Node to, Base kernel)
    {
        Edge[] edges = from.getEdges();
        for( Edge e : edges )
        {
            if( e.getInput().getCompleteNameInDiagram().equals( from.getCompleteNameInDiagram() )
                    && e.getOutput().getCompleteNameInDiagram().equals( to.getCompleteNameInDiagram() ) )
            {
                if( e.getKernel() != null )
                {
                    if( e.getKernel().getClass().equals( kernel.getClass() ) )
                    {
                        if( e.getKernel() instanceof SpecieReference )
                        {
                            if( ( (SpecieReference)e.getKernel() ).getRole().equals( ( (SpecieReference)kernel ).getRole() ) )
                                return e;
                        }
                        else
                            return e;
                    }
                }
                else if( kernel == null )
                    return e;
            }
        }
        return null;
    }

    @Override
    public Dimension resize(DiagramElement de, Dimension sizeChange, Dimension offset)
    {
        return resize( de, sizeChange );
    }

    @Override
    public String validateName(String name)
    {
        return name; //nothing to do by default
    }

    private Role copyRole(Role oldRole, DiagramElement de)
    {
        return oldRole instanceof VariableRole ? ( (VariableRole)oldRole ).clone( de, VariableRole.createName( de, false ) )
                : oldRole == null ? null : oldRole.clone( de );
    }

    @Override
    public boolean isAcceptableForReaction(Node node)
    {
        return node.getRole() instanceof VariableRole && !(node.getKernel() instanceof biouml.standard.type.Compartment);
    }
}
