package biouml.model;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.access.core.ClassIcon;
import com.developmentontheedge.beans.DPSProperties;
import ru.biosoft.util.TextUtil;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.type.Base;


/**
 * General definition of the compartment as a container for nodes and edges between them.
 *
 * @pending DataCollectionEvents - primary vector collection is used as event source.
 * To solve the problem we should redefine all events processing like Transformed or Derived
 * DataCollections.
 */
@SuppressWarnings ( "serial" )
@ClassIcon ( "resources/compartment.gif" )
@PropertyName("Compartment")
@PropertyDescription("Compartment is a container to group nodes and edges between them. For example it may correpsond to cell or organism compartment.")
public class Compartment extends Node implements DataCollection<DiagramElement>
{
    protected static final Logger log = Logger.getLogger(Compartment.class.getName());
    protected int shapeType;
    private DataElementPath completeName = null;

    /**
     * Create compartment for specific parent with id and kernel
     */
    public Compartment(DataCollection<?> parent, String id, Base kernel)
    {
        super(parent, id, kernel);
        Properties props = (DPSProperties) ( new DPSProperties(getAttributes()) ).clone();
        props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, id);
        collection = new VectorDataCollection<>(parent, props);
        setShapeSize(new Dimension(150, 150));
    }

    /**
     * Create compartment with <code>id == kernel.getName()</code>
     */
    public Compartment(DataCollection<?> parent, Base kernel)
    {
        this(parent, kernel.getName(), kernel);
    }

    /**
     * Search in compartment and in contains
     * <code>EquivalentNodeGroup</code>-s
     * @return found node or null if it was not found
     */
    public Node findNode(@Nonnull String id)
    {
        String[] path = id.split("\\.");
        if( path.length == 1 )
        {
            DiagramElement node = get(id);
            if( node instanceof Node )
            {
                return (Node)node;
            }
            // search in equivalent node groups
            for(DiagramElement obj: this)
            {
                if( obj instanceof EquivalentNodeGroup )
                {
                    obj = ( (EquivalentNodeGroup)obj ).get(id);
                    if( obj instanceof Node )
                    {
                        node = obj;
                        break;
                    }
                }
                else if( obj instanceof Compartment )
                {
                    Node tmp = ( (Compartment)obj ).findNode(id);
                    if( tmp != null )
                    {
                        node = tmp;
                        break;
                    }
                }
            }
            return (Node)node;
        }
        try
        {
            DiagramElement comp = StreamEx.of( path )
                    .foldLeft( this, (DiagramElement c, String name) -> c.cast( Compartment.class ).get( name ) );
            return comp == null ? null: comp.cast( Node.class );
        }
        catch( NullPointerException | LoggedClassCastException e )
        {
            return null;
        }
    }

    /**
     * Find diagram element
     */
    public DiagramElement findDiagramElement(String id)
    {
        if( id.indexOf('.') == -1 )
        {
            DiagramElement diagramElement = get(id);

            // Search in equivalent node groups
            if( diagramElement == null )
            {
                for(DiagramElement obj: this )
                {
                    if( obj instanceof EquivalentNodeGroup )
                    {
                        obj = ( (EquivalentNodeGroup)obj ).get(id);
                        if( obj != null )
                        {
                            diagramElement = obj;
                            break;
                        }
                    }
                    else if( obj instanceof Compartment )
                    {
                        DiagramElement tmp = ( (Compartment)obj ).findDiagramElement(id);
                        if( tmp != null )
                        {
                            diagramElement = tmp;
                            break;
                        }
                    }
                }
            }
            return diagramElement;
        }
        return getDiagramElement(id);
    }

    public DiagramElement getDiagramElement(String completeNameInDiagram)
    {
        if(completeNameInDiagram.isEmpty()) return this;
        String[] path = TextUtil.split(completeNameInDiagram, '.');
        Compartment comp = this;
        for( int i = 0; i < path.length - 1; i++ )
        {
            if( !comp.contains(path[i]) )
                break;
            DataElement element = comp.get(path[i]);
            if( ! ( element instanceof Compartment ) )
                break;
            comp = (Compartment)comp.get(path[i]);
        }
        return comp.get(path[path.length - 1]);
    }

    /**
     * Search element in diagram and elements roles
     */
    public Object findObject(String id) throws Exception
    {
        DataElement de = get(id);

        if( de == null )
        {
            for(DiagramElement obj: this)
            {
                if( obj instanceof EquivalentNodeGroup )
                {
                    obj = ( (EquivalentNodeGroup)obj ).get(id);
                    if( obj != null )
                    {
                        de = obj;
                        break;
                    }
                }
                else if( obj instanceof Compartment )
                {
                    Object tmp = ( (Compartment)obj ).findObject(id);
                    if( tmp != null )
                    {
                        return tmp;
                    }
                }
                else
                {
                    Role role = obj.getRole();
                    if( role instanceof DataElement )
                    {
                        if( ( (DataElement)role ).getName().equals(id) )
                        {
                            return role;
                        }
                    }
                }
            }
        }

        return de;
    }

    //////////////////////////////////////////////////////////////////
    // Utility methods for QueryEngine
    //

    protected Map<Base,List<DiagramElement>> kernelMap = new HashMap<>();

    /**
     * Check if this compartment contain any node with kernel
     */
    public boolean containsKernel(Base kernel)
    {
        return kernelMap.containsKey(kernel);
    }

    /**
     * Return list if all all nodes in this compartment with kernel
     */
    public StreamEx<Node> getKernelNodes(Base kernel)
    {
        List<DiagramElement> list = kernelMap.get(kernel);
        if(list == null) return StreamEx.empty();
        return StreamEx.of( list ).select( Node.class );
    }

    //////////////////////////////////////////////////////////////////
    // Properties
    //

    public static final int SHAPE_RECTANGLE = 0;
    public static final int SHAPE_ROUND_RECTANGLE = 1;
    public static final int SHAPE_ELLIPSE = 2;

    @PropertyName("Shape type")
    public int getShapeType()
    {
        return shapeType;
    }
    public void setShapeType(int shapeType)
    {
        int oldValue = this.shapeType;
        this.shapeType = shapeType;
        firePropertyChange("shapeType", oldValue, shapeType);
    }

    /////////////////////////////////////////////////////////////////
    // DataCollection interface implementation through delegation
    //

    /** Primary collection. */
    protected VectorDataCollection<DiagramElement> collection;

    @Override
    public DataCollectionInfo getInfo()
    {
        return collection.getInfo();
    }

    @Override
    public int getSize()
    {
        return collection.getSize();
    }

    @Override
    public boolean isEmpty()
    {
        return collection.isEmpty();
    }

    @Override
    public @Nonnull Class<? extends DiagramElement> getDataElementType()
    {
        return Compartment.class;
    }

    @Override
    public boolean isMutable()
    {
        return collection.isMutable();
    }

    @Override
    public boolean contains(String name)
    {
        return collection.contains(name);
    }

    @Override
    public boolean contains(DataElement de)
    {
        return contains(de.getName());
    }

    @Override
    public @Nonnull Iterator<DiagramElement> iterator()
    {
        return collection.iterator();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return collection.getNameList();
    }

    @Override
    public DiagramElement get(String name)
    {
        return collection.get(name);
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        return collection.getDescriptor(name);
    }

    @Override
    public DiagramElement put(DiagramElement obj) throws DataElementPutException
    {
        if( obj.getName() == null || obj.getName().isEmpty() )
            throw new DataElementPutException(new Exception("Element has empty name"), getCompletePath().getChildPath(""));
        if( collection.get( obj.getName() ) == obj )
            return obj;
        putKernel(obj, obj.getKernel());
        if( obj instanceof Edge )
        {
            Edge edge = (Edge)obj;
            edge.nodes().forEach( n -> n.addEdge( edge ) );
        }
        try
        {
            DiagramElement de = collection.put( obj );
            Diagram d = Diagram.getDiagram( this );
            if( d != null )
            {
                if( obj instanceof Compartment )
                    d.register( obj.recursiveStream().map( n -> n.getName() ).toSet() ); //here may be slow
                else
                    d.register( obj.getName() );
            }
            return de;
        }
        catch( DataElementPutException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new DataElementPutException( e, getCompletePath().getChildPath( obj.getName() ) );
        }

    }

    private void putKernel(DiagramElement obj, Base kernel)
    {
        if( kernel != null )
        {
            List<DiagramElement> values = kernelMap.get(kernel);
            if( values == null )
            {   // To save memory, singleton map for 1:1 kernel->node mapping used as it appears very often
                kernelMap.put(kernel, Collections.singletonList(obj));
            } else
            {
                if(!(values instanceof ArrayList))
                {
                    values = new ArrayList<>(values);
                    kernelMap.put(kernel, values);
                }
                values.add(obj);
            }
        }
    }

    @Override
    public void remove(String name) throws Exception
    {
        if(name == null)
            return;

        DiagramElement de = collection.get(name);
        if( de instanceof Edge )
        {
            Edge edge = (Edge)de;
            removeKernel(de, edge.getKernel());
            edge.nodes().forEach( n -> n.removeEdge( edge ) );
        }

        if( de instanceof Node )
        {
            removeKernel(de, ( (Node)de ).getKernel());
        }

        Diagram d = Diagram.getDiagram( this );
        if( d != null )
        {
            if( de instanceof Compartment )
                d.deregister( de.recursiveStream().map( el -> el.getName() ).toSet() );
            else
                d.deregister( name );
        }
        collection.remove(name);
    }

    private void removeKernel(DiagramElement obj, Base kernel)
    {
        if( kernel != null )
        {
            List<DiagramElement> values = kernelMap.get(kernel);
            if( values == null )
                log.warning("KernelMap error");
            else
            {
                if(values.size() == 1 && values.contains(obj))
                    kernelMap.remove(kernel);
                else
                    values.remove(obj);
            }
        }
    }

    /** @pending replace */
    @Override
    public void addDataCollectionListener(DataCollectionListener l)
    {
        collection.addDataCollectionListener(l);
    }

    /** @pending replace */
    @Override
    public void removeDataCollectionListener(DataCollectionListener l)
    {
        collection.removeDataCollectionListener(l);
    }

    @Override
    public @Nonnull DataElementPath getCompletePath()
    {
        if( completeName == null )
        {
            DataCollection<?> origin = getOrigin();
            completeName = ( origin == null ? DataElementPath.EMPTY_PATH : origin.getCompletePath() ).getChildPath(getName());
        }
        return completeName;
    }

    @Override
    public void setOrigin(DataCollection origin)
    {
        super.setOrigin( origin );
        completeName = null;
    }

    @Override
    public void close() throws Exception
    {
        collection.close();
    }

    /** Do nothing because cache is not used. */
    @Override
    public void release(String dataElementName)
    {
    }

    /** Return null because cache is not used. */
    @Override
    public DataElement getFromCache(String dataElementName)
    {
        return null;
    }

    /**
     * Enable/disable events notification of two types:
     * 1) PropertyChangeEvent (inherited from Option)
     * 2) DataCollectionEvent (encapsulated ru.biosoft.access.core.DataCollection)
     */
    @Override
    public void setNotificationEnabled(boolean notificationEnabled)
    {
        super.setNotificationEnabled(notificationEnabled);
        collection.setNotificationEnabled(notificationEnabled);
    }

    /**
     * Enable/disable events propagation of two types:
     * 1) PropertyChangeEvent (inherited from Option)
     * 2) DataCollectionEvent (encapsulated ru.biosoft.access.core.DataCollection)
     */
    @Override
    public void setPropagationEnabled(boolean propagationEnabled)
    {
        super.setPropagationEnabled(propagationEnabled);
        collection.setPropagationEnabled(propagationEnabled);
    }

    /**
     * @todo comment
     */
    @Override
    public void propagateElementWillChange(DataCollection<?> source, DataCollectionEvent primaryEvent)
    {
        collection.propagateElementWillChange(source, primaryEvent);
    }

    /**
     * @todo comment
     */
    @Override
    public void propagateElementChanged(DataCollection<?> source, DataCollectionEvent primaryEvent)
    {
        collection.propagateElementChanged(source, primaryEvent);
    }


    /**
     * Clone this compartment for new parent with new name and new kernel
     */
    @Override
    public @Nonnull Compartment clone(Compartment newParent, String newName, Base newKernel) throws IllegalArgumentException
    {
        if( newParent == this )
            throw new IllegalArgumentException("Can not clone compartment into itself, compartment=" + newParent);

        Compartment compartment = new Compartment(newParent, newName, newKernel);
        compartment.setNotificationEnabled(false);
        doClone(compartment);
        compartment.setNotificationEnabled(isNotificationEnabled());
        return compartment;
    }

    /**
     * Clone this compartment for new parent with new name
     */
    @Override
    public @Nonnull Compartment clone(Compartment newParent, String newName) throws IllegalArgumentException
    {
        return clone(newParent, newName, getKernel());
    }

    /**
     * @pending clone edge properties (inPort, outPort, ...)
     */
    protected void doClone(Compartment compartment)
    {
        try
        {
            compartment.setShapeType(getShapeType());
            compartment.setShapeSize(getShapeSize());
            compartment.setVisible(isVisible());

            DiagramType type = Diagram.getDiagram( compartment ).getType();

            // clone internal nodes
            stream(Node.class).map( node -> node.clone( compartment, node.getName(), type.needCloneKernel( node.getKernel() ) ) )
                    .forEach( compartment::put );

            // clone internal edges
            stream( Edge.class ).map( edge -> edge.clone( compartment, edge.getName(), type.needCloneKernel( edge.getKernel() ) ) )
                    .forEach( compartment::put );

            super.doClone(compartment);
        }
        catch( Exception exc )
        {
            log.log(Level.SEVERE, "Cloning compartment error", exc);
        }
    }

    /**
     * Utility function - recursively check if this compartment
     * (or sub-compartments) have node with name
     */
    public static DiagramElement findNode(Compartment compartment, String name)
    {
        return compartment.recursiveStream().select( Compartment.class ).map( c -> c.get( name ) ).nonNull().findFirst().orElse( null );
    }

    public Node[] getNodes()
    {
        return stream(Node.class).toArray( Node[]::new );
    }

    public @Nonnull List<Node> getNodesWithKernel(Base kernel)
    {
        return stream(Node.class).filter( n -> {
            Base k = n.getKernel();
            return ( k != null && k.getCompletePath().equals( kernel.getCompletePath() ) );
        } ).collect( Collectors.toList() );
    }

    public boolean isShapeTypeHidden()
    {
        return !(getKernel() instanceof biouml.standard.type.Compartment);
    }

    public String[] getAvailableShapes()
    {
        return new String[]{"rectangle", "round rectangle", "ellipse"};
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        return DiagramElement.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean isValid()
    {
        return collection.isValid();
    }

    public void visitEdges(Consumer<Edge> visitor)
    {
        for(DiagramElement de : this)
        {
            if(de instanceof Compartment)
            {
                ((Compartment)de).visitEdges( visitor );
            } else if(de instanceof Edge)
            {
                visitor.accept( (Edge)de );
            }
        }
    }

    @Override
    public void reinitialize() throws LoggedException
    {
        collection.reinitialize();
    }

    public void clear()
    {
        for( String name : collection.names().collect( Collectors.toList() ) )
        {
            try
            {
                remove(name);
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
        }
    }

    //Methods override DataCollection for more convenient with of StreamEx
    public StreamEx<DiagramElement> stream()
    {
        if( this.isEmpty() )
            return StreamEx.empty();

        return StreamEx.of( names() ).map( name -> {
            try
            {
                return get( name );
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
        } );
    }

    public <TT extends DiagramElement> StreamEx<TT> stream(Class<TT> elementClass)
    {
        return stream().select( elementClass );
    }
}
