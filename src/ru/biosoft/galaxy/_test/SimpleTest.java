package ru.biosoft.galaxy._test;

import java.io.StringWriter;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.galaxy.GalaxyMethod;
import ru.biosoft.galaxy.GalaxyMethodTest;

/**
 * Simple test of Galaxy method
 * This test should be run with biouml.plugins.junittest.TestRunner
 */
public class SimpleTest extends TestCase
{
    protected static final String analysesDir = "./analyses";
    protected static final String testMethodPath = "analyses/Galaxy/solexa_tools/lastz_wrapper_2";

    public SimpleTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(SimpleTest.class.getName());
        suite.addTest(new SimpleTest("test"));
        return suite;
    }

    public void test() throws Exception
    {
        DataCollection repository = CollectionFactory.createRepository(analysesDir);
        assertNotNull("Can't create repository", repository);

        DataElement de = CollectionFactory.getDataElement(testMethodPath);
        assertNotNull("Can't find test", de);
        assertTrue("Data element is not analysis method info", ( de instanceof AnalysisMethodInfo ));

        AnalysisMethod method = ( (AnalysisMethodInfo)de ).createAnalysisMethod();
        assertNotNull("Can't create analysis", method);
        assertTrue("Analysis is not Galaxy method", ( method instanceof GalaxyMethod ));

        try
        {
            StringWriter errors = new StringWriter();
            for( GalaxyMethodTest test : ( (GalaxyMethod)method ).getMethodInfo().getTests() )
            {
                boolean result = ( (GalaxyMethod)method ).processTest(test, errors);
                if( errors.toString().trim().length() > 0 )
                {
                    fail(errors.toString());
                }
                assertTrue("Incorrect test result", result);
            }
        }
        catch( Exception e )
        {
            fail("Test fail: " + e.getMessage());
        }
    }
}
