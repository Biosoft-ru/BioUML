package ru.biosoft.access;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import com.developmentontheedge.application.ApplicationUtils;

public class FileImagesDataCollection extends VectorDataCollection
{
    protected final static Component component = new Component()
    {
    };

    protected final static MediaTracker tracker = new MediaTracker(component);
    public final static String FILTER = "bmp jpg jpeg gif png";

    /** Property for storing filter file extension  */
    public static final String FILE_FILTER = "fileFilter";

    /** Subdirectory corresponded with this collection. */
    private final File root;

    public FileImagesDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        String file = properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
        if( file == null )
            file = properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
        root = new File(file);
        init(properties);
    }

    /**
     * Initialize file collection from the properties.
     * @throws RuntimeException If error occurs.
     */
    protected void init(Properties properties)
    {
        FileFilter filter = new SimpleFileFilter(properties.getProperty(FILE_FILTER));
        File[] files = root.listFiles(filter);
        if( files != null )
        {
            try
            {
                for( File file : files )
                {
                    if( filter.accept(file) )
                    {
                        super.doPut(new FileDataElement(file.getName(), this, file), true);
                        getInfo().addUsedFile(file);
                    }
                }
            }
            catch( Exception exc )
            {
                log.log(Level.SEVERE, "Error during file collection creating root=" + root, exc);
                throw new RuntimeException("FileCollection failed root=" + root, exc);
            }
        }
    }

    /**
     * Returns <code>FileDataElement.class</code>
     *
     * @return <code>FileDataElement.class</code>
     */
    @Override
    public @Nonnull Class<FileDataElement> getDataElementType()
    {
        return FileDataElement.class;
    }

    public File getFile()
    {
        return root;
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        File file = new File(root, name);
        //if (!((FileDataElement)de).getFile().delete())
        if( !file.delete() )
            throw new Exception("File can not be destroyed: " + file.getAbsolutePath());
        super.doRemove(name);
    }

    public String getRoot()
    {
        return root.getPath().substring(1);
    }

    @Override
    protected DataElement doGet(String name)
    {
        String absolutePath = System.getProperty("user.dir") + getRoot() + System.getProperty("file.separator") + name;
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.getImage(absolutePath);
        synchronized( tracker )
        {
            tracker.addImage(image, 0);
            try
            {
                tracker.waitForID(0, 0);
            }
            catch( InterruptedException e )
            {
                System.out.println("INTERRUPTED while loading Image");
            }
            boolean error = tracker.isErrorID(0);
            tracker.removeImage(image, 0);
            if( error )
            {
                return null;
            }
        }
        try
        {
            return new ImageDataElement( name, this, getBufferedImageFromImage( image ) );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    protected void doPut(DataElement obj, boolean isNew)
    {
        ImageDataElement ide = (ImageDataElement)obj;
        try
        {
            File dst = new File(System.getProperty("user.dir") + getRoot() + System.getProperty("file.separator") + ide.getName());
            if( dst.exists() )
            {
                log.log(Level.SEVERE, "Picture already exists");
                return;
            }

            ByteArrayOutputStream outImage = new ByteArrayOutputStream();
            ImageIO.write(ide.getImage(null), "PNG", outImage);
            byte[] imageBytes = outImage.toByteArray();
            ByteArrayInputStream in = new ByteArrayInputStream(imageBytes);
            OutputStream out = new FileOutputStream(dst);
            ApplicationUtils.copyStream(out, in);
            super.doPut(ide, isNew);
        }
        catch( IOException ex )
        {
            log.log(Level.SEVERE, "Import image error", ex);
        }
    }

    private BufferedImage getBufferedImageFromImage(Image image) throws Exception
    {
        synchronized( tracker )
        {
            tracker.addImage(image, 0);
            try
            {
                tracker.waitForID(0, 0);
            }
            catch( InterruptedException e )
            {
                System.out.println("INTERRUPTED while loading Image");
            }
            boolean error = tracker.isErrorID(0);
            Object[] errors = tracker.getErrorsID( 0 );
            tracker.removeImage(image, 0);
            if( error )
            {
                throw new Exception( "Error during get buffered image occurred" + StreamEx.of( errors ).joining() );
            }
        }
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try
        {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
        }
        catch( HeadlessException e )
        {
            // The system does not have a screen
        }

        if( bimage == null )
        {
            int type = BufferedImage.TYPE_INT_RGB;
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        Graphics g = bimage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bimage;
    }
}
