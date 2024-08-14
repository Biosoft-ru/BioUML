package ru.biosoft.access._test;

import java.util.AbstractList;
import java.util.List;

import javax.annotation.Nonnull;

import junit.framework.TestCase;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;

/**
 * @author lan
 *
 */
public class FilteredDataCollectionTest extends TestCase
{
    private static class TestDataElement extends DataElementSupport
    {
        private final int value;
        
        public TestDataElement(String name, DataCollection<TestDataElement> origin, int value)
        {
            super(name, origin);
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
    }
    
    private static class TestDataCollection extends AbstractDataCollection<TestDataElement>
    {
        private final int length;
        
        public TestDataCollection(String name, DataCollection<?> origin, int length)
        {
            super(name, origin, null);
            this.length = length;
        }
        
        @Override
        public boolean contains(String name)
        {
            try
            {
                int num = Integer.parseInt(name);
                return num >= 0 && num < length;
            }
            catch( NumberFormatException e )
            {
                return false;
            }
        }

        @Override
        public @Nonnull List<String> getNameList()
        {
            return new AbstractList<String>()
            {
                @Override
                public String get(int index)
                {
                    return String.valueOf(index);
                }

                @Override
                public int size()
                {
                    return length;
                }

                @Override
                public int indexOf(Object o)
                {
                    try
                    {
                        int num = Integer.parseInt(o.toString());
                        return num >= 0 && num < length ? num : -1;
                    }
                    catch( NumberFormatException e )
                    {
                        return -1;
                    }
                }
            };
        }

        @Override
        public TestDataElement get(String name) throws Exception
        {
            if(!contains(name)) return null;
            return new TestDataElement(name, this, Integer.parseInt(name));
        }
    }
    
    public void testFilteredCollection() throws Exception
    {
        // Test TestDataCollection first
        DataCollection<TestDataElement> dc = new TestDataCollection("test", null, 100000);
        assertEquals(100000, dc.getSize());
        assertEquals(100000, dc.getNameList().size());
        assertEquals("123", dc.getNameList().get(123));
        TestDataElement de = dc.get("54321");
        assertNotNull(de);
        assertEquals(54321, de.getValue());
        
        DataCollection<TestDataElement> fdc = new FilteredDataCollection<>(dc, new Filter<TestDataElement>()
        {
            @Override
            public boolean isEnabled()
            {
                return true;
            }
            
            @Override
            public boolean isAcceptable(TestDataElement de)
            {
                return de.getValue()%3 == 0;
            }
        });
        
        assertEquals(33334, fdc.getSize());
        assertEquals(33334, fdc.getNameList().size());
        assertEquals("30000", fdc.getNameList().get(10000));
        assertNotNull(dc.get("1234"));
        assertNull(fdc.get("1234"));
        assertEquals(3702, fdc.get("3702").getValue());
        
        for(TestDataElement tde: fdc)
        {
            assertTrue(tde.getValue()%3 == 0);
        }
    }
}
