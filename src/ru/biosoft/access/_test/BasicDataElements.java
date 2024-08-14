package ru.biosoft.access._test;

import java.awt.image.BufferedImage;

import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.TextDataElement;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class BasicDataElements extends TestCase
{
    public void testTextDataElement()
    {
        TextDataElement text1 = new TextDataElement("text", null);
        assertNull(text1.getContent());
        TextDataElement text2 = new TextDataElement("text", null, "some text");
        assertEquals("some text", text2.getContent());
        text2.setContent("new text");
        assertEquals("new text", text2.getContent());
        assertEquals(8, text2.getContentLength());
    }

    public void testHtmlDataElement()
    {
        HtmlDataElement html = new HtmlDataElement("html", null, "<b>Hello!</b>");
        assertEquals("<b>Hello!</b>", html.getContent());
        html.setContent("<b>Bye!</b>");
        assertEquals("<b>Bye!</b>", html.getContent());
    }
    
    public void testImageDataElement()
    {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ImageDataElement ide = new ImageDataElement("image", null, image);
        assertEquals(image, ide.getImage(null));
        assertEquals("PNG", ide.getFormat());
        ide = new ImageDataElement("image", null, image, "GIF");
        assertEquals("GIF", ide.getFormat());
        ide.setFormat("PNG");
        assertEquals("PNG", ide.getFormat());
    }
}
