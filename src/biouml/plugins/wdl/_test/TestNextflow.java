package biouml.plugins.wdl._test;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.yaml.snakeyaml.Yaml;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.nextflow.NextFlowImporter;
import biouml.plugins.wdl.nextflow.NextFlowPreprocessor;
import biouml.plugins.wdl.nextflow.NextFlowRunner;
import one.util.streamex.StreamEx;
import biouml.plugins.wdl.FileScriptLoader;
import biouml.plugins.wdl.ScriptLoader;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl._test.TestNextflow.WorkflowTestResults.WorkflowTestResult;
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
    private File nextflowResultsDir = null;
    private WorkflowReportGenerator workflowReportGenerator = new WorkflowReportGenerator();
    private List<TestResult> testResults = new ArrayList<>();
    private File yamlFile = null;
    private int limit = 800; //number of tests to execute
    //stdout_as_output
    private String selected = null;//"basic_directory";//"type_pair_files";//"null_optional_vs_default_subworkflows" - publish dir ext workflow;
    File suiteDir = null;
    public static void main(String ... args) throws Exception
    {
//        File f1 = new File("C:/Users/Damag/eclipse_2024_6/BioUML/out/biouml/plugins/wdl/_test/resources/wdl-conformance-tests/tests/basic_select_first/basic_select_first.json");
//        File f2 = new File("C:/Users/Damag/eclipse_2024_6/BioUML/out/biouml/plugins/wdl/_test/resources/wdl-conformance-tests/workflow_outputs/selectFirstWorkflow.file_output");
//        
//        String s1 = ApplicationUtils.readAsString( f1 );
//        String s2 = ApplicationUtils.readAsString( f2 );
//        
//        String md51 = getMD5Hash( s1 );
//        String md52 = getMD5Hash( s2 );   
        
//        File f = new File("C:/Users/Damag/eclipse_2024_6/BioUML/out/biouml/plugins/wdl/_test/resources/wdl-conformance-tests/workflow_outputs/ceilWorkflow.the_ceiling");
//
//      FileInputStream src = new FileInputStream(f);
//              System.out.println("CHECK SRC");
//              byte[] bytes = src.readAllBytes();
//              
//      System.out.println(bytes.length);
//      for (byte b : bytes) {
//          System.out.printf("%02x ", b);
//      }
//      src.close();        
//        
//      
        testWDL( "resources/wdl-conformance-tests", "conformance.yaml" );
      
        
//        normalizeAllFiles();
        
        //        testWDL1();//
        //        testWDL("resources/test_suite");
    }

    
    private static void normalizeAllFiles() throws Exception
    {
        File dir = new File("C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/_test/resources/wdl-conformance-tests/tests");
        for (File folder: dir.listFiles())
        {
            for (File file: folder.listFiles())
            {
                String content = ApplicationUtils.readAsString( file );
                content = normalizeLinebBreeaks(content);
                ApplicationUtils.writeString( file, content );
            }
        }
    }
    
    public static void testWDL(String path, String yamlFileName) throws Exception
    {
//        String userDirectory = System.getProperty("biouml.sbmltest.path");
                
        TestNextflow tester = new TestNextflow();
        tester.init( TestNextflow.class.getResource( path ), yamlFileName );
        tester.test( tester.yamlFile );

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
        //        tester.test( "two_steps" );
        //        tester.test( "two_steps2" );
        //        tester.test( "two_steps3" );
        //        tester.test( "four_steps" );
        //        tester.test( "scatter_simple" );
        //        tester.test( "call_expr_call2" );
        //        tester.test( "simple_if" );
        //        tester.test( "scatter_range" );
        //        tester.test( "scatter_range2" );
        //        tester.test( "private_declaration" );
        //        tester.test( "cycle_expressions" );
        //        tester.test( "cycle_expressions2" );
        //        tester.test( "cycle_expressions3" );
        //        tester.test( "test_map" );
        //        tester.test( "array_select" );
        //        tester.test( "array_select2" );
        //        tester.test( "array_input" );
        //        tester.test( "array_input2" );
        //        tester.test( "array_input3" );
        //        tester.test( "two_inputs" );
        //        tester.test( "cycle_expression_call" );
        //        tester.test( "cycle_expression_call2" );
        //        tester.test( "nested_access" );
        //        tester.test( "nested_access2" );
        //        tester.test( "test_scatter" );
        //        tester.test( "object_output" );
        //        tester.test( "object_output2" );
        //        tester.test( "two_inputs_cycle" );

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
        File f = new File(url.toURI());
        String path = f.getAbsolutePath();
        path = path.replace( "out", "src");
        suiteDir = new File(path );
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
        nextflowResultsDir = new File( new File( resultsDir, name), "output");
        String originalNextflow = ApplicationUtils.readAsString( new File( testDir, name + ".nf" ) );
        Diagram diagram = new NextFlowImporter().importNextflow( originalNextflow );
        NextFlowGenerator nextFlowGenerator = new NextFlowGenerator();
        nextFlowGenerator.setPublishOutput( true );
        String nextflow = nextFlowGenerator.generate( diagram );
        System.out.println( nextflow );
    }

    //    outputs:
    //        quoteWorkflow.str_output:
    //          type: Array[String]
    //          value: ['"value1"', '"value2"', '"value3"']
    //        quoteWorkflow.int_output:
    //          type: Array[String]
    //          value: ['"1"', '"2"', '"-3"']
    //        quoteWorkflow.float_output:
    //          type: Array[String]
    //          value: ['"1.234000"', '"-0.543000"', '"-13.300000"']
    //        quoteWorkflow.bool_output:
    //          type: Array[String]
    //          value: ['"true"', '"false"', '"true"']

    public static class WorkflowTestResults
    {
        Map<String, TestResult> results = new HashMap<>();

        public static class WorkflowTestResult
        {
            String workflowName;
            String name;
            String type;
            Object value;

            public WorkflowTestResult(String fullName, String type, Object value)
            {
                String[] parts = fullName.split( "\\." );
                workflowName = parts[0];
                workflowName = parts[1];
                this.name = fullName;
                this.type = type;
                this.value = value;
            }
        }
    }

    public void test(File yamlFile) throws Exception
    {
        Yaml parser = new Yaml();
        Object obj = parser.load( ApplicationUtils.readAsString( yamlFile ) );
        List<Object> rootMap = (List<Object>)obj;
        int current = -1;
        for( Object test : rootMap )
        {
            current++;
            if( current > limit )
                continue;

            Map<Object, Object> testMap = (Map<Object, Object>)test;
            String description = testMap.get( "description" ).toString();
            String id = testMap.get( "id" ).toString();
            if (id.equals( "string_placeholders" ))
            {
                System.out.println( "" );
            }
            Object inputs = testMap.get( "inputs" );
            Map<Object, Object> inputsMap = (Map<Object, Object>)inputs;
            String testPath = inputsMap.get( "dir" ).toString();
            String testName = inputsMap.get( "wdl" ).toString();
            String testJSON = inputsMap.get( "json" ).toString();

            Object outputs = testMap.get( "outputs" );
            Map<Object, Object> outputsMap = (Map<Object, Object>)outputs;
            Set<WorkflowTestResult> results = new HashSet<>();
            for( Entry<Object, Object> entry : outputsMap.entrySet() )
            {
                String fullName = entry.getKey().toString();
                Map<Object, Object> entity = (Map<Object, Object>)entry.getValue();
                String type = entity.get( "type" ).toString();
                Object value = entity.get( "value" );
//                Class cl = Class.forName( type );
//                Object val = cl.cast( value );
                results.add(new WorkflowTestResult( fullName, type, value ) );
            }
            //            Path olderPath = yamlFile.getParentFile().toPath();
            Path testAbsolutePath = Path.of( yamlFile.getParentFile().getAbsolutePath(), testPath );
            File testDir = testAbsolutePath.toFile();

            if( selected != null && !selected.equals( id ) )
                continue;
            
//            this.createFiles( testDir, testName );
            test( id, description, testDir, testName, testJSON, results );
        }
    }

    public void testAll() throws Exception
    {
        if( testsDir == null )
            return;
        for( String test : testsDir.list() )
        {
            test( test );
        }
    }

    public void test(String testName)
    {
        test( testName, "TBA", new File( testsDir, testName ), testName + ".wdl", testName + ".json" , new HashSet<>());
    }
    
//    public void createFiles(File testDir, String wdlName) throws Exception
//    {
//        String name = testDir.getName();
//
//        //            File testDir = new File( testDir, wdlName );
//        String originalWDL = ApplicationUtils.readAsString( new File( testDir, wdlName ) );
//        Diagram diagram = null;
//        String nextflow = null;
//        String generatedWDL = null;
//        String validated = null;
//        String roundWDL = null;
//        File resultDir = new File( resultsDir, name );
//        resultDir.mkdirs();
//
//        copyFiles( testDir, resultDir );
//    }

    public void test(String id, String description, File testDir, String wdlName, String jsonName, Set<WorkflowTestResult> expected)
    {
        String name = testDir.getName();

        System.out.println( "TESTING " + id );
        TestResult testResult = new TestResult( id );
        testResult.setDescrption( description );
        try
        {
//                        File testDir = new File( testDir, wdlName );
            String originalWDL = ApplicationUtils.readAsString( new File( testDir, wdlName ) );
            Diagram diagram = null;
            String nextflow = null;
            String generatedWDL = null;
            String validated = null;
            String roundWDL = null;
            File resultDir = new File( resultsDir, id );
            resultDir.mkdirs();
           nextflowResultsDir = new File(resultDir, "output");
        
            copyFiles( testDir, resultDir );

            //1. Generate diagram
            try
            {
                WDLImporter importer = new WDLImporter();
                importer.setScriptLoader( new FileScriptLoader( ScriptLoader.WDL_TYPE, testDir ) );
                ScriptInfo info = importer.readScript( name, originalWDL );               
                diagram = new DiagramGenerator().generateDiagram( info, new WDLDiagramType().createDiagram( null, id ), null );
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
                    WDLImporter importer = new WDLImporter();
                    importer.setScriptLoader( new FileScriptLoader( ScriptLoader.WDL_TYPE, resultDir ) );
                    Diagram roundDiagram =  importer.generateDiagram( generatedWDL, name, null );
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
                    
                    String relPath = suiteDir.toPath().relativize( resultDir.toPath() ).toString().replace( "\\", "/" )+"/output";
                    nextFlowGenerator.setPublishDir( relPath);
                    nextFlowGenerator.setPublishOutput( true );
                    nextflow = nextFlowGenerator.generate( diagram );
                    if( nextflow != null )
                        testResult.setNextflowGenerated( TestUtil.TEST_OK );

                }
                catch( Exception ex )
                {
                    testResult.setNextflowGenerated( ex.toString() );
                }

                //5. Execute nextflow
                if(TestUtil.TEST_OK .equals( testResult.getNextflowGenerated()) && executeNextflow )
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
                saveResults( id, wdlName, resultDir, description, roundWDL, generatedWDL, nextflow, diagram );

                List<String> results = new ArrayList<>();
                for( WorkflowTestResult result : expected )
                {
                    String error = checkResult( result, new File(new File(resultDir, "output"), "outputs.json" ));
                    if( !error.equals( "" ) )
                        results.add( error );
                }

                if( results.isEmpty() )
                    testResult.setNextflowChecked( "Ok" );
                else
                {
                    testResult.setNextflowChecked( StreamEx.of(results).joining("\n") );
                    System.out.println( StreamEx.of(results).joining("\n") );
                }

                //6. Validate WDL (optional)
                if( !validateWDL )
                    validated = "N/A";
                else if( generatedWDL != null )
                    validated = TestUtil.validateWDL( new File( new File( resultsDir, id ), id + "_exported.wdl" ).getAbsolutePath(),
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
            
            content = normalizeLinebBreeaks(content);
            ApplicationUtils.writeString( copy, content );
        }
    }

    
    private static String normalizeLinebBreeaks(String text)
    {
        return text.replace("\r\n", "\n");
    }

    private void saveResults(String name, String wdlPath, File resultDir, String description, String roundWDL, String generatedWDL,
            String nextflow, Diagram diagram) throws Exception
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

    private static String runNextFlow(File baseDir, File testDir, File resultDir, String name, String script, String jsonName,
            List<String> imports)
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
    
    public String checkFile(File file, Object resultValue) throws Exception
    {
        String content = ApplicationUtils.readAsString( file ); 
        Map<String, Object> value = (Map<String, Object>)resultValue;
        if( value.containsKey( "md5sum" ) )
        {
            String md5 = getMD5Hash( content );
            String expectedMd5 = value.get( "md5sum" ).toString();
            if( md5.equals( expectedMd5 ) )
                return "";
            return file.getName() + " MD5: " + md5 + "\nExpected: " + expectedMd5;
        }
        else if (value.containsKey( "regex" ))
        {
            if (content.endsWith("\n"))
                content = content.substring(0, content.length() - 1);
            
            String regex = value.get( "regex" ).toString();
            regex = pythonRegexToJavaRegex( regex );
            if( Pattern.compile( regex ).matcher( content ).find() )
//            if( Pattern.compile( regex ).matcher( content ).matches() )
                return "";
            return "Does not match: "+content+"\n"+regex;
        }
        else
        {
           return "!!!!!MISSED REGEX OR MSUM!!!!!!";
        }
    }

    public String checkResult(WorkflowTestResult result, File outputs) throws Exception
    {
        String outputJson = ApplicationUtils.readAsString( outputs );
        Object jsonValue = new JSONTokener(outputJson).nextValue();
        if( result.type.equals( "File" ) )
        {
            JSONObject jsonObj = (JSONObject)jsonValue;
            File generated = new File( nextflowResultsDir, jsonObj.get( result.name ).toString() );
            return checkFile( generated, result.value );
        }
        else if( result.type.equals( "Array[File]" ) )
        {
            JSONObject jsonObj = (JSONObject)jsonValue;
            JSONArray array = (JSONArray)jsonObj.get( result.name );
            ArrayList<String> checkArray = (ArrayList<String>)result.value;
            for( int i = 0; i < array.length(); i++ )
            {
                String path = array.get( i ).toString();
                File generated = new File( nextflowResultsDir, path );
                Object checker = checkArray.get( i );
                String err = checkFile( generated, checker );
                if( !err.isEmpty() )
                    return err;
            }
            return "";
        }
        else
        {
            JSONObject jsonObj = (JSONObject)jsonValue;
            Object generatedValue = jsonObj.get( result.name );

            if( generatedValue instanceof JSONArray )
            {
                if( ( (JSONArray)generatedValue ).toList().equals( result.value ) )
                    return "";
                else
               {
                   return  "Got "+((JSONArray)generatedValue ).toList()+"\nExpected: " + result.value.toString();
               }
            }
            else if (generatedValue instanceof Boolean || generatedValue instanceof String || generatedValue instanceof Double || generatedValue instanceof Integer)
            {
                if ( generatedValue.equals( result.value ))
                    return "";
                else
                {
                    return "Got "+generatedValue.toString()+"\nExpected: " + result.value.toString();
                }
            }
            else
            {
                if (generatedValue instanceof JSONObject)
                {
                    return equals(((JSONObject)generatedValue).toMap(), result.value);  
                }
                return "UNKNOWN class "+generatedValue.getClass();
            }

        }
    }
    
    private String equals(Object generated, Object expected)
    {
        if (generated instanceof Map)
        {
            if (expected instanceof Map)
            {
                boolean ersult = true;
                for (Entry e: ((Map<Object, Object>)generated).entrySet())
                {
                    Object key = e.getKey();
                    Object value = e.getValue();
                    
                    Object expectedValue = ((Map<Object, Object>)expected).get(key);
                    if ( expectedValue == null)
                        return "Missing value for key: "+key;
                    return equals(expectedValue, value ); 
                }
                return "";
            }
            return "Wrong value:" + generated.toString();
        }
        else
        {
            if ( !(generated.equals( expected )))
               return  "Got "+generated.toString()+"\nExpected: " + expected.toString();
            return "";
        }
    }
    
    public static String pythonRegexToJavaRegex(String regex) {
        StringBuilder out = new StringBuilder();

        boolean inCharClass = false;
        boolean escaped = false;

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);

            if (escaped) {
                out.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                out.append(c);
                escaped = true;
                continue;
            }

            if (c == '[') {
                inCharClass = true;
                out.append(c);
                continue;
            }

            if (c == ']' && inCharClass) {
                inCharClass = false;
                out.append(c);
                continue;
            }

            if (!inCharClass && c == '{') {
                if (isValidJavaQuantifier(regex, i)) {
                    out.append(c); // keep {6}, {1,3}, {1,}
                } else {
                    out.append("\\{"); // literal {
                }
                continue;
            }

            out.append(c);
        }

        return out.toString();
    }

    private static boolean isValidJavaQuantifier(String s, int openBrace) {
        int i = openBrace + 1;

        // must start with digit
        if (i >= s.length() || !Character.isDigit(s.charAt(i)))
            return false;

        while (i < s.length() && Character.isDigit(s.charAt(i)))
            i++;

        if (i < s.length() && s.charAt(i) == ',') {
            i++;
            while (i < s.length() && Character.isDigit(s.charAt(i)))
                i++;
        }

        return i < s.length() && s.charAt(i) == '}';
    }

    public static String getMD5Hash(String input)
    {
        try
        {
            // Get MD5 MessageDigest instance
            MessageDigest md = MessageDigest.getInstance( "MD5" );

            // Compute the hash bytes
            byte[] hashBytes = md.digest( input.getBytes( StandardCharsets.UTF_8 ) );

            // Convert byte array into a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for( byte b : hashBytes )
            {
                String hex = Integer.toHexString( 0xff & b );
                if( hex.length() == 1 )
                {
                    hexString.append( '0' ); // Pad with leading zero
                }
                hexString.append( hex );
            }
            return hexString.toString();

        }
        catch( NoSuchAlgorithmException e )
        {
            throw new RuntimeException( "MD5 algorithm not found", e );
        }
    }
}