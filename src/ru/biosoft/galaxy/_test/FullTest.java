package ru.biosoft.galaxy._test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.galaxy.GalaxyDataCollection;
import ru.biosoft.galaxy.GalaxyDataCollection.GalaxyLabel;
import ru.biosoft.galaxy.GalaxyMethod;
import ru.biosoft.galaxy.GalaxyMethodTest;
import ru.biosoft.galaxy.javascript.JavaScriptGalaxy;

/**
 * Run all tests of Galaxy methods
 * This test should be run with biouml.plugins.junittest.TestRunner
 */
public class FullTest extends TestCase
{
    protected static final String analysesDir = "../analyses";
    protected static final File resultPath = AbstractBioUMLTest.getTestFile( "result.html" );

    public FullTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(FullTest.class.getName());
        suite.addTest(new FullTest("test"));
        return suite;
    }

    public void test() throws Exception
    {
        DataCollection repository = CollectionFactory.createRepository(analysesDir);
        assertNotNull("Can't create repository", repository);

        GalaxyDataCollection galaxyDE = JavaScriptGalaxy.GALAXY_COLLECTION.getDataElement(GalaxyDataCollection.class);

        List<GalaxyTestResult> results = new ArrayList<>();

        int count = 0;
        int totalSize = galaxyDE.stream( DataCollection.class ).mapToInt( DataCollection::getSize ).sum();
        int successSize = 0;
        List<DataCollection> folders = null;
        galaxyDE.stream().filter( DataCollection.class::isInstance ).map( e -> (DataCollection)e ).collect( Collectors.toList() );
        for( DataCollection<?> folder : folders )
        {
            List<String> methodNames = folder.getNameList();

            for( String methodName : methodNames )
            {
                DataElement de = folder.get(methodName);
                if( de instanceof GalaxyLabel )
                    continue;
                assertTrue("Unknown element type:" + de.getName(), de instanceof AnalysisMethodInfo);

                AnalysisMethod method = ( (AnalysisMethodInfo)de ).createAnalysisMethod();
                assertNotNull("Can't create analysis", method);
                assertTrue("Analysis is not Galaxy method", ( method instanceof GalaxyMethod ));

                GalaxyTestResult result = new GalaxyTestResult(count++, folder.getName(), method.getName());
                results.add(result);
                GalaxyTestThread testThread = new GalaxyTestThread((GalaxyMethod)method, result);
                long time = System.currentTimeMillis();
                testThread.start();

                int watchDog = 30; //time limit in seconds
                while( testThread.isAlive() )
                {
                    watchDog--;
                    if( watchDog == 0 )
                    {
                        testThread.stop();
                        result.status = false;
                        result.errors = "Time out";
                        break;
                    }
                    Thread.sleep(1000);
                }
                result.time = System.currentTimeMillis() - time;

                if( result.status )
                    successSize++;

                result.interpreter = ( (GalaxyMethod)method ).getMethodInfo().getCommand().getInterpreter();

                System.err.println("Complete " + count + " tests of " + totalSize);
            }
        }

        try(PrintWriter pw = new PrintWriter(resultPath))
        {
            fillTop(pw, "Success " + successSize + " of " + totalSize);

            for( GalaxyTestResult result : results )
            {
                fillRow(pw, result);
            }

            fillBottom(pw);
        }
    }

    protected void fillTop(PrintWriter pw, String info)
    {
        pw.println("<html>");
        pw.println("<body>");
        pw.println("  <p>");
        pw.println("    " + info);
        pw.println("  </p>");
        pw.println("  <table width='100%' border='1'>");
        pw.println("    <tr>");
        pw.println("      <td>#</td>");
        pw.println("      <td>Folder</td>");
        pw.println("      <td>Method name</td>");
        pw.println("      <td>Interpreter</td>");
        pw.println("      <td>Status</td>");
        pw.println("      <td>Time(ms)</td>");
        pw.println("      <td>Count</td>");
        pw.println("      <td>Compare</td>");
        pw.println("      <td>Errors</td>");
        pw.println("    </tr>");
    }

    protected void fillRow(PrintWriter pw, GalaxyTestResult result)
    {
        pw.println("    <tr>");
        pw.println("      <td>");
        pw.println("        " + result.ind);
        pw.println("      </td>");
        pw.println("      <td>");
        pw.println("        " + result.folder);
        pw.println("      </td>");
        pw.println("      <td>");
        pw.println("        " + result.name);
        pw.println("      </td>");
        pw.println("      <td>");
        pw.println("        " + result.interpreter + "&nbsp;");
        pw.println("      </td>");
        pw.println("      <td>");
        pw.println("        " + ( ( result.status ) ? "<font color='green'>OK</font>" : "<font color='red'>error</font>" ));
        pw.println("      </td>");
        pw.println("      <td>");
        pw.println("        " + result.time);
        pw.println("      </td>");
        pw.println("      <td>");
        pw.println("        " + result.testsPassed + "/" + result.testCount);
        pw.println("      </td>");
        pw.println("      <td>");
        pw.println("        " + result.compareMethod);
        pw.println("      </td>");
        pw.println("      <td>");
        pw.println("        <pre>" + result.errors + "</pre>&nbsp;");
        pw.println("      </td>");
        pw.println("    </tr>");
    }

    protected void fillBottom(PrintWriter pw)
    {
        pw.println("  </table>");
        pw.println("</body>");
        pw.println("</html>");
    }

    public static class GalaxyTestResult
    {
        public int ind;
        public String folder;
        public String name;
        public boolean status;
        public String errors;
        public long time;
        public String interpreter;
        public int testCount;
        public int testsPassed;
        public String compareMethod;

        public GalaxyTestResult(int ind, String folder, String name)
        {
            this.ind = ind;
            this.folder = folder;
            this.name = name;
        }
    }

    public static class GalaxyTestThread extends Thread
    {
        protected GalaxyMethod method;
        protected GalaxyTestResult result;

        public GalaxyTestThread(GalaxyMethod method, GalaxyTestResult result)
        {
            this.method = method;
            this.result = result;
        }

        @Override
        public void run()
        {
            StringWriter errors = new StringWriter();
            List<GalaxyMethodTest> tests = ( method ).getMethodInfo().getTests();
            int testsPassed = 0;
            for( GalaxyMethodTest test : tests )
            {
                boolean ok;
                try
                {
                    ok = ( method ).processTest(test, errors);
                         
                }catch(Throwable e)
                {
                    errors.write("Exception: " + e.getMessage() + "\n");
                    ok = false;
                }
                if(ok)
                    testsPassed++;
                result.compareMethod = test.getComparators().toString();
            }

            result.testCount = tests.size();
            result.status = testsPassed == tests.size();
            result.errors = errors.toString();
            result.testsPassed = testsPassed;
        }
    }
}
