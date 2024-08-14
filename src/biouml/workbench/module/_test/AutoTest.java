package biouml.workbench.module._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );

        suite.addTest( ImportExportModuleTest.suite() );

        return suite;
    }
}