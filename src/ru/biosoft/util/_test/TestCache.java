package ru.biosoft.util._test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.util.Cache;

public class TestCache extends AbstractBioUMLTest
{
    public void testCache() throws Throwable
    {
        LongAdder a = new LongAdder();
        Function<String, String> c = Cache.soft( key -> {
            a.increment();
            return key.trim();
        });
        try
        {
            TaskPool.getInstance().executeMultiple( () -> {
                for(int i=0; i<10000; i++) {
                    String str = " "+i+" ";
                    String expected = String.valueOf(i);
                    assertEquals(expected, c.apply( str ));
                }
            });
        }
        catch( ExecutionException e )
        {
            throw e.getCause();
        }
        assertEquals(10000, a.intValue());
    }
}
