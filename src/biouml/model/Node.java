package biouml.model;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Base;
import biouml.standard.type.ImageDescriptor;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.graphics.View;
import ru.biosoft.util.DimensionEx;

/**
 * Diagram node element
 */
@SuppressWarnings ( "serial" )
@ClassIcon ( "resources/node.gif" )
@PropertyName ( "Node" )
@PropertyDescription("Diagram node.")
public class Node extends DiagramElement
{
    public final static String INNER_NODES_PORT_FINDER_ATTR = "innerNodesPortFinder";
    private boolean isVisible = true;
    protected Point location = new Point( 0, 0 );
    private double titleAngle = Double.NaN;
    private int titleOffset = 0;
    private View titleView;
    private boolean showTitle = true;
    private boolean useCustomImage = false;
    protected static final Logger log = Logger.getLogger( Node.class.getName() );
    
    /**
     * This property will be used only if {@link SemanticController} believes that
     * node is resizable. Generally Compartment shape is resizable by default.
     */
    private DimensionEx size = new DimensionEx(this);

    protected Dimension shapeSize;
    

    /** Arbitrary image can be associated with diagram node element. */
    protected ImageDescriptor image = null;
    

    private final Map<String, Edge> edges = new LinkedHashMap<>();
    
    public Node(DataCollection<?> parent, String name, Base kernel)
    {
        super( parent, name, kernel );
    }

    public Node(DataCollection<?> parent, Base kernel)
    {
        super( parent, kernel );
    }

    @PropertyName("Location")
    @PropertyDescription("Node location.")
    public @Nonnull Point getLocation()
    {
        return (Point)location.clone();
    }
    /** @pending verify new location */
    public void setLocation(Point location)
    {
        Point oldValue = this.location;
        this.location = (Point)location.clone();
        firePropertyChange( "location", oldValue, location.clone() );
    }
    public void setLocation(int x, int y)
    {
        setLocation( new Point( x, y ) );
    }

    public void setRelativeLocation(Diagram diagram, Point location)
    {
        if( diagram.getView() == null )
        {
            setLocation( location );
            return;
        }

        // calculate offset for some diagram node
        Rectangle rect = diagram.getView().getBounds();
        Point point = new Point();
        Node de = diagram.stream( Node.class ).without( this ).findFirst().orElse( null );
        if( de != null )
        {
            point = de.getLocation();
            if( de.getView() != null )
                rect = de.getView().getBounds();
        }

        setLocation( location.x + point.x - rect.x, location.y + point.y - rect.y );
    }

    @PropertyName("Shape Size")
    public @CheckForNull DimensionEx getShapeSize2()
    {
        return size;
    }

    public void setShapeSize2(DimensionEx size)
    {
        DimensionEx oldValue = this.size;
        this.size = size;
        this.shapeSize = size.getDimension();
        if( useCustomImage && image != null )
            image.updateScale();
        firePropertyChange( "shapeSize2", oldValue, this.size);
    }
    
    @PropertyName("Shape Size")
    public @Nonnull Dimension getShapeSize()
    {
        return size.getDimension();
    }

    public void setShapeSize(Dimension shapeSize)
    {
        this.size.setHeight(shapeSize.height);
        this.size.setWidth(shapeSize.width);
        if( useCustomImage && image != null )
            image.updateScale();
    }

    @PropertyName("Image")
    public @Nonnull ImageDescriptor getImage()
    {
        if( image == null )
        {
            image = new ImageDescriptor();
            image.setParent( this );
        }
        return image;
    }
    public void setImage(ImageDescriptor image)
    {
        ImageDescriptor oldValue = this.image;
        this.image = image;
        if( image != null )
            image.setParent( this );
        firePropertyChange( "image", oldValue, image );
    }

    public void addEdge(Edge edge)
    {
        if( !edges.containsKey( edge.getName() ) )
            edges.put( edge.getName(), edge );
    }

    public void removeEdge(Edge edge)
    {
        edges.remove( edge.getName() );
    }

    public @Nonnull Edge[] getEdges()
    {
        return edges.values().toArray( new Edge[edges.size()] );
    }

    public @Nonnull StreamEx<Edge> edges()
    {
        return StreamEx.ofValues( edges );
    }
    public @Nonnull Node clone(Compartment newParent, String newName, boolean cloneKernel)
    {
        Base kernel = getKernel();
        try
        {
            if( cloneKernel && getKernel() instanceof CloneableDataElement )
                kernel = (Base) ( (CloneableDataElement)kernel ).clone( kernel.getOrigin(), newName );
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Cloning node error", ex );
        }
        return clone( newParent, newName, kernel );
    }

    public @Nonnull Node clone(Compartment newParent, String newName, Base newKernel)
    {
        Node node = new Node( newParent, newName, newKernel );
        node.setPropagationEnabled( false );
        doClone( node );
        node.setView( view );
        node.setPropagationEnabled( isPropagationEnabled() );
        return node;
    }

    @Override
    public @Nonnull Node clone(Compartment newParent, String newName)
    {
        return clone( newParent, newName, getKernel() );
    }

    protected void doClone(Node node)
    {
        node.setTitle( getTitle() );

        if( role != null )
        {
            Role role = this.role.clone( node );
            node.setRole( role );
        }

        if( attributes != null )
        {
            Iterator<String> iter = attributes.nameIterator();
            while( iter.hasNext() )
            {
                DynamicProperty oldProp = attributes.getProperty( iter.next() );
                DynamicProperty prop = null;
                try
                {
                    prop = DynamicPropertySetSupport.cloneProperty( oldProp );
                }
                catch( Exception e )
                {
                    prop = oldProp;
                }
                node.getAttributes().add( prop );
            }
        }

        node.setComment( getComment() );
        node.setUseCustomImage(isUseCustomImage());
        if( node.isUseCustomImage() )
            node.setImage(getImage().clone());
        node.setLocation( getLocation() );
        node.setShapeSize( getShapeSize() );
        node.setFixed( isFixed() );
        node.setVisible(isVisible());
        node.setShowTitle(isShowTitle());
        node.setPredefinedStyle(getPredefinedStyle());
        if( node.getPredefinedStyle().equals(DiagramElementStyle.STYLE_NOT_SELECTED) )
            node.setCustomStyle(getCustomStyle().clone());
    }

    public static Compartment findCommonOrigin(@Nonnull DiagramElement first, @Nonnull DiagramElement second)
    {
        if( first.getOrigin() == null || second.getOrigin() == null )
            return null;
        DataCollection<?> origin = first.getOrigin();
        DataElementPath secondPath = second.getOrigin().getCompletePath();
        while( origin != null )
        {
            DataElementPath path = origin.getCompletePath();
            if( secondPath.isDescendantOf( path ) )
                break;

            origin = origin.getOrigin();
        }
        return (Compartment)origin;
    }

    public boolean isNotResizable()
    {
        Diagram diagram = Diagram.optDiagram( this );
        return diagram == null || !diagram.getType().getSemanticController().isResizable( this );
    }

    public boolean isImageHidden()
    {
        return Diagram.optDiagram(this).getType().getDiagramViewBuilder().forbidCustomImage( this );
    }
    
    public double getTitleAngle()
    {
        return titleAngle;
    }
    public void setTitleAngle(double titleAngle)
    {
        double oldValue = this.titleAngle;
        this.titleAngle = titleAngle;
        firePropertyChange( "titleAngle", oldValue, titleAngle );
    }

    public int getTitleOffset()
    {
        return titleOffset;
    }
    public void setTitleOffset(int titleOffset)
    {
        int oldValue = this.titleOffset;
        this.titleOffset = titleOffset;
        firePropertyChange( "titleOffset", oldValue, titleOffset );
    }

    public View getTitleView()
    {
        return titleView;
    }
    public void setTitleView(View titleView)
    {
        this.titleView = titleView;
    }

    @PropertyName("Visible")
    @PropertyDescription("Visible.")
    public boolean isVisible()
    {
        return isVisible;
    }
    public void setVisible(boolean isVisible)
    {
        boolean oldValue = isVisible();
        this.isVisible = isVisible;
        firePropertyChange( "isVisible", oldValue, isVisible );
    }
    
    @PropertyName("Show title")
    @PropertyDescription("Show title.")
    public boolean isShowTitle()
    {
        return showTitle;
    }
    public void setShowTitle(boolean showTitle)
    {
        boolean oldValue = isShowTitle();
        this.showTitle = showTitle;
        firePropertyChange( "showTitle", oldValue, showTitle );
    }
    
    @Override
    protected boolean shouldFire(PropertyChangeEvent evt)
    {
        if( evt.getPropagationId() == getImage() || evt.getPropagationId() == getShapeSize2() || evt.getPropagationId() == getCustomStyle() )
            return true;
        return super.shouldFire(evt);
    }
    
    public boolean useDefaultView()
    {
        return !isUseCustomImage();
    }

    @PropertyName("Use custom image")
    public boolean isUseCustomImage()
    {
        return useCustomImage;
    }
    public void setUseCustomImage(boolean useCustomImage)
    {
        if( this.useCustomImage == useCustomImage )
            return;
        boolean oldValue = this.useCustomImage;
        this.useCustomImage = useCustomImage;
        this.setImage(null);
        firePropertyChange( "useCustomImage", oldValue, useCustomImage );
    }
}
