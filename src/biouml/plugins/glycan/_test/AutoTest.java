package biouml.plugins.glycan._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.glycan.parser._test.GlycanParserTest;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());

        suite.addTest(GlycanParserTest.suite());

        return suite;
    }
}