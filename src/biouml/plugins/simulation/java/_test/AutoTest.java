package biouml.plugins.simulation.java._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());

        suite.addTest(TestEventModel.suite());
        suite.addTest(TestJavaSimulationEngine.suite());
        suite.addTest(TestStateTransitionModel.suite());
        suite.addTest(SimulationTest.suite());
        suite.addTest( TestFastReaction.suite() );
//        suite.addTest( TestLargeModel.suite() );
        suite.addTest( TestInitialAssignments.suite() );
        suite.addTest( TestCycledEqs.suite() );
        suite.addTest(TestConstraintsModel.suite());
        return suite;
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
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
} // end of AutoTest