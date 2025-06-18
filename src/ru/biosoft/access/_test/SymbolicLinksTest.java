package ru.biosoft.access._test;

import junit.framework.TestCase;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.SymbolicLinkDataCollection;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * @author lan
 *
 */
public class SymbolicLinksTest extends TestCase
{
    //private final int eventCount = 0;
    
    public void testSymbolicLinks() throws Exception
    {
        VectorDataCollection<DataElement> test = new VectorDataCollection<>("test");
        VectorDataCollection<DataElement> test2 = new VectorDataCollection<>("test2");
        CollectionFactory.registerRoot(test);
        CollectionFactory.registerRoot(test2);
        test.put(new TextDataElement("text", test, "some text"));
        test2.put(new SymbolicLinkDataCollection(test2, "link", DataElementPath.create(test)));
        assertEquals("some text", ((TextDataElement)CollectionFactory.getDataElement("test2/link/text")).getContent());
        assertEquals("some text", ((TextDataElement)DataElementPath.create("test2/link/text").getTargetElement()).getContent());
        assertEquals("test/text", DataElementPath.create("test2/link/text").getTargetPath().toString());
        // TODO: check events
        /*test2.addDataCollectionListener(new DataCollectionListenerSupport()
        {
            @Override
            public void elementChanged(DataCollectionEvent e) throws Exception
            {
                eventCount++;
            }
        });
        test.put(new TextDataElement("text2", test, "some text 2"));
        assertEquals(1, eventCount);*/
    }
}
