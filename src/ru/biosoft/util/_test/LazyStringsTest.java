package ru.biosoft.util._test;

import ru.biosoft.util.LazyStringBuilder;
import ru.biosoft.util.LazySubSequence;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class LazyStringsTest extends TestCase
{
    private static class StubCharSequence implements CharSequence
    {
        protected String value = null;
        private static final String REAL_VALUE = "Real string value";

        @Override
        public int length()
        {
            return toString().length();
        }

        @Override
        public char charAt(int index)
        {
            return toString().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end)
        {
            return new LazySubSequence(this, start, end);
        }

        @Override
        public String toString()
        {
            value = REAL_VALUE;
            return value;
        }
    }
    
    public void testLazySequences() throws Exception
    {
        StubCharSequence seq1 = new StubCharSequence();
        StubCharSequence seq2 = new StubCharSequence();
        StubCharSequence seq3 = new StubCharSequence();
        LazyStringBuilder buffer = new LazyStringBuilder();
        buffer.append(seq1.subSequence(5, 16).subSequence(0, 6)).append('+').append(seq2).append("+").append(seq3, 0, 4);
        CharSequence result = buffer.subSequence(4, 27);
        assertNull(seq1.value);
        assertNull(seq2.value);
        assertNull(seq3.value);
        assertEquals("ng+Real string value+Re", result.toString());
        assertNotNull(seq1.value);
        assertNotNull(seq2.value);
        assertNotNull(seq3.value);
    }
}
