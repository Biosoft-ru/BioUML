package biouml.plugins.wdl._test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.developmentontheedge.application.ApplicationUtils;
import biouml.model.Diagram;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.nextflow.NextFlowImporter;
import biouml.plugins.wdl.nextflow.NextFlowPreprocessor;
import biouml.plugins.wdl.nextflow.NextFlowRunner;
import biouml.plugins.wdl.FileScriptLoader;
import biouml.plugins.wdl.ScriptLoader;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.diagram.DiagramGenerator;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.model.ScriptInfo;

public class TestNextflow
{
    private static boolean validateWDL = true;
    private static String WOM_TOOL_PATH = "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/wdl/womtool-92.jar";

    public boolean executeNextflow = true;
    private File testsDir;
    private File resultsDir;
    private WorkflowReportGenerator workflowReportGenerator = new WorkflowReportGenerator();
    private List<TestResult> testResults = new ArrayList<>();
    private File yamlFile = null;
private int limit = 1000;
private String selected =  null;//"string_placeholders_1.1";//"write_map_as_command";//"sub_as_input_with_file";//"dedent";//"round_as_input";//"output_collision";//"transpose_as_input";
File suiteDir = null;
    public static void main(String ... args) throws Exception
    {
        testWDL( "resources/wdl-conformance-tests", "conformance.yaml" );

        //        testWDL("resources/test_suite");
    }

    public static void testWDL(String path, String yamlFileName) throws Exception
    {
        TestNextflow tester = new TestNextflow();
        tester.init( TestNextflow.class.getResource( path ), yamlFileName );
        tester.test(tester.yamlFile);

        //        tester.test("array_coerce");
        tester.generateStatistics( tester.testResults );
    }

    public static void testWDL(String path) throws Exception
    {
        TestNextflow tester = new TestNextflow();
        tester.init( TestNextflow.class.getResource( path ), null );
        tester.testAll();

        //        tester.test("array_coerce");
        tester.generateStatistics( tester.testResults );
    }

    public static void testWDL1() throws Exception
    {
        TestNextflow tester = new TestNextflow();
        tester.init( TestNextflow.class.getResource( "resources/test_suite" ), null );

        //CHECKED:
        tester.test( "hello_world" );
        tester.test( "two_steps" );
        tester.test( "two_steps2" );
        tester.test( "two_steps3" );
        tester.test( "four_steps" );
        tester.test( "scatter_simple" );
        tester.test( "call_expr_call2" );
        tester.test( "simple_if" );
        tester.test( "scatter_range" );
        tester.test( "scatter_range2" );
        tester.test( "private_declaration" );
        tester.test( "cycle_expressions" );
        tester.test( "cycle_expressions2" );
        tester.test( "cycle_expressions3" );
        tester.test( "test_map" );
        tester.test( "array_select" );
        tester.test( "array_select2" );
        tester.test( "array_input" );
        tester.test( "array_input2" );
        tester.test( "array_input3" );
        tester.test( "two_inputs" );
        tester.test( "cycle_expression_call" );
        tester.test( "cycle_expression_call2" );
        tester.test( "nested_access" );
        tester.test( "nested_access2" );
        tester.test( "test_scatter" );
        tester.test( "object_output" );
        tester.test( "object_output2" );
        tester.test( "two_inputs_cycle" );

        //        DO NOT WORK

        //      tester.test( "double_scatter" );

        //        test( "nested_cycles" );
        //                test( "double_scatter2" );
        //        test("call_expr_call");
        //        test("align");
        //                test( "struct_to_struct" );
        //        tester.test( "array_objects" );
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

    public static void testNextflow() throws Exception
    {
        TestNextflow tester = new TestNextflow();
        tester.init( TestNextflow.class.getResource( "resources/test_suite/nextflow" ), null );
        tester.testNextflow( "main" );
    }

    public void init(File testDir, File resultDir) throws Exception
    {
        testsDir = testDir;
        resultsDir = resultDir;
        TestUtil.deleteDir( resultsDir );
        resultsDir.mkdir();
    }

    public void init(URL url, String yamlFileName) throws Exception
    {
        suiteDir = new File( url.toURI() );
        testsDir = new File( suiteDir, "tests" );
        resultsDir = new File( suiteDir, "results" );
        if( yamlFileName != null )
            this.yamlFile = new File( suiteDir, yamlFileName );
        TestUtil.deleteDir( resultsDir );
        resultsDir.mkdir();
    }

    private void generateStatistics(List<TestResult> results) throws Exception
    {
        TestsReportGenerator generator = new TestsReportGenerator();
        String html = generator.generate( results, resultsDir );
        ApplicationUtils.writeString( new File( resultsDir, "report.html" ), html );

    }

    public void testNextflow(String name) throws Exception
    {
        System.out.println( "TESTING " + name );
        File testDir = new File( testsDir, name );
        String originalNextflow = ApplicationUtils.readAsString( new File( testDir, name + ".nf" ) );
        Diagram diagram = new NextFlowImporter().importNextflow( originalNextflow );
        NextFlowGenerator nextFlowGenerator = new NextFlowGenerator();
        String nextflow = nextFlowGenerator.generate( diagram );
        System.out.println( nextflow );
    }

    public void test(File yamlFile) throws Exception
    {
        Yaml parser = new Yaml();
        Object obj = parser.load( ApplicationUtils.readAsString(yamlFile) );
        List<Object> rootMap = (List<Object>)obj;
        int current = -1;
        for (Object test: rootMap)
        {
            current++;
            if (current > limit)
                continue;

            Map<Object, Object> testMap = (Map<Object, Object>)test;
            String description = testMap.get( "description").toString();
            String id = testMap.get( "id" ).toString();
            Object inputs = testMap.get( "inputs" );
            Map<Object, Object> inputsMap = (Map<Object, Object>)inputs;
            String testPath = inputsMap.get( "dir" ).toString();
            String testName = inputsMap.get( "wdl" ).toString();
            String testJSON = inputsMap.get( "json" ).toString();
            
           
//            Path olderPath = yamlFile.getParentFile().toPath();
            Path testAbsolutePath = Path.of( yamlFile.getParentFile().getAbsolutePath(), testPath );
            File testDir = testAbsolutePath.toFile();
            
            if (selected != null && !testDir.getName().equals( selected ))
                continue;
            test(id, description, testDir, testName, testJSON);

        }
    }

    public void testAll() throws Exception
    {
        if (testsDir == null)
            return;
        for( String test : testsDir.list() )
        {
            test( test );
        }
    }

    public void test(String testName)
    {
        
    }
    
    public void test(String id, String description, File testDir, String wdlName, String jsonName)
    {
        String name = testDir.getName();
        System.out.println( "TESTING " + name );
        TestResult testResult = new TestResult( name );
        testResult.setDescrption( description );
        try
        {
//            File testDir = new File( testDir, wdlName );
            String originalWDL = ApplicationUtils.readAsString( new File( testDir, wdlName ) );
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
                WDLImporter importer = new WDLImporter();
                importer.setScriptLoader( new FileScriptLoader( ScriptLoader.WDL_TYPE, testDir ) );
                ScriptInfo info = importer.readScript( name, originalWDL );
                diagram = new DiagramGenerator().generateDiagram( info, new WDLDiagramType().createDiagram( null, name ) );
                //                diagram = TestUtil.generateDiagram( name, originalWDL );
            }
            catch( Exception ex )
            {
                testResult.setDiagramGenerated( ex.toString() );
            }

            if( diagram != null )
            {
                testResult.setDiagramGenerated( TestUtil.TEST_OK );
                //            testResult.setTitle( WorkflowUtil.getMeta( diagram ).get( "Name" ) );
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
//                        File jsonFile = new File( testDir, jsonName);
//                        String json = jsonFile.exists() ? ApplicationUtils.readAsString( jsonFile ) : null;
                        String nextFlowExecuted = runNextFlow( suiteDir, testDir, resultDir, name, nextflow, jsonName );
                        testResult.setNextflowExecuted( nextFlowExecuted );
                    }
                    catch( Exception ex )
                    {
                        testResult.setNextflowExecuted( ex.toString() );
                    }
                }
                saveResults( name, wdlName, resultDir, description, roundWDL, generatedWDL, nextflow, diagram );

                //6. Validate WDL (optional)
                if( !validateWDL )
                    validated = "N/A";
                else if( generatedWDL != null )
                    validated = TestUtil.validateWDL( new File( new File( resultsDir, name ), name + "_exported.wdl" ).getAbsolutePath(),
                            WOM_TOOL_PATH );
                testResult.setWDLValidated( validated );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            testResult.setError( ex.getMessage() );
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

    private void saveResults(String name, String wdlPath, File resultDir, String description, String roundWDL, String generatedWDL, String nextflow,
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
        ApplicationUtils.writeString( new File( resultDir, name + ".html" ), workflowReportGenerator.generate( name, wdlPath, resultDir ) );
    }

    private static void checkScript(String name, String nextFlow) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/nextflow/" + name + ".nf" );
        String test = ApplicationUtils.readAsString( new File( url.getFile() ) );
        //        assertEquals( test, nextFlow );
    }

    private static String runNextFlow(File baseDir, File testDir, File resultDir, String name, String script, String jsonName)
    {
        return runNextFlow( baseDir, testDir, resultDir, name, script, jsonName, new ArrayList<String>() );
    }

    private static String runNextFlow(File baseDir, File testDir, File resultDir, String name, String script, String jsonName, List<String> imports)
    {
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        try
        {
            NextFlowRunner.generateFunctions( resultDir.getCanonicalPath() );

            for( String imported : imports )
            {
                File file = new File( testDir, imported + ".nf" );
                File copy = new File( resultDir, file.getName() );
                ApplicationUtils.copyFile( copy, file );
            }

            File f = new File( resultDir, name + ".nf" );
            ApplicationUtils.writeString( f, script );

            ProcessBuilder builder = null;

            if( jsonName != null )
            {
                File oldJson = new File( resultDir, jsonName );
                File nfJson = new File( resultDir, f.getName() + ".json" );
                ApplicationUtils.writeString( nfJson, NextFlowPreprocessor.processJson( ApplicationUtils.readAsString( oldJson ) ) );
                
                Path basePath = baseDir.toPath();
                Path wdlPath = f.toPath();
                Path jsonPath = nfJson.toPath();
                String wdlRelPath = basePath.relativize( wdlPath ).toString().replace( "\\", "/" );
                String jsonRelPath = basePath.relativize( jsonPath ).toString().replace( "\\", "/" );
                if( isWindows )
                {
                    builder = new ProcessBuilder( "wsl", "--cd", baseDir.getAbsolutePath(), "nextflow", wdlRelPath, "-params-file",
                            jsonRelPath );
                }
                else
                {
                    builder = new ProcessBuilder( "nextflow", f.getName(), "-params-file", nfJson.getName() );
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
            ex.printStackTrace();
            return ex.getMessage();
        }
    }
}