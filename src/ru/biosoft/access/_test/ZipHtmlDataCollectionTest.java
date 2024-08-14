package ru.biosoft.access._test;

import java.util.Arrays;
import java.util.HashSet;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.html.ZipHtmlDataCollection;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class ZipHtmlDataCollectionTest extends TestCase
{
    public void testZipHtmlDataCollection() throws Exception
    {
        DataCollection<?> root = CollectionFactory.createRepository("../data/test/ru/biosoft/access/zhtml");
        assertNotNull(root);
        ZipHtmlDataCollection dc = DataElementPath.create("zhtml/zhtml").getDataElement(ZipHtmlDataCollection.class);
        HtmlDataElement html = DataElementPath.create("zhtml/zhtml/index.html").getDataElement(HtmlDataElement.class);
        assertEquals("<h1>Test</h1>", html.getContent().trim());
        TextDataElement css = DataElementPath.create("zhtml/zhtml/test.css").getDataElement(TextDataElement.class);
        assertEquals("h1 {color: blue;}", css.getContent().trim());
        html = DataElementPath.create("zhtml/zhtml/dir/test.html").getDataElement(HtmlDataElement.class);
        assertEquals("<h1>Hello world!</h1>", html.getContent().trim());
        ImageDataElement img = DataElementPath.create("zhtml/zhtml/html.gif").getDataElement(ImageDataElement.class);
        assertEquals(16, img.getImageSize().width);
        assertEquals(16, img.getImage(null).getWidth());
        assertEquals(16, img.getImage(null).getHeight());
        
        HashSet<String> names = new HashSet<>(Arrays.asList("index.html", "test.css", "dir", "html.gif"));
        assertEquals(names, new HashSet<>(dc.getNameList()));
        assertEquals(names.size(), dc.getSize());
        for(DataElement de: dc)
        {
            assertTrue(names.contains(de.getName()));
        }
        dc.close();
    }
}
