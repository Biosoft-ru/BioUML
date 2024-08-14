package ru.biosoft.bsa._test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.LongSequence;
import ru.biosoft.bsa.Sequence;

/**
 * @author lan
 *
 */
public class LongFastaTest extends TestCase
{
    public void testLongFastaParallel() throws Throwable
    {
        BSATestUtils.createRepository();
        
        final int nThreads = 10;
        Thread[] threads = new Thread[nThreads];
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<Throwable>());
        final List<String> sequences = Collections.synchronizedList(new ArrayList<String>());
        DataCollection<AnnotatedSequence> dc = CollectionFactory.getDataCollection("databases/fasta/sample");
        assertNotNull(dc);
        final Sequence sequence = dc.get("seq_0001").getSequence();
        assertNotNull(sequence);
        assertTrue(sequence instanceof LongSequence);
        
        for(int i=0; i<nThreads; i++)
        {
            threads[i] = new Thread(getClass().getSimpleName()+"-#"+i)
            {
                @Override
                public void run()
                {
                    try
                    {
                        int threadNum = Integer.parseInt(getName().substring(getName().length()-1));
                        assertEquals(264000, sequence.getLength());
                        byte[] b = new byte[sequence.getLength()];
                        for(int i=0; i<sequence.getLength(); i++)
                        {
                            int j = (i+sequence.getLength()*threadNum/nThreads)%sequence.getLength();
                            b[j] = sequence.getLetterAt(j+1);
                        }
                        sequences.add(new String(b));
                    }
                    catch(Throwable e)
                    {
                        errors.add(e);
                    }
                }
            };
            threads[i].start();
        }
        
        for(int i=0; i<nThreads; i++)
        {
            threads[i].join();
        }
        
        if(!errors.isEmpty())
        {
            throw errors.get(0);
        }
        
        assertEquals(nThreads, sequences.size());
        for(int i=1; i<nThreads; i++)
            assertEquals(sequences.get(0), sequences.get(i));
    }
}
