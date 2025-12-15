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

    private static final String endl = System.getProperty( "line.separator" );
    private static List<String> statistics = new ArrayList<>();
    private static String[] list = new String[] {"hello", "scatter_range_2_steps", "scatter_simple", "scatter_range", "scatter_range2",
            "two_steps", "four_steps"};

    private static File testsDir;
    private static File descriptionDir;
    private static File imagesDir;
    private static File wdlDir;
    private static File nextflowDir;
    private static File resultsDir;
    private static List<String> tests = new ArrayList<>();
    private static WorkflowReportGenerator workflowReportGenerator;
    private static List<TestResult> testResults = new ArrayList<>();

    public static void main(String ... args) throws Exception
    {

        URL url = TestWDL.class.getResource( "../test_examples/" );

        testsDir = new File( url.toURI() );
        resultsDir = new File( testsDir, "results" );
        TestNextflow tester = new TestNextflow();
        workflowReportGenerator = new WorkflowReportGenerator();
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
        tester.test( "scatter_simple" );
        tester.test( "double_scatter" );
        tester.test( "two_steps3" );
        tester.test( "two_inputs_cycle" );
        tester.test( "scatter_simple" );
        //        DO NOT WORK

        //        test( "nested_cycles" );
        //                test( "double_scatter2" );
        //double if

        //        test("call_expr_call");

        //        test("align");
        //                test( "struct_to_struct" );
        //                test( "array_objects" );
        //test("hic2");

        //Caclulations in ouput
        //Expressions from call passed to another call


        //        test( "call_mix_expr");

        //        generateHTML( tests );
        //        generateRST( tests );
        tester.generateStatistics(testResults);
        System.out.println( "\n\nRESULT:" );
        for( String result : statistics )
            System.out.println( result );


        //        for (String name: list)
        //            test(name);
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
    
    private void generateStatistics(List<TestResult> results) throws Exception
    {
        TestsReportGenerator generator = new TestsReportGenerator();
        String html = generator.generate( results, resultsDir );
        ApplicationUtils.writeString( new File( resultsDir, "report.html" ), html );
        
    }

    public void test(String name) throws Exception
    {
        String originalWDL = TestUtil.loadWDL( name );
        Diagram diagram = TestUtil.generateDiagram( name, originalWDL );
        tests.add( name );


        NextFlowGenerator nextFlowGenerator = new NextFlowGenerator();
        String nextflow = nextFlowGenerator.generate( diagram );

        WDLGenerator wdlGenerator = new WDLGenerator();
        String generatedWDL = wdlGenerator.generate( diagram );
        //        System.out.println( "Exported Nextflow: " );
        //        System.out.println( nextflow );

        testResults.add(new TestResult(name, "Ok", "Ok"));
        saveResults( name, TestUtil.loadDescription(name), originalWDL, generatedWDL, nextflow, diagram );
        //        saveNextflow( name, nextflow );
        //        saveWDL( name, generatedWDL );
        //
        //
        //        String report = ;
        //        saveReport(name, report);
        //        String json = getParameters( name );
        //        boolean success = runNextFlow( name, nextflow, json );
        //        statistics.add( name + " " + success );
    }



    private static String getParameterFile(String name) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/wdl/" + name );
        if( url == null )
            return null;
        return ApplicationUtils.readAsString( new File( url.getFile() ) );
    }

    private void saveResults(String name, String description, String originalWDL, String generatedWDL, String nextflow, Diagram diagram) throws Exception
    {
        File dir = new File( resultsDir, name );
        dir.mkdirs();
        TestUtil.exportImage( new File( dir, name + ".png" ), diagram );
        ApplicationUtils.writeString( new File( dir, name + ".nf" ), nextflow );
        ApplicationUtils.writeString( new File( dir, name + ".txt" ), description );
        ApplicationUtils.writeString( new File( dir, name + ".wdl" ), originalWDL );
        ApplicationUtils.writeString( new File( dir, name + "_exported.wdl" ), generatedWDL );
        ApplicationUtils.writeString( new File( dir, name + ".html" ), workflowReportGenerator.generate( name, dir ) );

    }

    private static String getParameters(String name) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/wdl/" + name + ".json" );
        if( url == null )
            return null;
        return ApplicationUtils.readAsString( new File( url.getFile() ) );
    }

    private static void checkScript(String name, String nextFlow) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/nextflow/" + name + ".nf" );
        String test = ApplicationUtils.readAsString( new File( url.getFile() ) );
        //        assertEquals( test, nextFlow );
    }


    private static boolean runNextFlow(String name, String script, String parameters)
    {
        return runNextFlow( name, script, parameters, new ArrayList<String>() );
    }

    private static boolean runNextFlow(String name, String script, String parameters, List<String> imports)
    {
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

            //            NextFlowPreprocessor preprocessor = new NextFlowPreprocessor();
            //            script = preprocessor.preprocess( script );
            File f = new File( outputDir, name + ".nf" );
            ApplicationUtils.writeString( f, script );

            if( parameters != null )
            {
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
                String[] command = new String[] {"wsl", "--cd", parent, "nextflow", f.getName(), "-params-file", jsonFile.getName()};
                return TestUtil.executeCommand( command );
            }
            else
            {
                String[] command = new String[] {"wsl", "--cd", parent, "nextflow", f.getName()};
                return TestUtil.executeCommand( command );
            }

        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            return false;
        }
    }

    public static class TestResult
    {
        private String name;
        private String wdlGeneration = "Ok";
        private String diagramGeneration = "Ok";
        private String nextflowGeneration = "Ok";
        
        public TestResult(String name, String wdlGeneration, String nextflowGeneration)
        {
            this.name = name;
            this.wdlGeneration = wdlGeneration;
            this.nextflowGeneration = nextflowGeneration;
        }
        
        public String getName()
        {
            return name;
        }
        
        public String getDiagramGeneration()
        {
            return diagramGeneration;
        }
        
        public String getWDLGeneration()
        {
            return wdlGeneration;
        }
        
        public String getNextflowGeneration()
        {
            return nextflowGeneration;
        }
    }
}