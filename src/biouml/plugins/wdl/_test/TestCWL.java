package biouml.plugins.wdl._test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.CWLGenerator;
//import junit.framework.TestCase;
import biouml.plugins.wdl.CWLRunner;
import biouml.plugins.wdl.NextFlowGenerator;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.diagram.WDLLayouter;
import ru.biosoft.util.TempFiles;

public class TestCWL
{

    private static String[] list = new String[] {"hello", "scatter_range_2_steps", "scatter_simple", "scatter_range", "scatter_range2",
            "two_steps", "four_steps"};

    public static void main(String ... args) throws Exception
    {
        //        test( "faidx" );

//        test( "two_steps" );
//        test( "two_steps" );
        test("aggregate");
        //        for (String name: list)
        //            test(name);

        //                test( "scatter_range_2_extra" );
        //                test( "scatter_range_2_steps" );
        //                test( "scatter_simple" );
        //        test( "scatter_range" );
        //                test( "scatter_range2" );
        //        test( "two_steps" );
        //        test( "four_steps" );
        //        test( "private_declaration_task" );
        //                test( "pbmm2" );
        //        test( "pbsv_1" );
        //        test( "array_input" );
        // test( "array_select");


        //        test( "array_on_the_fly" );
        //        test( "lima" );

        //        test("faidx_import");
        //        test( "fastqc1" );
        //        test( "test_scatter" );
    }

    public static void test() throws Exception
    {
        test( "two_steps" );
    }

    public static void test(String name) throws Exception
    {
        Diagram diagram = TestUtil.loadDiagramCWL( name );
        WDLLayouter.layout( diagram );
        TestWDL.exportImage(diagram, new File("C:/Users/Damag/cwl.png"));
        
        String cwl =  new CWLGenerator().generate( diagram );
        System.out.println( cwl );
//        String nextflow =  new NextFlowGenerator().generate( diagram );
//        System.out.println( nextflow );
//        CWLGenerator cwlGenerator = new CWLGenerator();

//        String cwl = cwlGenerator.generate( diagram );
//        System.out.println( "Exported CWL: " );
//        System.out.println( cwl );

//        runCWL( name, cwl );
        //        checkScript( name, nextFlow );
    }

    private static void runCWL(String name, String script)
    {
        runCWL( name, script, new ArrayList<>() );
    }

    private static void runCWL(String name, String script, List<String> imports)
    {
        try
        {
            String outputDir = TempFiles.path( "cwl" ).getAbsolutePath();
            new File( outputDir ).mkdirs();

            for( String imported : imports )
            {
                URL url = TestWDL.class.getResource( "../test_examples/cwl/" + imported + ".cwl" );
                if( url == null )
                    throw new IllegalArgumentException( "No input file exists: " + name );

                File file = new File( url.getFile() );
                File copy = new File( outputDir, file.getName() );
                ApplicationUtils.copyFile( copy, file );
            }

            File f = new File( outputDir, name + ".cwl" );
            ApplicationUtils.writeString( f, script );
            Process process;
            URL url = TestWDL.class.getResource( "../test_examples/cwl/" + name + ".json" );
            if( url != null )
            {
                File parametersFile = new File( url.getFile() );
                File paremetersCopy = new File( outputDir, parametersFile.getName() );
                ApplicationUtils.copyFile( paremetersCopy, parametersFile );
                String content = ApplicationUtils.readAsString( parametersFile );
                Set<String> files = WorkflowSettings.getFileInputs( content );
                for( String file : files )
                {
                    URL inputURL = TestWDL.class.getResource( "../test_examples/cwl/" + file );
                    File inputFile = new File( inputURL.getFile() );
                    File copy = new File( outputDir, inputFile.getName() );
                    ApplicationUtils.copyFile( copy, inputFile );
                }
                process = CWLRunner.run( f.getName(), name + ".json", outputDir, false );
            }
            else
            {
                process = CWLRunner.run( f.getName(), null, outputDir, false );
            }
            CWLRunner.logProcess( process );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private static void checkScript(String name, String cwl) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/cwl/" + name + ".cwl" );
        File f = new File( url.getFile() );
        String test = ApplicationUtils.readAsString( new File( url.getFile() ) );
        //        assertEquals( test, cwl );
    }

    private static void saveResult(String name, String nextFlow) throws Exception
    {
        File f = new File( "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/nextflow/" + name + ".nf" );
        ApplicationUtils.writeString( f, nextFlow );
    }
}
