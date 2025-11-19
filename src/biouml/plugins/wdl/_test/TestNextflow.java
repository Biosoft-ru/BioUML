package biouml.plugins.wdl._test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.developmentontheedge.application.ApplicationUtils;
import biouml.model.Diagram;
import biouml.plugins.wdl.NextFlowGenerator;
import biouml.plugins.wdl.NextFlowRunner;
import biouml.plugins.wdl.WorkflowUtil;
import ru.biosoft.util.TempFiles;

public class TestNextflow //extends //TestCase
{

    private static List<String> statistics = new ArrayList<>();
    private static String[] list = new String[] {"hello", "scatter_range_2_steps", "scatter_simple", "scatter_range", "scatter_range2",
            "two_steps", "four_steps"};

    public static void main(String ... args) throws Exception
    {

        //CHECKED:
        //        test("hello_world");
        //                test( "simple_if" );
        //                test( "cycle_expressions" );
        //                test( "cycle_expressions2" );
        //                test( "cycle_expressions3" );
        //                test( "scatter_range" );
        //                test( "scatter_range2" );
        //                test( "scatter_simple" );
        //                test( "four_steps" );
        //                test( "cycle_expression_call" );
        //                test( "cycle_expression_call2" );
        //                test( "array_input" );
        //                test( "array_input2" );
        //                test( "nested_access" );
        //                test( "two_inputs" );
        //                test( "two_steps" );
        //                test( "two_steps2" );
        //                test( "two_steps3" );
        //                test("two_inputs_cycle");
        //                test("test_map"); 
        //        test("private_declaration");
        //        test("object_output");
        //        test("object_output2");
        //        test("test_scatter");
        //        test( "array_select" );
        //        test( "nested_access2");
        //        test( "array_select2" );
        //        test( "double_scatter");
        //        test( "double_scatter2");
        //        DO NOT WORK

        //test("hic2");

        //Caclulations in ouput
        //Expressions from call passed to another call


        //        test( "call_mix_expr");

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

    public static void test() throws Exception
    {
        test( "hello" );
    }

    public static void testOrder(String name) throws Exception
    {
        Diagram diagram = TestUtil.loadDiagram( name );
        WorkflowUtil.orderCallsScatters( diagram );
    }
    public static void test(String name) throws Exception
    {
        Diagram diagram = TestUtil.loadDiagram( name );
        NextFlowGenerator nextFlowGenerator = new NextFlowGenerator();
        String nextflow = nextFlowGenerator.generate( diagram );
        String json = getParameters( name );
        System.out.println( "Exported Nextflow: " );
        System.out.println( nextflow );
        boolean success = runNextFlow( name, nextflow, json );
        statistics.add( name + " " + success );
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
        File f = new File( url.getFile() );
        String test = ApplicationUtils.readAsString( new File( url.getFile() ) );
        //        assertEquals( test, nextFlow );
    }

    private static void saveResult(String name, String nextFlow) throws Exception
    {
        File f = new File( "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/nextflow/" + name + ".nf" );
        ApplicationUtils.writeString( f, nextFlow );
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
                File json = new File( outputDir, name + ".json" );
                ApplicationUtils.writeString( json, parameters );
                String[] command = new String[] {"wsl", "--cd", parent, "nextflow", f.getName(), "-params-file", json.getName()};
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


    public static File generateFunctions(String outputDir) throws IOException
    {
        String content = """
                def basename(filePath) {
                    return new File(filePath.toString()).getName()
                }

                def sub(input, pattern, replacement) {
                    return input.replaceAll(pattern, replacement)
                }

                def ceil(val) {
                    return Math.ceil(val)
                }

                def length(arr) {
                    return arr.size()
                }

                def range(n) {
                    return (0..<n).toList()
                }
                """;
        File result = new File( outputDir, "biouml_function.nf" );
        ApplicationUtils.writeString( result, content );
        return result;
    }
}
