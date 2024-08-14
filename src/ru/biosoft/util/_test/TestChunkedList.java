package ru.biosoft.util._test;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import ru.biosoft.util.ChunkedList;

public class TestChunkedList extends TestCase
{
    public void testIndexOf()
    {
        Integer[] values = {0,1,2,3,4,5,6,7,8,9};
        List<Integer> list = new ChunkedList<Integer>(values.length, 2, false)
        {
            @Override
            protected Integer[] getChunk(int from, int to)
            {
                return Arrays.copyOfRange( values, from, to );
            }
        };
        assertEquals( 5, (int)list.get( 5 ));
        assertEquals( 5, list.indexOf( 5 ));
    }
}
