package ru.biosoft.access._test;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.repository.IconFactory;

/**
 * @author lan
 *
 */
public class VectorDataCollectionTest extends DataCollectionTest
{
    @Override
    protected int getOriginalSize()
    {
        return 10;
    }

    @Override
    protected String getOriginalName()
    {
        return "test";
    }

    @Override
    protected void setUp() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        for(int i=0; i<10; i++)
        {
            vdc.put(new TextDataElement(String.valueOf(i), vdc));
        }
        setDataCollection(vdc);
    }
    
    public void testToArray()
    {
        TextDataElement[] array = (TextDataElement[])((VectorDataCollection)dataCollection).toArray(new TextDataElement[10]);
        for(int i=0; i<10; i++)
        {
            assertEquals(String.valueOf(i), array[i].getName());
        }
    }
    
    public void testDescriptor()
    {
        assertNull(dataCollection.getDescriptor("invalid"));
        assertTrue(dataCollection.getDescriptor("0").isLeaf());
        assertEquals(TextDataElement.class, dataCollection.getDescriptor("1").getType());
        assertEquals(IconFactory.getClassIconId(TextDataElement.class), dataCollection.getDescriptor("2").getIconId());
    }
}
