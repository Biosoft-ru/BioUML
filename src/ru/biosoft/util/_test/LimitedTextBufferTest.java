package ru.biosoft.util._test;

import ru.biosoft.util.LimitedTextBuffer;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author lan
 *
 */
public class LimitedTextBufferTest extends TestCase
{
    public LimitedTextBufferTest(String name)
    {
        super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(LimitedTextBufferTest.class.getName());
        suite.addTest(new LimitedTextBufferTest("testLimitedTextBuffer"));
        return suite;
    }

    public void testLimitedTextBuffer() throws Exception
    {
        LimitedTextBuffer buffer = new LimitedTextBuffer(5);
        buffer.add("asdfasdfafa\nasdfasd\n\nsadfasdf\nasdfasdf\nsfgdsfgsdfg\n");
        assertEquals(buffer.toString(), "asdfasd\n\nsadfasdf\nasdfasdf\nsfgdsfgsdfg\n");
        buffer.add("asdfasdfafa\nasdfasd\n\nsadfasdf\nasdfasdf\nsfgdsfgsdfg\n");
        assertEquals(buffer.toString(), "asdfasd\n\nsadfasdf\nasdfasdf\nsfgdsfgsdfg\n");
        buffer.add("fasgasdff\n");
        assertEquals(buffer.toString(), "\nsadfasdf\nasdfasdf\nsfgdsfgsdfg\nfasgasdff\n");
        buffer.add("\n\n");
        assertEquals(buffer.toString(), "asdfasdf\nsfgdsfgsdfg\nfasgasdff\n\n\n");
        assertEquals(buffer.toString(), "asdfasdf\nsfgdsfgsdfg\nfasgasdff\n\n\n");
    }
}
