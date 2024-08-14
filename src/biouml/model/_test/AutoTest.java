package biouml.model._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());

        suite.addTest(DynamicModelTest.suite());
        suite.addTest(TestDiagramToXML.suite());

        suite.addTestSuite(TestCompartment.class);
        suite.addTestSuite(TestNode.class);
        suite.addTestSuite(TestMoving.class);
        suite.addTestSuite(TestVariableNameMode.class);

        return suite;
    }

    /**
     * Run test in TestRunner. If args[0].startsWith("text") then textui runner
     * runs, otherwise swingui runner runs.
     * 
     * @param args[0]
     *            Type of test runner.
     */
    public static void main(String[] args)
    {
        if( args != null && args.length != 0 && args[0].startsWith("text") )
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(AutoTest.class);
        }
    }
}
