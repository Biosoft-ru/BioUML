package ru.biosoft.access._test;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;
import ru.biosoft.access.core.Index;

public class IndexTest extends TestCase
{
    protected Index index;
    
    public IndexTest(String name)
    {
        super(name);
    }
    
    protected void fillIndex(String namePrefix, int from, int n)
    {
        for( int i = 0; i < n; i++ )
            index.put(namePrefix + String.format("%09d", i + from), new Index.IndexEntry(i + from, 10));
    }
    
    protected void fillIndex(String namePrefix, int n)
    {
        fillIndex(namePrefix, 0, n);
    }

    public void testPut() throws Exception
    {
        fillIndex("entryName", 200000);
    }
    
    public void testIterate() throws Exception
    {
        fillIndex("entryName", 200000);
        Iterator<Map.Entry<String, Index.IndexEntry>> it = index.entrySet().iterator();
        int i = 0;
        while(it.hasNext())
        {
            Map.Entry<String, Index.IndexEntry> entry = it.next();
            String key = entry.getKey();
            Index.IndexEntry value = entry.getValue();
            assertEquals("entryName" + String.format("%09d", i), key);
            assertEquals(i, value.from);
            assertEquals(10, value.len);
            i++;
        }
    }
    
    public void testRandomAccess() throws Exception
    {
        int n = 200000;
        fillIndex("entryName", n);
        Random random = new Random();
        for(int i = 0; i < n; i++)
        {
            int r = random.nextInt(n);
            String key = "entryName" + String.format("%09d", r);
            Index.IndexEntry entry = (Index.IndexEntry)index.get(key);
            assertNotNull(entry);
            assertEquals(r, entry.from);
            assertEquals(10, entry.len);
        }
    }
    
    public void testPutRemovePut()
    {
        fillIndex("entry", 50, 200);
        assertEquals(200, index.size());
        for( int i = 100; i < 150; i++ )
        {
            index.remove("entry" + String.format("%09d", i));
            assertEquals(200 - ( i - 99 ), index.size());
        }
        assertEquals(150, index.size());
        fillIndex("entry", 130, 10);
        assertEquals(160, index.size());
        fillIndex("entry", 300, 100);
        assertEquals(260, index.size());

        for( int i = 50; i < 100; i++ )
            assertTrue(index.containsKey("entry" + String.format("%09d", i)));
        
        for( int i = 130; i < 140; i++ )
            assertTrue(index.containsKey("entry" + String.format("%09d", i)));
        
        for( int i = 150; i < 250; i++ )
            assertTrue(index.containsKey("entry" + String.format("%09d", i)));

        for( int i = 300; i < 400; i++ )
            assertTrue(index.containsKey("entry" + String.format("%09d", i)));
    }
    
    @Override
    public void tearDown() throws Exception
    {
        index.close();
        if(index.getIndexFile().isDirectory())
          FileUtils.deleteDirectory(index.getIndexFile());
        else
          index.getIndexFile().delete();
    }
 
}
