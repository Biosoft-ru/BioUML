package ru.biosoft.util._test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.util.ListUtil;

import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class TestListUtil extends TestCase
{
    public void testAggregates()
    {
        Map<String, List<String>> testMap = new LinkedHashMap<>();
        List<String> aList = new ArrayList<>();
        List<String> bList = new ArrayList<>();
        assertTrue(ListUtil.isEmpty(testMap));
        testMap.put("b", bList);
        assertTrue(ListUtil.isEmpty(testMap));
        testMap.put("a", aList);
        assertTrue(ListUtil.isEmpty(testMap));
        assertEquals(0, ListUtil.sumTotalSize(testMap));
        bList.add("foo");
        assertFalse(ListUtil.isEmpty(testMap));
        bList.add("bar");
        aList.add("2");
        aList.add("1");
        assertEquals(4, ListUtil.sumTotalSize(testMap));
        ListUtil.sortAll(testMap);
        assertEquals("{b=[bar, foo], a=[1, 2]}", testMap.toString());
    }
}
