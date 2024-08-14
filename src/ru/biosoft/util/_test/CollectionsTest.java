package ru.biosoft.util._test;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ru.biosoft.util.IntKeysAsStringMap;
import ru.biosoft.util.IntValuesAsStringMap;
import ru.biosoft.util.ReversedList;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class CollectionsTest extends TestCase
{
    public void testIntKeysAsStringMap() throws Exception
    {
        TIntObjectMap<String> intMap = new TIntObjectHashMap<>();
        intMap.put(1, "test1");
        intMap.put(2, "test2");
        intMap.put(10, "test10");
        intMap.put(16, "test16");
        Map<String, String> map = new IntKeysAsStringMap<>( intMap );
        assertEquals(4, map.size());
        assertEquals("test10", map.get("10"));
        map.put("13", "test13");
        assertEquals("test13", intMap.get(13));
        intMap.put(20, "test20");
        assertTrue(map.containsKey("20"));
        map.remove("2");
        assertFalse(intMap.containsKey(2));
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("1", "test1");
        expectedMap.put("20", "test20");
        expectedMap.put("16", "test16");
        expectedMap.put("13", "test13");
        expectedMap.put("10", "test10");
        assertEquals(expectedMap, map);
        for(Entry<String, String> entry: map.entrySet())
        {
            assertEquals(intMap.get(Integer.parseInt(entry.getKey())), entry.getValue());
        }
    }
    
    public void testIntValuesAsStringMap() throws Exception
    {
        TObjectIntMap<String> intMap = new TObjectIntHashMap<>();
        intMap.put("test1", 1);
        intMap.put("test2", 2);
        intMap.put("test10", 10);
        intMap.put("test16", 16);
        Map<String, String> map = new IntValuesAsStringMap<>( intMap );
        assertEquals(4, map.size());
        assertEquals("10", map.get("test10"));
        map.put("test13", "13");
        assertEquals(13, intMap.get("test13"));
        intMap.put("test20", 20);
        assertTrue(map.containsKey("test20"));
        map.remove("test2");
        assertFalse(intMap.containsKey("test2"));
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("test1", "1");
        expectedMap.put("test20", "20");
        expectedMap.put("test16", "16");
        expectedMap.put("test13", "13");
        expectedMap.put("test10", "10");
        assertEquals(expectedMap, map);
        for(Entry<String, String> entry: map.entrySet())
        {
            assertEquals(intMap.get(entry.getKey()), Integer.parseInt(entry.getValue()));
        }
    }
    
    public void testReversedList()
    {
        ReversedList<Integer> list = new ReversedList<>( Arrays.asList( 1, 3, 5, 7, 9 ) );
        assertEquals(Arrays.asList(9,7,5,3,1), list);
    }
}
