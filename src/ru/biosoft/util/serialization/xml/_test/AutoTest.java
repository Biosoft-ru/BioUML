package ru.biosoft.util.serialization.xml._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 06.07.2006
 * Time: 12:52:18
 */
public class AutoTest extends TestCase
{
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite( XMLSerializerTest.class );
        suite.addTestSuite( ParserTest.class );
        return suite;
    }

    public AutoTest( String s )
    {
        super( s );
    }
}
