package ru.biosoft.access.task._test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.jobcontrol.Iteration;

public class TestIterationRunner extends TestCase
{

    public void test1() throws InterruptedException, ExecutionException
    {
        List<Integer> collection = new ArrayList<>();
        for(int i = 0; i < 10; i++)
            collection.add(i);
        
        
        AtomicInteger sum = new AtomicInteger();
        
        AtomicInteger parallelism = new AtomicInteger( 0 );
        AtomicInteger max = new AtomicInteger( 0 );
        
        Iteration<Integer> iteration = new Iteration<Integer>()
        {
            @Override
            public boolean run(Integer element)
            {
                int cur = parallelism.incrementAndGet();
                max.accumulateAndGet( cur, Math::max );
                sum.addAndGet( element );
                parallelism.decrementAndGet();
                return true;
            }
        };
        TaskPool.getInstance().iterate( collection, iteration, null, 3 );
        
        assertEquals( 45, sum.get() );
        System.out.println( max.get() );
        if(max.get() > 3)
            fail();
    }
}