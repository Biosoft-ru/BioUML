package biouml.plugins.simulation._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());
        suite.addTest(ResultSqlTransformerTest.suite());
        suite.addTest(ResultTransformerTest.suite());
//        suite.addTest(CompositeSimulationTest.suite()); TODO: new composite tests
        suite.addTest(TestTableElementPreprocessor.suite());
        suite.addTest(SpanTest.suite());
        suite.addTest(NewtonSolverTest.suite());
        suite.addTest(TestSolverResult.suite());
        return suite;
    }
}
