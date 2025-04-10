package ru.biosoft.analysis._test;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.util.TextUtil2;

/**
 * This class allows to test analyzes using common testing protocol defined in resources/*.t files
 * @author lan
 */
public class AnalysesTest extends AbstractBioUMLTest
{
    public AnalysesTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AnalysesTest.class.getName());
        suite.addTest(new AnalysesTest("testBasics"));
        suite.addTest(new AnalysesTest("testFilterTable"));
        suite.addTest(new AnalysesTest("testJoinTable"));
        suite.addTest(new AnalysesTest("testMultiJoinTable"));
        suite.addTest(new AnalysesTest("testSelectColumns"));
        suite.addTest(new AnalysesTest("testUpDownIdentification"));
        //suite.addTest(new AnalysesTest("testAnnotateTable"));
        //suite.addTest(new AnalysesTest("testConvertTable"));
        //suite.addTest(new AnalysesTest("testConvertTableWithIDColumn"));
        //suite.addTest(new AnalysesTest("testConvertTableWithMaxMatches"));
        return suite;
    }
    
    public void testBasics() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        Application.setPreferences(new Preferences());
        for(String name: AnalysisMethodRegistry.getAnalysisNamesWithGroup())
        {
            //System.err.println( "------------------------------------------------- '" + name + "'----------" );
            //TODO: add GTDR DB
            //ignore GTRD testing, since it need DB installation
            if( "GTRD/Search regulated genes".equals( name ) )
                continue;
            //ignore PSDPC analysis testing, it requires settings from biouml.server.path/appconfig/
            if( "Genome enhancer/PSD pharmaceutical compounds analysis".equals( name ) )
                continue;

            if( "Import/Video file (*.mp4, *.webm, *.ogg)".equals( name ) )
                continue;

            if( "Unclassified/Illumina methylation probes to track".equals( name ) )
                continue;

            try
            {
                String[] fields = TextUtil2.split( name, '/' );
                String analysisName = fields[1];
                AnalysisMethodInfo info = AnalysisMethodRegistry.getMethodInfo(analysisName);
                assertNotNull(name, info);
                Class<? extends AnalysisMethod> analysisClass = info.getAnalysisClass();
                assertNotNull(name, analysisClass);
                AnalysisMethod method = info.createAnalysisMethod();
                assertNotNull(name, method);
                assertEquals(name, analysisClass, method.getClass());
                
                AnalysisParameters parameters = method.getParameters();
                assertNotNull(name, parameters);
                method.setParameters(parameters);
                
                String description = info.getDescription();
                assertNotNull(name, description);
                String descriptionHTML = info.getDescriptionHTML();
                assertNotNull(name, descriptionHTML);
                assertFalse(name, descriptionHTML.startsWith("ru/"));
                assertFalse(name, descriptionHTML.startsWith("biouml/"));
                
                assertNotNull(name, method.getJobControl());
                assertNotNull(name, method.getLogger());
                assertTrue(name, method.estimateMemory()>=0);
                
            }
            catch( Throwable t )
            {
                StringWriter stringWriter = new StringWriter();
                t.printStackTrace(new PrintWriter(stringWriter));
                fail(name+": "+t+"\n"+stringWriter);
            }
        }
    }

    public void testFilterTable() throws Exception
    {
        new AnalysisTestExecutor(AnalysesTest.class.getResource("resources/FilterTable.t")).execute();
    }

    public void testJoinTable() throws Exception
    {
        new AnalysisTestExecutor(AnalysesTest.class.getResource("resources/JoinTable.t")).execute();
    }
    
    public void testMultiJoinTable() throws Exception
    {
        new AnalysisTestExecutor(AnalysesTest.class.getResource("resources/MultiJoinTable.t")).execute();
    }
    
    public void testSelectColumns() throws Exception
    {
        new AnalysisTestExecutor(AnalysesTest.class.getResource("resources/SelectColumns.t")).execute();
    }
    
    public void testUpDownIdentification() throws Exception
    {
        new AnalysisTestExecutor(AnalysesTest.class.getResource("resources/UpDownIdentification.t")).execute();
    }

    public void t_estAnnotateTable() throws Exception
    {
        AnalysisTestExecutor executor = new AnalysisTestExecutor(AnalysesTest.class.getResource("resources/AnnotateTable.t"));
        executor.setRepositoryPath( "../data/test/ru/biosoft/analysis/databases" );
        executor.execute();
    }
    
    public void t_estConvertTable() throws Exception
    {
        AnalysisTestExecutor executor = new AnalysisTestExecutor(AnalysesTest.class.getResource("resources/ConvertTable.t"));
        executor.setRepositoryPath( "../data/test/ru/biosoft/analysis/databases" );
        executor.execute();
    }
    
    public void t_estConvertTableWithIDColumn() throws Exception
    {
        AnalysisTestExecutor executor = new AnalysisTestExecutor(AnalysesTest.class.getResource("resources/ConvertTableWithIDColumn.t"));
        executor.setRepositoryPath( "../data/test/ru/biosoft/analysis/databases" );
        executor.execute();
    }
    
    public void t_estConvertTableWithMaxMatches() throws Exception
    {
        AnalysisTestExecutor executor = new AnalysisTestExecutor(AnalysesTest.class.getResource("resources/ConvertTableWithMaxMatches.t"));
        executor.setRepositoryPath( "../data/test/ru/biosoft/analysis/databases" );
        executor.execute();
    }
}
