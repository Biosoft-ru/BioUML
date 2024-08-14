package biouml.standard.type;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Module;
import biouml.model.Node;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.util.DimensionEx;
/**
 * @pending - lazy initialisation
 */
public class ImageDescriptor extends Option implements ImageObserver, PropertyChangeListener
{
    static final Dimension DEFAULT_ORIGINAL_SIZE = new Dimension( -1, -1);

    protected static final Logger log = Logger.getLogger(ImageDescriptor.class.getName());

    protected final static Component component = new Component()
    {
    };
    protected final static MediaTracker tracker = new MediaTracker(component);

    //TODO: probably unify path with source
    private DataElementPath path;
    protected String source;
    private boolean isParentChanging = false; //flag to indicate that size changes came from parent to avoid cycling with listener 

    private DimensionEx scale = new DimensionEx( this, 1, Integer.MAX_VALUE, 1, Integer.MAX_VALUE, 100, 100 );
    
    /** Creates empty image. */
    public ImageDescriptor()
    {
        scale.addPropertyChangeListener(this);
    }

    public ImageDescriptor(DataElementPath path)
    {
       this.path = path;
    }
    
    public ImageDescriptor(String source)
    {
        this(source, null);
    }

    public ImageDescriptor(String source, Dimension size)
    {
        this.source = source;

        this.size = size;
        if( this.size == null )
            this.size = new Dimension();
    }


    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    /**
     * Module instance. It is used to get ru.biosoft.access.core.DataElement (image data or structure data)
     * from the module DataCollections.
     */
    public Module getModule()
    {
        return Module.optModule((DataElement)getParent());
    }
    
    /**
     * The image source.
     * Generally it is name of ru.biosoft.access.core.DataElement (image or chemical structure)
     * stored in the module instance.
     */
    public String getSource()
    {
        return source;
    }
    public void setSource(String source)
    {
        String oldValue = this.source;
        this.source = source;

        // invalidate size
        initialised = false;
        initialised2 = false;
        setSize(new Dimension(0, 0));

        loadImage();

        firePropertyChange("source", oldValue, source);
    }

    /** The image size. */
    protected Dimension size = new Dimension();;
    public @Nonnull Dimension getSize()
    {
        return new Dimension(size);
    }
    public void setSize(@Nonnull Dimension size)
    {
        Assert.notNull( "size", size );

        Dimension oldSize = this.size;
        this.size = size;
        firePropertyChange("size", oldSize, size);
    }

    /** Original size of the image. */
    protected Dimension originalSize = new Dimension();
    @PropertyName("Original size")
    public Dimension getOriginalSize()
    {
        return new Dimension(size);
    }
    public void setOriginalSize(Dimension size)
    {
        //do nothing
    }

    protected Image image;
    public Image getImage()
    {
        loadImage();

        if( image == null )
            originalSize = DEFAULT_ORIGINAL_SIZE;
        return image;
    }

    protected CompositeView imageView = null;
    public CompositeView getImageView(Graphics2D g)
    {
        loadImageView(g);
        return imageView;
    }

    /**
     * Indicates how node title should be located relative the image. *
     protected titleAlignment
     */
    ////////////////////////////////////////////////////////////////////////////
    // Utility methods to get original image
    //
    protected int loadStatus = 0;
    protected boolean initialised = false;
    protected boolean initialised2 = false;


    /** Implements the image observer interface. */
    @Override
    public boolean imageUpdate(Image img, int info, int x, int y, int width, int height)
    {
        System.out.println("imageUpdate: " + info + ", image=" + img + " " + width + "x" + height);
        // stub
        if( info != ALLBITS )
            return true;

        return false;
    }

    /**
     * Loads the image, returning only when the image is loaded.
     * @param image the image
     */
    protected void loadImage()
    {
        if( initialised )
            return;
        
        initialised = true;
        
        if( path != null )
        {
            ImageDataElement ide = path.getDataElement(ImageDataElement.class);
            image = ide.getImage();
        }
        else if( source == null || source.length() == 0 )
        {
            image = null;
        }
        else if( source.contains("Data/structure/") )
        {
            try
            {
                Structure structure = null;
                if( source.startsWith("databases/") )
                {
                    structure = (Structure)CollectionFactory.getDataElement(source);
                }
                else
                {
                    structure = (Structure)CollectionFactory.getDataElement(source, getModule());
                }
                if( structure != null )
                    image = CDKRenderer.createStructureImage(structure, size);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not create structure view, source=" + source + ", error: " + t, t);
            }
        }
        else if( source.startsWith("Dictionaries/image/") )
        {
        }
        else
        {
            try
            {
                DataCollection idc = (DataCollection)getModule().get(Module.IMAGES);
                ImageDataElement ide = (ImageDataElement)idc.get(source);
                image = ide.getImage(null);
            }
            catch( Exception e )
            {
                image = null;
            }
        }

        // update the image size
        if( image != null ) // image is loaded successfully
        {
            int imageWidth = image.getWidth(this);
            int imageHeight = image.getHeight(this);

            if( DEFAULT_ORIGINAL_SIZE.equals(originalSize) )
            {
                size.width = imageWidth;
                size.height = imageHeight;
            }

            originalSize.width = imageWidth;
            originalSize.height = imageHeight;

            if (this.getParent() instanceof Node)
            {
                Dimension nodeSize = ((Node)getParent()).getShapeSize();
                scale.setWidth((int)(nodeSize.getWidth() / imageWidth * 100.0));
                scale.setHeight((int)(nodeSize.getHeight() / imageHeight * 100.0));
                ( (Node)getParent() ).setShapeSize( new Dimension( (int) ( imageWidth * scale.getWidth() / 100.0 ),
                        (int) ( imageHeight * scale.getHeight() / 100.0 ) ) );
            }
            
            
            firePropertyChange("originalSize", null, null);

            if( size.width == 0 || size.height == 0 )
            {
                size.setSize(originalSize);
                firePropertyChange("size", null, null);
            }
        }

    }

    protected void loadImageView(Graphics2D g)
    {
        if( initialised2 )
            return;

        initialised2 = true;

        if( source == null || source.length() == 0 )
        {
            imageView = null;
        }
        else if( source.contains("Data/structure/") )
        {
            try
            {
                Structure structure = null;
                if( source.startsWith("databases/") )
                {
                    structure = (Structure)CollectionFactory.getDataElement(source);
                }
                else
                {
                    structure = (Structure)CollectionFactory.getDataElement(source, getModule());
                }
                if( structure != null )
                {
                    imageView = CDKRenderer.createStructureView(structure, size, g);
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not create structure view, source=" + source + ", error: " + t, t);
            }
        }
    }

    public String[] getStructures(Module module)
    {
        List<String> structures = new ArrayList<>();
        try
        {
            DataElementPath basePath = module.getCompletePath().getChildPath("Data", "structure");
            for(DataElementPath structurePath: basePath.getChildren())
            {
                structures.add(structurePath.toString());
            }
        }
        catch( Exception e )
        {
            return null;
        }
        return structures.toArray(new String[structures.size()]);
    }

    public String[] getImages()
    {
        Module module = getModule();
        if( !module.contains(Module.IMAGES) )
        {
            return null;
        }
        try
        {
            return ( (DataCollection<?>)module.get(Module.IMAGES) ).names().toArray( String[]::new );
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public void addImage(BufferedImage image, String name)
    {
        try
        {
            DataCollection dc = (DataCollection)getModule().get(Module.IMAGES);
            if( dc != null )
            {
                dc.put(new ImageDataElement(name, dc, image));
            }
        }
        catch( Exception e )
        {
        }
    }
        
    @PropertyName("Image path")
    public DataElementPath getPath()
    {
        return path;
    }
    public void setPath(DataElementPath path)
    {
        this.initialised = false;
        Object oldValue = this.path;
        this.path = path;
        firePropertyChange("path", oldValue, path);
    }
    
    @Override
    public ImageDescriptor clone()
    {
        ImageDescriptor result = new ImageDescriptor();
        if( getPath() != null )
            result.setPath(getPath());
        result.setSize(getSize());
        if( getSource() != null )
            result.setSource(this.getSource());
        return result;
    }

    @PropertyName("Image scale")
    public DimensionEx getScale()
    {
        return scale;
    }
    public void setScale(DimensionEx scale)
    {
        Object oldValue = scale;
        this.scale = scale;
        firePropertyChange("scale", oldValue, scale);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( isParentChanging )
            return;
        if( evt.getSource() == getScale() && this.path != null )
        {
            if( this.getParent() instanceof Node )
            {
                if( evt.getPropertyName().equals("width") )
                {
                    int scale = (int)evt.getNewValue();
                    int nodeWidth = (int) ( getOriginalSize().width * scale / 100.0 );
                    ( (Node)getParent() ).getShapeSize2().setWidth(nodeWidth);
                }

                if( evt.getPropertyName().equals("height") )
                {
                    int scale = (int)evt.getNewValue();
                    int nodeHeight = (int) ( getOriginalSize().height * scale / 100.0 );
                    ( (Node)getParent() ).getShapeSize2().setHeight(nodeHeight);
                }
            }

        }
    }

    public void updateScale()
    {
        isParentChanging = true;
        Dimension imageSize = getOriginalSize();
        Dimension nodeSize = ( (Node)getParent() ).getShapeSize();
        scale.setWidth( (int) ( nodeSize.getWidth() / imageSize.getWidth() * 100.0 ) );
        scale.setHeight( (int) ( nodeSize.getHeight() / imageSize.getHeight() * 100.0 ) );
        isParentChanging = false;
    }
}
