package ru.biosoft.access._test;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * @author lan
 *
 */
public class DataCollectionUtilsTest extends TestCase
{
    public void testGetClassTitle() throws Exception
    {
        assertEquals("element", DataCollectionUtils.getClassTitle((Class<? extends DataElement>)null));
        assertEquals("element", DataCollectionUtils.getClassTitle(DataElement.class));
        assertEquals("text", DataCollectionUtils.getClassTitle(TextDataElement.class));
        assertEquals("collection", DataCollectionUtils.getClassTitle(VectorDataCollection.class));
        // check whether cache works correctly
        assertEquals("collection", DataCollectionUtils.getClassTitle(VectorDataCollection.class));
        assertEquals("collection", DataCollectionUtils.getClassTitle(SqlDataCollection.class));
        assertEquals("image", DataCollectionUtils.getClassTitle(new ImageDataElement("", null, new BufferedImage( 10, 10, BufferedImage.TYPE_INT_ARGB ))));
    }
    
    public void testAsCollection() throws Exception
    {
        VectorDataCollection<TextDataElement> vdc = new VectorDataCollection<>("test", TextDataElement.class, null);
        vdc.put(new TextDataElement("test1", vdc));
        vdc.put(new TextDataElement("test2", vdc));
        vdc.put(new TextDataElement("test3", vdc));
        Collection<TextDataElement> c = DataCollectionUtils.asCollection(vdc, TextDataElement.class);
        assertFalse(c.isEmpty());
        assertEquals(3, c.size());
        Iterator<TextDataElement> iterator = c.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("test1", iterator.next().getName());
        assertTrue(iterator.hasNext());
        assertEquals("test2", iterator.next().getName());
        assertTrue(iterator.hasNext());
        assertEquals("test3", iterator.next().getName());
        assertFalse(iterator.hasNext());
    }
    
    public void testIsLeaf() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        vdc.put(new TextDataElement("test1", vdc));
        assertTrue(DataCollectionUtils.isLeaf(DataElementPath.create("test/test1")));
        assertFalse(DataCollectionUtils.isLeaf(DataElementPath.create("test")));
        assertFalse(DataCollectionUtils.isLeaf(DataElementPath.create("blahblah")));
    }
}
