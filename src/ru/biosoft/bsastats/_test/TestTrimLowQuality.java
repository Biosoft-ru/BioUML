package ru.biosoft.bsastats._test;

import junit.framework.TestCase;
import ru.biosoft.bsastats.Task;
import ru.biosoft.bsastats.TrimLowQuality;

public class TestTrimLowQuality extends TestCase
{
    public void test() throws Exception
    {
        TrimLowQuality processor = new TrimLowQuality();
        processor.setPhredQualityThreashold( 10 );
        processor.setFrom3PrimeEnd( true );
        processor.setFrom5PrimeEnd( true );
        byte[] quals = new byte[] {5,15,20,20,20,20,4,12,4,3};
        Task task = new Task( "ACTGACTGAC".getBytes(), quals, null );
        Task processed = processor.process( task );
        assertEquals("CTGAC", new String( processed.getSequence() ));
    }
}
