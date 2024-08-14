

package ru.biosoft.bsa._test;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.bsa.DiNucleotideAlphabet;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Slice;
import ru.biosoft.bsa.SlicedSequence;

public class SlicedSequenceTest extends TestCase
{
    /** Standart JUnit constructor */
    public SlicedSequenceTest(String name)
    {
        super(name);
    }

    public void testLetterAtMethod()
    {
        Sequence sequence = new TestSlicedSequence();
        byte letter = sequence.getLetterAt(0);
        assertEquals("Incorrect value", letter, 'a');
        letter = sequence.getLetterAt(100);
        assertEquals("Incorrect value", letter, 'a');
        letter = sequence.getLetterAt(85);
        assertEquals("Incorrect value", letter, 'a');
        letter = sequence.getLetterAt(21);
        assertEquals("Incorrect value", letter, 'c');
        letter = sequence.getLetterAt(21);
        assertEquals("Incorrect value", letter, 'c');
        letter = sequence.getLetterAt(35);
        assertEquals("Incorrect value", letter, 'a');
        letter = sequence.getLetterAt(2);
        assertEquals("Incorrect value", letter, 'g');
        letter = sequence.getLetterAt(15);
        assertEquals("Incorrect value", letter, 'a');
        letter = sequence.getLetterAt(92);
        assertEquals("Incorrect value", letter, 'g');
        letter = sequence.getLetterAt(50);
        assertEquals("Incorrect value", letter, 'a');
    }
    
    public void testLetterCodeAtMethod()
    {
        Sequence sequence = new TestSlicedSequence();
        Random r = new Random();
        for(int i=0; i<100; i++)
        {
            int position = r.nextInt( sequence.getLength() )+sequence.getStart();
            byte code = (byte) ( position%5 );
            assertEquals(code, sequence.getLetterCodeAt(position));
        }
        DiNucleotideAlphabet diAlphabet = DiNucleotideAlphabet.getInstance();
        for(int i=0; i<100; i++)
        {
            int position = r.nextInt( sequence.getLength()-1 )+sequence.getStart();
            byte code;
            switch(position%5)
            {
                case 0:
                    code = 1; // 'ac' 0*4+1
                    break;
                case 1:
                    code = 6; // 'cg' 1*4+2
                    break;
                case 2:
                    code = 11; // 'gt' 2*4+3
                    break;
                default:
                    code = diAlphabet.codeForAny();
            }
            assertEquals(code, sequence.getLetterCodeAt(position, diAlphabet));
        }
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite()
    {
        return new TestSuite(SlicedSequenceTest.class);
    }

    public static class TestSlicedSequence extends SlicedSequence
    {
        public TestSlicedSequence()
        {
            super(Nucleotide5LetterAlphabet.getInstance());
        }

        @Override
        public int getLength()
        {
            return 100;
        }
        
        @Override
        public int getStart()
        {
            return 1;
        }

        @Override
        protected Slice loadSlice(int pos)
        {
            int from = Math.max(0, pos - (int) ( Math.random() * 10.0 ));
            int to = pos + (int) ( Math.random() * 10.0 ) + 1;
            return loadSlice(from, to);
        }

        protected Slice loadSlice(int from, int to)
        {
            Slice result = new Slice();
            result.from = from;
            result.to = to;

            byte[] bytes = new byte[to - from];
            for( int i = 0; i < ( to - from ); i++ )
            {
                bytes[i] = (byte)"acgtn".codePointAt( (from + i)%5 );
            }

            result.data = bytes;

            return result;
        }
    }
}
