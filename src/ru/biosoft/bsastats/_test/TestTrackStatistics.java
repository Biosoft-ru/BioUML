package ru.biosoft.bsastats._test;

import java.util.Random;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.bsastats.processors.BasicStatsProcessor;
import ru.biosoft.bsastats.processors.DuplicateSequencesProcessor;
import ru.biosoft.bsastats.processors.GCContentPerBaseProcessor;
import ru.biosoft.bsastats.processors.GCPerSequenceProcessor;
import ru.biosoft.bsastats.processors.LengthDistributionProcessor;
import ru.biosoft.bsastats.processors.NContentPerBaseProcessor;
import ru.biosoft.bsastats.processors.NucleotideContentPerBaseProcessor;
import ru.biosoft.bsastats.processors.QualityPerSequenceProcessor;
import ru.biosoft.bsastats.processors.StatisticsProcessor;
import ru.biosoft.bsastats.processors.StatisticsProcessor.Quality;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 * TODO: test the rest of processors
 */
public class TestTrackStatistics extends AbstractBioUMLTest
{
    private static final int NUM_SITES = 1000;
    private Random random = new Random(1);
    private VectorDataCollection<DataElement> vdc;
    private int minLen = Integer.MAX_VALUE, maxLen = Integer.MIN_VALUE;
    private static Logger log = Logger.getLogger(TestTrackStatistics.class.getName());
    
    private void feedWithTestData(StatisticsProcessor processor) throws Exception
    {
        processor.init(log);
        for( int j = 0; j < NUM_SITES; j++ )
        {
            int length = (int) ( random.nextGaussian() * 30 + 100 );
            if( length < 10 )
                length = 10;
            if( length > 190 )
                length = 190;
            if(minLen > length) minLen = length;
            if(maxLen < length) maxLen = length;
            byte[] sequence = new byte[length];
            byte[] qualities = new byte[length];
            for( int i = 0; i < length; i++ )
            {
                sequence[i] = (byte)"ACGT".codePointAt(random.nextInt(4));
                qualities[i] = (byte) ( random.nextInt(30) + 2 );
            }
            processor.update(sequence, qualities);
        }
        processor.save(vdc);
        for(String name: processor.getReportItemNames())
        {
            DataElement de = vdc.get(name);
            assertNotNull(de);
            if(name.endsWith(" chart"))
                assertTrue(de instanceof ImageElement);
            else
                assertTrue(de instanceof TableDataCollection);
        }
    }
    
    public void testBasicStatsProcessor() throws Exception
    {
        StatisticsProcessor processor = new BasicStatsProcessor();
        feedWithTestData(processor);
        DataElement de = vdc.get(processor.getName());
        assertTrue(de instanceof TableDataCollection);
        TableDataCollection tdc = (TableDataCollection)de;
        assertEquals(NUM_SITES, Integer.parseInt(String.valueOf(tdc.get("Count").getValue("Value"))));
        double gc = Double.parseDouble(String.valueOf(tdc.get("GC%").getValue("Value")));
        assertTrue(gc>49.5 && gc < 50.5);
        double avgLength = Double.parseDouble(String.valueOf(tdc.get("Avg length").getValue("Value")));
        assertTrue(avgLength > 98 && avgLength < 102);
        assertEquals(minLen, Integer.parseInt(String.valueOf(tdc.get("Min length").getValue("Value"))));
        assertEquals(maxLen, Integer.parseInt(String.valueOf(tdc.get("Max length").getValue("Value"))));
        assertEquals(Quality.OK, processor.getQuality());
    }
    
    public void testLengthDistributionProcessor() throws Exception
    {
        StatisticsProcessor processor = new LengthDistributionProcessor();
        feedWithTestData(processor);
        DataElement de = vdc.get(processor.getName());
        assertTrue(de instanceof TableDataCollection);
        TableDataCollection tdc = (TableDataCollection)de;
        int sum = 0;
        int maxVal = -1;
        int maxPos = -1;
        for(RowDataElement rde: tdc)
        {
            int pos = Integer.parseInt(rde.getName());
            assertTrue(pos >= minLen && pos <= maxLen);
            int val = ((Number)rde.getValues()[0]).intValue();
            assertEquals(val*100.0/NUM_SITES, rde.getValues()[1]);
            sum += val;
            if(val > maxVal)
            {
                maxVal = val;
                maxPos = pos;
            }
        }
        assertEquals(NUM_SITES, sum);
        assertTrue(maxPos > 90 && maxPos < 110);
        assertEquals(Quality.WARN, processor.getQuality());
    }
    
    public void testNContentPerBaseProcessor() throws Exception
    {
        StatisticsProcessor processor = new NContentPerBaseProcessor();
        feedWithTestData(processor);
        DataElement de = vdc.get(processor.getName());
        assertTrue(de instanceof TableDataCollection);
        TableDataCollection tdc = (TableDataCollection)de;
        assertEquals(maxLen, tdc.getSize());
        for(RowDataElement rde: tdc)
        {
            int pos = Integer.parseInt(rde.getName());
            assertTrue(pos >= 1 && pos <= maxLen);
            int val = ((Number)rde.getValues()[0]).intValue();
            assertEquals(0, val);
            double percent = ((Number)rde.getValues()[1]).doubleValue();
            assertEquals(0.0, percent);
        }
        assertEquals(Quality.OK, processor.getQuality());
    }

    public void testGCContentPerBaseProcessor() throws Exception
    {
        StatisticsProcessor processor = new GCContentPerBaseProcessor();
        feedWithTestData(processor);
        DataElement de = vdc.get(processor.getName());
        assertTrue(de instanceof TableDataCollection);
        TableDataCollection tdc = (TableDataCollection)de;
        assertEquals(maxLen, tdc.getSize());
        for(RowDataElement rde: tdc)
        {
            int pos = Integer.parseInt(rde.getName());
            assertTrue(pos >= 1 && pos <= maxLen);
            int val = ((Number)rde.getValues()[0]).intValue();
            double percent = ((Number)rde.getValues()[1]).doubleValue();
            if(pos <= minLen)
                assertEquals(percent, val*100.0/NUM_SITES);
            else
                assertTrue(percent == 0 || percent > val*100.0/NUM_SITES);
            if(pos < 100)
                assertTrue(percent > 45 && percent < 55);
        }
        assertEquals(Quality.ERROR, processor.getQuality());
    }

    public void testNucleotideContentPerBaseProcessor() throws Exception
    {
        StatisticsProcessor processor = new NucleotideContentPerBaseProcessor();
        feedWithTestData(processor);
        DataElement de = vdc.get(processor.getName());
        assertTrue(de instanceof TableDataCollection);
        TableDataCollection tdc = (TableDataCollection)de;
        assertEquals(maxLen, tdc.getSize());
        for(RowDataElement rde: tdc)
        {
            int pos = Integer.parseInt(rde.getName());
            assertTrue(pos >= 1 && pos <= maxLen);
            for(int i=0; i<4; i++)
            {
                int val = ((Number)rde.getValues()[2*i]).intValue();
                double percent = ((Number)rde.getValues()[2*i+1]).doubleValue();
                if(pos <= minLen)
                    assertEquals(percent, val*100.0/NUM_SITES);
                else
                    assertTrue(percent == 0 || percent > val*100.0/NUM_SITES);
                if(pos < 100)
                    assertTrue(percent > 20 && percent < 30);
            }
        }
        assertEquals(Quality.ERROR, processor.getQuality());
    }

    public void testDuplicateSequencesProcessor() throws Exception
    {
        StatisticsProcessor processor = new DuplicateSequencesProcessor();
        random = new Random(1);
        feedWithTestData(processor);
        DataElement de = vdc.get(processor.getName());
        assertTrue(de instanceof TableDataCollection);
        TableDataCollection tdc = (TableDataCollection)de;
        assertEquals(20, tdc.getSize());
        for(RowDataElement rde: tdc)
        {
            if(rde.getName().equals("20+")) continue;
            int pos = Integer.parseInt(rde.getName());
            assertTrue(pos >= 1 && pos < 20);
            if(pos == 1)
            {
                assertEquals(NUM_SITES, ((Number)rde.getValues()[0]).intValue());
                assertEquals(NUM_SITES, ((Number)rde.getValues()[1]).intValue());
                assertEquals(100.0, ((Number)rde.getValues()[2]).doubleValue());
            } else
            {
                assertEquals(0, ((Number)rde.getValues()[0]).intValue());
                assertEquals(0, ((Number)rde.getValues()[1]).intValue());
                assertEquals(0.0, ((Number)rde.getValues()[2]).doubleValue());
            }
        }
        assertEquals(Quality.OK, processor.getQuality());
        random = new Random(1);
        feedWithTestData(processor);
        de = vdc.get(processor.getName());
        assertTrue(de instanceof TableDataCollection);
        tdc = (TableDataCollection)de;
        assertEquals(20, tdc.getSize());
        for(RowDataElement rde: tdc)
        {
            if(rde.getName().equals("20+")) continue;
            int pos = Integer.parseInt(rde.getName());
            assertTrue(pos >= 1 && pos < 20);
            if(pos == 2)
            {
                assertEquals(NUM_SITES*2, ((Number)rde.getValues()[0]).intValue());
                assertEquals(NUM_SITES, ((Number)rde.getValues()[1]).intValue());
            } else
            {
                assertEquals(0, ((Number)rde.getValues()[0]).intValue());
                assertEquals(0, ((Number)rde.getValues()[1]).intValue());
            }
        }
        assertEquals(Quality.ERROR, processor.getQuality());
    }
    
    public void testGCPerSequenceProcessor() throws Exception
    {
        StatisticsProcessor processor = new GCPerSequenceProcessor();
        feedWithTestData(processor);
        assertEquals(Quality.OK, processor.getQuality());
    }
    
    public void testQualityPerSequenceProcessor() throws Exception
    {
        StatisticsProcessor processor = new QualityPerSequenceProcessor();
        feedWithTestData(processor);
        DataElement de = vdc.get(processor.getName());
        assertTrue(de instanceof TableDataCollection);
        TableDataCollection tdc = (TableDataCollection)de;
        int sum = 0;
        for(RowDataElement rde: tdc)
        {
            int pos = Integer.parseInt(rde.getName());
            assertTrue(pos >= 2 && pos <= 32);
            int val = ((Number)rde.getValues()[0]).intValue();
            double percent = ((Number)rde.getValues()[1]).doubleValue();
            assertEquals(percent, val*100.0/NUM_SITES);
            sum+=val;
        }
        assertEquals(NUM_SITES, sum);
        assertEquals(Quality.ERROR, processor.getQuality());
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        vdc = new VectorDataCollection<>("test");
    }
}
