package ru.biosoft.access._test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import javax.imageio.ImageIO;

import junit.framework.TestCase;
import ru.biosoft.access.ImageConnection;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * @author lan
 *
 */
public class ImageConnectionTest extends TestCase
{
    public static class TestURLStreamHandlerFactory implements URLStreamHandlerFactory
    {
        @Override
        public URLStreamHandler createURLStreamHandler(String protocol)
        {
            if(protocol.equals("image"))
            return new URLStreamHandler()
            {
                @Override
                protected URLConnection openConnection(URL u) throws IOException
                {
                    return new ImageConnection(u);
                }
            };
            return null;
        }
    }
    
    public void testImageConnection() throws Exception
    {
        VectorDataCollection<ImageElement> vdc = new VectorDataCollection<>( "test" );
        CollectionFactory.registerRoot(vdc);
        BufferedImage testImage = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        vdc.put(new ImageDataElement("image", vdc, testImage));
        URL.setURLStreamHandlerFactory(new TestURLStreamHandlerFactory());
        URLConnection connection = new URL("image:///test/image").openConnection();
        assertEquals("image/png", connection.getContentType());
        BufferedImage readImage = ImageIO.read(connection.getInputStream());
        assertEquals(testImage.getWidth(), readImage.getWidth());
        assertEquals(testImage.getHeight(), readImage.getHeight());
    }
}
