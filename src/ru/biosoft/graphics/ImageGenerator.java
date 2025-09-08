package ru.biosoft.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

/**
 * Utility class to generate images.
 */
public class ImageGenerator
{
    protected static final Logger log = Logger.getLogger(ImageGenerator.class.getName());

    public static BufferedImage generateImage(View view, double scale, boolean antiAliasing)
    {
        return generateImage(view, scale, antiAliasing, false);
    }

    public static BufferedImage generateImage(View view, double scale, boolean antiAliasing, boolean isTransparent)
    {
        Rectangle r = view.getBounds();
        int width = (int)Math.ceil((r.width + 2 * r.x)*scale);
        int height = (int)Math.ceil((r.height + 2 * r.y)*scale);
        BufferedImage image = new BufferedImage(width, height, isTransparent ? BufferedImage.TYPE_INT_ARGB: BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);
        if(antiAliasing)
        {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        if( isTransparent )
            graphics.setColor(new Color(1, 1, 1, 0.f));
        else
            graphics.setColor(Color.white);
        graphics.fill(new Rectangle(0, 0, width, height));
        graphics.setTransform(at);
        view.paint(graphics);

        return image;
    }

    public static void encodeImage(BufferedImage image, String format, OutputStream out) throws Exception
    {
        encodeImage( image, format, null, out );
    }

    public static void encodeImage(BufferedImage image, String format, IIOMetadata metadata, OutputStream out) throws Exception
    {
        try
        {
            //TODO: refactor code duplication with ru.biosoft.access.support.FileImageTransformer
            if( metadata != null )
            {
                ImageWriter writer = ImageIO.getImageWritersByFormatName( format ).next();
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                try (ImageOutputStream stream = ImageIO.createImageOutputStream( out ))
                {
                    writer.setOutput( stream );
                    writer.write( metadata, new IIOImage( image, null, metadata ), writeParam );
                }
            }
            else
                ImageIO.write( image, format, out );
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Error during image encoding", t);
        }
        finally
        {
            if( out != null )
                out.close();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Image map generation issues
    //

    public interface ReferenceGenerator
    {
        /**
         * Generates href for the specified object
         */
        public String getReference(Object obj);

        /**
         * Get target for link
         */
        public String getTarget(Object obj);

        /**
         * Get alt/title for map element
         */
        public String getTitle(Object obj);
    }


    public static String generateImageMap(View view, ReferenceGenerator generator)
    {
        return generateImageMap(view, generator, 0);
    }

    public static String generateImageMap(View view, ReferenceGenerator generator, float zoomFactor)
    {
        view.setLocation(10, 10);

        StringBuffer result = new StringBuffer();
        generateImageMap(view, generator, result, zoomFactor);

        return result.toString();
    }

    protected static void generateImageMap(View view, ReferenceGenerator generator, StringBuffer result, float zoomFactor)
    {
        if( view instanceof CompositeView )
        {
            CompositeView composite = (CompositeView)view;

            for( int i = composite.size() - 1; i >= 0; i-- )
                generateImageMap(composite.elementAt(i), generator, result, zoomFactor);
        }

        if( view.isActive() )
        {
            String ref = generator.getReference(view.getModel());
            if( ref != null )
            {
                Rectangle bounds = view.getBounds();
                bounds.x -= 3;
                bounds.y -= 3;
                bounds.width += 6;
                bounds.height += 6;

                // scale bounds
                if( zoomFactor != 0 )
                {
                    bounds.x = (int) ( bounds.x * zoomFactor );
                    bounds.y = (int) ( bounds.y * zoomFactor );
                    bounds.width = (int) ( bounds.width * zoomFactor );
                    bounds.height = (int) ( bounds.height * zoomFactor );
                }

                //  <area shape="rect" coords="{$left},{$top},{$right},{$bottom}" href="{$transpath_get_cgi}?{$id}" target="desc_frame"/>
                result.append("<area shape=\"rect\"");
                result.append(" coords=\"" + bounds.x + "," + bounds.y + "," + ( bounds.x + bounds.width ) + ","
                        + ( bounds.y + bounds.height ) + "\" ");
                result.append("href=\"");
                result.append(ref);
                result.append("\" target=\"");
                result.append(generator.getTarget(view.getModel()));
                result.append("\"");

                String title = generator.getTitle(view.getModel());
                if( title != null )
                {
                    result.append(" title=\"");
                    result.append(title);
                    result.append("\"");
                }

                result.append("/>\n");
            }
        }
    }


    public static BufferedImage toBufferedImage(Image image)
    {
        if( image instanceof BufferedImage )
        {
            return (BufferedImage)image;
        }

        // Create a buffered image with a format that's compatible with the screen
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