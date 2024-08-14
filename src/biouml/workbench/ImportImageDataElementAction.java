package biouml.workbench;

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
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.TextUtil;
import biouml.workbench.resources.MessageBundle;

import com.developmentontheedge.application.Application;

public class ImportImageDataElementAction extends AbstractAction
{
    public static final String KEY = "Import Image Data Element";

    public static final String IMAGES_COLLECTION = "Images data collection";

    private static final MessageBundle mb = BioUMLApplication.getMessageBundle();

    protected final static Component component = new Component()
    {
    };

    protected final static MediaTracker tracker = new MediaTracker(component);

    public ImportImageDataElementAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object o = getValue(IMAGES_COLLECTION);
        if( ! ( o instanceof DataCollection ) )
            return;
        DataCollection dc = (DataCollection)o;

        if( !dc.isMutable() )
        {
            String message = mb.getResourceString("ERROR_CANNOT_IMPORT");
            message = MessageFormat.format(message, new Object[] {dc.getCompletePath()});
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), message);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                if( file.isDirectory() )
                    return true;
                if( file.isFile() && null != file.getName() )
                {
                    String fileName = file.getName().toLowerCase();
                    String extensions[] = TextUtil.split( mb.getResourceString("PIC_EXTENSIONS"), ' ' );
                    for( String extension : extensions )
                    {
                        if( fileName.endsWith(extension) )
                            return true;
                    }
                }
                return false;
            }

            @Override
            public String getDescription()
            {
                return mb.getResourceString("PIC_EXTENSIONS_TITLE");
            }
        });
        chooser.setMultiSelectionEnabled(true);

        int res = chooser.showOpenDialog(Application.getApplicationFrame());
        if( res == JFileChooser.APPROVE_OPTION )
        {
            File files[] = chooser.getSelectedFiles();
            for( File file : files )
            {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                try
                {
                    BufferedImage image = getBufferedImageFromImage( toolkit.getImage( file.getPath() ) );
                    dc.put(new ImageDataElement(file.getName(), dc, image));
                }
                catch(Exception ex)
                {
                    ExceptionRegistry.log( ex );
                    //TODO: add error to log
                }
            }
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
                throw new Exception( "Error during get buffered image occurred: " + StreamEx.of( errors ).joining( "\n" ) );
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