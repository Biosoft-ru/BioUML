package ru.biosoft.server.servlets.webservices._test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import javax.imageio.ImageIO;

import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.providers.ContentProvider;

/**
 * @author lan
 *
 */
public class TestContentProvider extends AbstractProviderTest
{
    public void testContentProvider() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        TextDataElement text = new TextDataElement("text", vdc, "text content");
        TextDataElement css = new TextDataElement("test.css", vdc, "b {color: blue;}");
        HtmlDataElement html = new HtmlDataElement("index.html", vdc, "<b>Html content</b>");
        ImageDataElement image = new ImageDataElement("image", vdc, new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB));
        vdc.put(text);
        vdc.put(css);
        vdc.put(html);
        vdc.put(image);
        
        assertEquals("text content", getResponseString("content/test/text", Collections.<String,String>emptyMap()));
        assertLastContentType("text/plain");
        
        assertEquals("<b>Html content</b>", getResponseString("content/test/", Collections.<String,String>emptyMap()));
        assertLastContentType("text/html");
        
        assertEquals("b {color: blue;}", getResponseString("content/test/test.css", Collections.<String,String>emptyMap()));
        assertLastContentType("text/css");
        
        assertEquals("<html><body><h1>Error</h1><p>Element not found: test/unknown</p></body></html>",
                getResponseString("content/test/unknown", Collections.<String,String>emptyMap()).trim());
        assertLastContentType("text/html");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpResponseStub response = new HttpResponseStub();
        new ContentProvider().process(new BiosoftWebRequest(Collections.singletonMap(BiosoftWebRequest.ACTION, "test/image")),
                new BiosoftWebResponse(response, out));
        assertEquals("image/png", response.getContentType());
        BufferedImage readImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(20, readImage.getWidth());
        assertEquals(20, readImage.getHeight());
    }
}
