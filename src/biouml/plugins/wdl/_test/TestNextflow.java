package biouml.plugins.wdl._test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;
import biouml.model.Diagram;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.nextflow.NextFlowRunner;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.WorkflowUtil;

public class TestNextflow //extends //TestCase
{
    private static boolean validateWDL = true;
    private static String WOM_TOOL_PATH = "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/wdl/womtool-91.jar";

    private static boolean executeNextflow = true;

    private static String[] list = new String[] {"hello_world", "simple_if", "cycle_expressions", "cycle_expressions2",
            "cycle_expressions3", "scatter_range", "four_steps"};

    private File testsDir;
    private File resultsDir;
    private WorkflowReportGenerator workflowReportGenerator = new WorkflowReportGenerator();
    private List<TestResult> testResults = new ArrayList<>();

    public static void main(String ... args) throws Exception
    {
        TestNextflow tester = new TestNextflow();
        tester.init( TestNextflow.class.getResource( "resources/test_suite/" ) );

        //CHECKED:
        tester.test( "hello_world" );
        tester.test( "two_steps" );
        tester.test( "two_steps2" );
        tester.test( "two_steps3" );
        tester.test( "four_steps" );
        tester.test( "scatter_simple" );
        tester.test( "test_scatter" );
        tester.test( "call_expr_call2" );
        tester.test( "simple_if" );
        tester.test( "cycle_expressions" );
        tester.test( "cycle_expressions2" );
        tester.test( "cycle_expressions3" );
        tester.test( "scatter_range" );
        tester.test( "scatter_range2" );
        tester.test( "private_declaration" );
        tester.test( "test_map" );
        tester.test( "object_output" );
        tester.test( "object_output2" );
        tester.test( "nested_access" );
        tester.test( "nested_access2" );
        tester.test( "array_select" );
        tester.test( "array_select2" );
        tester.test( "array_input" );
        tester.test( "array_input2" );
        tester.test( "array_input3" );
        tester.test( "two_inputs" );
        tester.test( "two_inputs_cycle" );
        tester.test( "cycle_expression_call" );
        tester.test( "cycle_expression_call2" );

        //        DO NOT WORK

        //      tester.test( "double_scatter" );

        //        test( "nested_cycles" );
        //                test( "double_scatter2" );
        //        test("call_expr_call");
        //        test("align");
        //                test( "struct_to_struct" );
        //                test( "array_objects" );
        //test("hic2");
        //        test( "call_mix_expr");

        //                test( "scatter_range_2_extra" );
        //                test( "scatter_range_2_steps" );
        //                test( "pbmm2" );
        //        test( "pbsv_1" );
        //      test( "call_mix_expr");
        //        test( "lima" );
        //        test( "faidx2" );
        //        test( "extra_steps");
        //        test( "scatter_extra_steps" );
        //        test("faidx_import");
        //        test( "fastqc1" );

        tester.generateStatistics( tester.testResults );
    }

    public void init(URL url) throws Exception
    {
        File suiteDir = new File( url.toURI() );
        testsDir = new File( suiteDir, "tests" );
        resultsDir = new File( suiteDir, "results" );
        TestUtil.deleteDir( resultsDir );
        resultsDir.mkdir();
    }

    private void generateStatistics(List<TestResult> results) throws Exception
    {
        TestsReportGenerator generator = new TestsReportGenerator();
        String html = generator.generate( results, resultsDir );
        ApplicationUtils.writeString( new File( resultsDir, "report.html" ), html );

    }

    public void test(String name) throws Exception
    {
        TestResult testResult = new TestResult( name );
        File testDir = new File( testsDir, name );
        String originalWDL = ApplicationUtils.readAsString( new File( testDir, name + ".wdl" ) );
        Diagram diagram = null;
        String nextflow = null;
        String generatedWDL = null;
        String validated = null;
        String roundWDL = null;
        File resultDir = new File( resultsDir, name );
        resultDir.mkdirs();

        copyFiles( testDir, resultDir );

        //1. Generate diagram
        try
        {
            diagram = TestUtil.generateDiagram( name, originalWDL );
            if( diagram != null )
                testResult.setDiagramGenerated( TestUtil.TEST_OK );
        }
        catch( Exception ex )
        {
            testResult.setDiagramGenerated( ex.toString() );
        }

        if( diagram != null )
        {
//            testResult.setTitle( WorkflowUtil.getTitle( diagram ) );
//            testResult.setDescrption( WorkflowUtil.getShortDescription( diagram ) );
            //2. Generate WDL
            try
            {
                generatedWDL = new WDLGenerator().generate( diagram );
                if( generatedWDL != null )
                    testResult.setWDLGenerated( TestUtil.TEST_OK );
            }
            catch( Exception ex )
            {
                testResult.setWDLGenerated( ex.toString() );
            }

            //3. Round test
            try
            {
                Diagram roundDiagram = TestUtil.generateDiagram( name, generatedWDL );
                roundWDL = new WDLGenerator().generate( roundDiagram );
                if( roundWDL != null && roundWDL.equals( generatedWDL ) )
                    testResult.setRoundTest( TestUtil.TEST_OK );
            }
            catch( Exception ex )
            {
                testResult.setRoundTest( ex.toString() );
            }

            //4. Generate nextflow
            try
            {
                NextFlowGenerator nextFlowGenerator = new NextFlowGenerator();
                nextflow = nextFlowGenerator.generate( diagram );
                if( nextflow != null )
                    testResult.setNextflowGenerated( TestUtil.TEST_OK );

            }
            catch( Exception ex )
            {
                testResult.setNextflowGenerated( ex.toString() );
            }

            //5. Execute nextflow
            if( executeNextflow )
            {
                try
                {
                    File jsonFile = new File( testDir, name + ".json" );
                    String json = jsonFile.exists() ? ApplicationUtils.readAsString( jsonFile ) : null;
                    String nextFlowExecuted = runNextFlow( testDir, resultDir, name, nextflow, json );
                    testResult.setNextflowExecuted( nextFlowExecuted );
                }
                catch( Exception ex )
                {
                    testResult.setNextflowExecuted( ex.toString() );
                }
            }
            saveResults( name, resultDir, "//TODO" , roundWDL, generatedWDL, nextflow, diagram );

            //6. Validate WDL (optional)
            if( !validateWDL )
                validated = "N/A";
            else if( generatedWDL != null )
                validated = TestUtil.validateWDL( new File( new File( resultsDir, name ), name + "_exported.wdl" ).getAbsolutePath(),
                        WOM_TOOL_PATH );
            testResult.setWDLValidated( validated );
        }

        //        saveInput( name, originalWDL, nextflow );
        testResults.add( testResult );
        //        System.out.println( generatedWDL );
    }

    private void copyFiles(File source, File target) throws Exception
    {
        for( File s : source.listFiles() )
        {
            if( s.getName().endsWith( ".nf" ) )
                continue;
            File copy = new File( target, s.getName() );
            copy.createNewFile();
            String content = ApplicationUtils.readAsString( s );
            ApplicationUtils.writeString( copy, content );
        }
    }

    private void saveResults(String name, File resultDir, String description, String roundWDL, String generatedWDL, String nextflow,
            Diagram diagram) throws Exception
    {
        if( diagram != null )
            TestUtil.exportImage( new File( resultDir, name + ".png" ), diagram );
        if( nextflow != null )
            ApplicationUtils.writeString( new File( resultDir, name + ".nf" ), nextflow );
        if( description != null )
            ApplicationUtils.writeString( new File( resultDir, name + ".txt" ), description );
        if( generatedWDL != null )
            ApplicationUtils.writeString( new File( resultDir, name + "_exported.wdl" ), generatedWDL );
        if( roundWDL != null )
            ApplicationUtils.writeString( new File( resultDir, name + "_round.wdl" ), roundWDL );
        ApplicationUtils.writeString( new File( resultDir, name + ".html" ), workflowReportGenerator.generate( name, resultDir ) );
    }

    private static void checkScript(String name, String nextFlow) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/nextflow/" + name + ".nf" );
        String test = ApplicationUtils.readAsString( new File( url.getFile() ) );
        //        assertEquals( test, nextFlow );
    }

    private static String runNextFlow(File testDir, File resultDir, String name, String script, String parameters)
    {
        return runNextFlow( testDir, resultDir, name, script, parameters, new ArrayList<String>() );
    }

    private static String runNextFlow(File testDir, File resultDir, String name, String script, String parameters, List<String> imports)
    {
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        try
        {
            NextFlowRunner.generateFunctions( resultDir );

            for( String imported : imports )
            {
                File file = new File( testDir, imported + ".nf" );
                File copy = new File( resultDir, file.getName() );
                ApplicationUtils.copyFile( copy, file );
            }

            File f = new File( resultDir, name + ".nf" );
            ApplicationUtils.writeString( f, script );

            ProcessBuilder builder = null;

            if( parameters != null )
            {
                String jsonName = name + ".json";
                if( isWindows )
                {
                    builder = new ProcessBuilder( "wsl", "--cd", resultDir.getAbsolutePath(), "nextflow", f.getName(), "-params-file",
                            jsonName );
                }
                else
                {
                    builder = new ProcessBuilder( "nextflow", f.getName(), "-params-file", jsonName );
                    builder.directory( resultDir );
                }
            }
            else
            {
                if( isWindows )
                {
                    builder = new ProcessBuilder( "wsl", "--cd", resultDir.getAbsolutePath(), "nextflow", f.getName() );
                }
                else
                {
                    builder = new ProcessBuilder( "nextflow", f.getName() );
                    builder.directory( resultDir );
                }
            }
            return TestUtil.executeProcess( builder.start() );

        }
        catch( Exception ex )
        {
            return ex.toString();
        }
    }
}