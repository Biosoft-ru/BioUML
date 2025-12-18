package biouml.plugins.wdl._test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;
import biouml.model.Diagram;
import biouml.plugins.wdl.NextFlowGenerator;
import biouml.plugins.wdl.NextFlowRunner;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.WorkflowSettings;
import ru.biosoft.util.TempFiles;

public class TestNextflow //extends //TestCase
{
    private static boolean validateWDL = true;
    private static String WOM_TOOL_PATH = "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/wdl/womtool-91.jar";

    private static boolean executeNextflow = true;

    private static String[] list = new String[] {"hello_world", "simple_if", "cycle_expressions", "cycle_expressions2",
            "cycle_expressions3", "scatter_range", "four_steps"};

    private File testsDir;
    private File resultsDir;
    private List<String> tests = new ArrayList<>();
    private WorkflowReportGenerator workflowReportGenerator  = new WorkflowReportGenerator();
    private List<TestResult> testResults = new ArrayList<>();

    public static void main(String ... args) throws Exception
    {
        TestNextflow tester = new TestNextflow();
        tester.init( TestWDL.class.getResource( "../test_examples/"));

        //CHECKED:
        tester.test( "hello_world" );
        tester.test( "simple_if" );
        tester.test( "cycle_expressions" );
        tester.test( "cycle_expressions2" );
        tester.test( "cycle_expressions3" );
        tester.test( "scatter_range" );
        tester.test( "four_steps" );
        tester.test( "array_input" );
        tester.test( "array_input2" );
        tester.test( "array_input3" );
        tester.test( "nested_access" );
        tester.test( "two_inputs" );
        tester.test( "two_steps" );
        tester.test( "private_declaration" );
        tester.test( "object_output" );
        tester.test( "object_output2" );
        tester.test( "call_expr_call2" );
        tester.test( "array_select2" );
        tester.test( "test_scatter" );
        tester.test( "cycle_expression_call" );
        tester.test( "cycle_expression_call2" );
        tester.test( "two_steps2" );
        tester.test( "test_map" );
        tester.test( "array_select" );
        tester.test( "nested_access2" );
        tester.test( "scatter_range2" );
        tester.test( "double_scatter" );
        tester.test( "two_steps3" );
        tester.test( "two_inputs_cycle" );
        tester.test( "scatter_simple" );
        //        DO NOT WORK

        //        test( "nested_cycles" );
        //                test( "double_scatter2" );
        //        test("call_expr_call");
        //        test("align");
        //                test( "struct_to_struct" );
        //                test( "array_objects" );
        //test("hic2");
        //        test( "call_mix_expr");

        tester.generateStatistics( tester.testResults );

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
    }

    public void init(URL url) throws Exception
    {
        testsDir = new File( url.toURI() );
        resultsDir = new File( testsDir, "results" );
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
        tests.add( name );
        TestResult testResult = new TestResult( name );
        File testDir = TestUtil.loadTestFolder( name );
        String originalWDL = ApplicationUtils.readAsString( new File( testDir, name + ".wdl" ) );
        Diagram diagram = null;
        String nextflow = null;
        String generatedWDL = null;
        String validated = null;
        String roundWDL = null;
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
                    String nextFlowExecuted = runNextFlow( name, nextflow, json );
                    testResult.setNextflowExecuted( nextFlowExecuted );
                }
                catch( Exception ex )
                {
                    testResult.setNextflowExecuted( ex.toString() );
                }
            }
            saveResults( name, TestUtil.loadDescription( name ), originalWDL, roundWDL, generatedWDL, nextflow, diagram );

            //5. Validate WDL (optional)
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

    private static String getParameterFile(String name) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/wdl/" + name );
        if( url == null )
            return null;
        return ApplicationUtils.readAsString( new File( url.getFile() ) );
    }

    private void saveInput(String name, String originalWDL, String nextflow) throws Exception
    {
        File dir = new File( testsDir, name );
        dir.mkdirs();
        ApplicationUtils.writeString( new File( dir, name + ".wdl" ), originalWDL );
        ApplicationUtils.writeString( new File( dir, name + ".nf" ), nextflow );
    }

    private void saveResults(String name, String description, String originalWDL, String roundWDL, String generatedWDL, String nextflow,
            Diagram diagram) throws Exception
    {
        File dir = new File( resultsDir, name );
        dir.mkdirs();
        if( diagram != null )
            TestUtil.exportImage( new File( dir, name + ".png" ), diagram );
        if( nextflow != null )
            ApplicationUtils.writeString( new File( dir, name + ".nf" ), nextflow );
        if( description != null )
            ApplicationUtils.writeString( new File( dir, name + ".txt" ), description );
        if( originalWDL != null )
            ApplicationUtils.writeString( new File( dir, name + ".wdl" ), originalWDL );
        if( generatedWDL != null )
            ApplicationUtils.writeString( new File( dir, name + "_exported.wdl" ), generatedWDL );
        if( roundWDL != null )
            ApplicationUtils.writeString( new File( dir, name + "_round.wdl" ), roundWDL );
        ApplicationUtils.writeString( new File( dir, name + ".html" ), workflowReportGenerator.generate( name, dir ) );

    }

    private static void checkScript(String name, String nextFlow) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/nextflow/" + name + ".nf" );
        String test = ApplicationUtils.readAsString( new File( url.getFile() ) );
        //        assertEquals( test, nextFlow );
    }


    private static String runNextFlow(String name, String script, String parameters)
    {
        return runNextFlow( name, script, parameters, new ArrayList<String>() );
    }

    private static String runNextFlow(String name, String script, String parameters, List<String> imports)
    {
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        try
        {
            String outputDir = TempFiles.path( "nextflow" ).getAbsolutePath();
            new File( outputDir ).mkdirs();
            NextFlowRunner.generateFunctions( outputDir );


            for( String imported : imports )
            {
                URL url = TestWDL.class.getResource( "../test_examples/nextflow/" + imported + ".nf" );
                if( url == null )
                    throw new IllegalArgumentException( "No input file exists: " + name );

                File file = new File( url.getFile() );
                File copy = new File( outputDir, file.getName() );
                ApplicationUtils.copyFile( copy, file );
            }

            String parent = new File( outputDir ).getAbsolutePath().replace( "\\", "/" );

            File f = new File( outputDir, name + ".nf" );
            ApplicationUtils.writeString( f, script );

            ProcessBuilder builder = null;

            if( parameters != null )
            {
                String jsonName = name + ".json";
                File jsonFile = new File( outputDir, name + ".json" );
                String[] paramLines = parameters.replace( "{", "" ).replace( "}", "" ).replace( "\"", "" ).split( "," );
                for( String parameter : paramLines )
                {
                    try
                    {
                        String paramName = parameter.split( ":" )[1];
                        paramName = paramName.trim().replace( "\n", "" ).replace( ",", "" );
                        String paramContent = getParameterFile( paramName );
                        if( paramContent != null )
                        {
                            File paramFile = new File( outputDir, paramName );
                            ApplicationUtils.writeString( paramFile, paramContent );
                        }
                    }
                    catch( Exception ex )
                    {

                    }
                }
                for( String s : WorkflowSettings.getFileInputs( parameters ) )
                {

                }
                ApplicationUtils.writeString( jsonFile, parameters );

                if( isWindows )
                {
                    builder = new ProcessBuilder( "wsl", "--cd", parent, "nextflow", f.getName(), "-params-file", jsonName );
                }
                else
                {
                    builder = new ProcessBuilder( "nextflow", f.getName(), "-params-file", jsonName );
                    builder.directory( new File( outputDir ) );
                }
            }
            else
            {
                if( isWindows )
                {
                    builder = new ProcessBuilder( "wsl", "--cd", parent, "nextflow", f.getName() );
                }
                else
                {
                    builder = new ProcessBuilder( "nextflow", f.getName() );
                    builder.directory( new File( outputDir ) );
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