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
import biouml.plugins.wdl.nextflow.NextflowSettings;
import one.util.streamex.StreamEx;
import biouml.plugins.wdl.FileScriptLoader;
import biouml.plugins.wdl.ScriptLoader;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl._test.TestNextflow.WorkflowTestResults.WorkflowTestResult;
import biouml.plugins.wdl.diagram.DiagramGenerator;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.model.ScriptInfo;

public class TestNextflow
{
    private static boolean validateWDL = true;
    private static String WOM_TOOL_PATH = "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/wdl/womtool-92.jar";
    private static NextflowSettings settings = new NextflowSettings();
    {
        settings.setPublishOutput( NextflowSettings.COPY );
        settings.setStageInput( NextflowSettings.COPY );
    }
    public boolean executeNextflow = true;
    private File testsDir;
    private File resultsDir;
    private WorkflowReportGenerator workflowReportGenerator = new WorkflowReportGenerator();
    private List<TestResult> testResults = new ArrayList<>();
    private File yamlFile = null;
    private int limit = 666; //number of tests to execute
    //stdout_as_output
    private Set<String> selected = null;//Set.of("two_calls_if_cycle")        ;//"null_optional_vs_default_subworkflows" - publish dir ext workflow;
    private Set<String> excluded = null;//
            File suiteDir = null;
    
            public static void main(String ... args) throws Exception
            {
                testWDL( "resources/test_suite", "test.yaml" );
                testWDL( "resources/wdl-conformance-tests", "conformance.yaml" );
//                convert("C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/_test/resources/candidades/double_scatter2.wdl");
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
        TestNextflow tester = new TestNextflow();
        tester.init( TestNextflow.class.getResource( path ), yamlFileName );
        tester.test( tester.yamlFile );
        tester.generateStatistics( tester.testResults );
    }

    public static void testWDL(String path) throws Exception
    {
        TestNextflow tester = new TestNextflow();
        tester.init( TestNextflow.class.getResource( path ), null );
//        tester.test( "double_scatter" );
//        tester.test( "double_condition");
//        tester.test( "conditional_call");
//        tester.test( "multiple_outputs");
//        tester.test( "outer_call");
//        tester.test( "outer_expression");
        tester.testAll();
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
//        nextflowResultsDir = new File( new File( resultsDir, name), "output");
        String originalNextflow = ApplicationUtils.readAsString( new File( testDir, name + ".nf" ) );
        Diagram diagram = new NextFlowImporter().importNextflow( originalNextflow );
        NextFlowGenerator nextFlowGenerator = new NextFlowGenerator();
        String nextflow = nextFlowGenerator.generate( diagram );
        System.out.println( nextflow );
    }

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
            Set<String> tags = StreamEx.of(((ArrayList)testMap.get( "tags" ))).map( s->s.toString() ).toSet();
            String id = testMap.get( "id" ).toString();
            Object inputs = testMap.get( "inputs" );
            Map<Object, Object> inputsMap = (Map<Object, Object>)inputs;
            String testPath = inputsMap.get( "dir" ).toString();
            String testName = inputsMap.get( "wdl" ).toString();
            
            String testJSON = (inputsMap.containsKey( "json" ))? inputsMap.get( "json" ).toString(): null;

            Object outputs = testMap.get( "outputs" );
            Map<Object, Object> outputsMap = (Map<Object, Object>)outputs;
            Set<WorkflowTestResult> results = new HashSet<>();
            for( Entry<Object, Object> entry : outputsMap.entrySet() )
            {
                String fullName = entry.getKey().toString();
                Map<Object, Object> entity = (Map<Object, Object>)entry.getValue();
                String type = entity.get( "type" ).toString();
                Object value = entity.get( "value" );
                results.add(new WorkflowTestResult( fullName, type, value ) );
            }
            Path testAbsolutePath = Path.of( yamlFile.getParentFile().getAbsolutePath(), testPath );
            File testDir = testAbsolutePath.toFile();

            if( ( selected != null && !selected.contains( id ) ) || ( excluded != null && excluded.contains( id ) ) )
                continue;
            
//            this.createFiles( testDir, testName );
            test( id, description, tags, testDir, testName, testJSON, results );
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
        test( testName, "TBA", Set.of(), new File( testsDir, testName ), testName + ".wdl", testName + ".json" , new HashSet<>());
    }

    public void test(String id, String description, Set<String> tags, File testDir, String wdlName, String jsonName, Set<WorkflowTestResult> expected)
    {
        String name = testDir.getName();

        System.out.println( "TESTING " + id );
        TestResult testResult = new TestResult( id );
        testResult.setDescrption( description );
        testResult.setTags( StreamEx.of(tags).joining(", ") );
        try
        {
            String originalWDL = ApplicationUtils.readAsString( new File( testDir, wdlName ) );
            Map<String, Diagram> allDiagrams = null;
            Map<String, String> additionalNextflow = new HashMap<>();
            Diagram diagram = null;
            String nextflow = null;
            String generatedWDL = null;
            String validated = null;
            String roundWDL = null;
            File resultDir = new File( resultsDir, id );
            resultDir.mkdirs();
        
            copyFiles( testDir, resultDir );

            //1. Generate diagram
            try
            {
                WDLImporter importer = new WDLImporter();
                importer.setScriptLoader( new FileScriptLoader( ScriptLoader.WDL_TYPE, testDir ) );
                ScriptInfo info = importer.readScript( name, originalWDL );    
                DiagramGenerator generator = new DiagramGenerator();
                diagram = generator.generateDiagram( info, null, id );
                allDiagrams =  generator.getAllImports();
                //                diagram = TestUtil.generateDiagram( name, originalWDL );
            }
            catch( Exception ex )
            {
                testResult.setDiagramGenerated( ex.toString() );
            }

            if( diagram != null )
            {
                testResult.setDiagramGenerated( TestUtil.TEST_OK );
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
                    nextFlowGenerator.setNextflowSettings( settings );
                    nextFlowGenerator.setResultPath( "results/" + diagram.getName() + "/output/" );
                    
                    nextflow = nextFlowGenerator.generate( diagram );
                    if( nextflow != null )
                        testResult.setNextflowGenerated( TestUtil.TEST_OK );
                    
                    for (Entry<String,Diagram> imported: allDiagrams.entrySet())
                    {
                        if( imported.getValue().equals( diagram ) )
                            continue;
                        additionalNextflow.put( imported.getKey(), nextFlowGenerator.generate( imported.getValue()));
                    }

                }
                catch( Exception ex )
                {
                    testResult.setNextflowGenerated( ex.toString() );
                    ex.printStackTrace();
                }

                //5. Execute nextflow
                if(TestUtil.TEST_OK .equals( testResult.getNextflowGenerated()) && executeNextflow )
                {
                    try
                    {
                        String json = ( jsonName != null && new File( testDir, jsonName ).exists() ) ? jsonName : null;
                        String nextFlowExecuted = runNextFlow( suiteDir, testDir, resultDir, name, nextflow, additionalNextflow, json );
                        testResult.setNextflowExecuted( nextFlowExecuted );
                    }
                    catch( Exception ex )
                    {
                        testResult.setNextflowExecuted( ex.toString() );
                        ex.printStackTrace();
                    }
                }
                if( tags.contains( "fail" ) )
                {
                    if( testResult.getNextflowExecuted().equals( TestUtil.TEST_OK ) )
                        testResult.setNextflowExecuted( "Test should fail but was passed" );
                    else 
                        testResult.setNextflowExecuted(  TestUtil.TEST_FAILED_OK );
                }
                    
                saveResults( id, wdlName, resultDir, description, tags, roundWDL, generatedWDL, nextflow, diagram ); 

                //6. Check generated result
                if( TestUtil.TEST_OK.equals( testResult.getNextflowExecuted() ) || TestUtil.TEST_FAILED_OK.equals( testResult.getNextflowExecuted() ))
                {
                    List<String> errors = new ArrayList<>();
                    for( WorkflowTestResult result : expected )
                    {
                        String error = checkResult( result, new File(new File(resultDir, "output"), "outputs.json" ));
                        if( !error.isEmpty() )
                            errors.add( error );
                    }
                    if( errors.isEmpty() )
                        testResult.setNextflowChecked( TestUtil.TEST_OK );
                    else
                    {
                        testResult.setNextflowChecked( StreamEx.of( errors ).joining( "\n" ) );
                        System.out.println( StreamEx.of( errors ).joining( "\n" ) );
                    }
                }   
                
                //7. Validate WDL (optional)
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
    
    private static void convert(String path)
    {
        try
        {
            File wdlFile = new File(path);
            String id = wdlFile.getName().substring( 0, wdlFile.getName().lastIndexOf( "." ) );
            String originalWDL = ApplicationUtils.readAsString( wdlFile );
            WDLImporter importer = new WDLImporter();
            importer.setScriptLoader( new FileScriptLoader( ScriptLoader.WDL_TYPE, wdlFile.getParentFile() ) );
            ScriptInfo info = importer.readScript( id, originalWDL );
            DiagramGenerator generator = new DiagramGenerator();
            Diagram diagram = generator.generateDiagram( info, null, id );
            NextFlowGenerator nextFlowGenerator = new NextFlowGenerator();
            nextFlowGenerator.setNextflowSettings( settings );
            nextFlowGenerator.setResultPath( "results/" + diagram.getName() + "/output/" );
            String nextflow = nextFlowGenerator.generate( diagram );
            File result = new File( wdlFile.getParentFile(), id + ".nf" );
            ApplicationUtils.writeString( result, nextflow );
            System.out.println( nextflow );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
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

    private void saveResults(String name, String wdlPath, File resultDir, String description, Set<String> tags, String roundWDL, String generatedWDL,
            String nextflow, Diagram diagram) throws Exception
    {
        if( diagram != null )
            TestUtil.exportImage( new File( resultDir, name + ".png" ), diagram );
        if( nextflow != null )
            ApplicationUtils.writeString( new File( resultDir, name + ".nf" ), nextflow );
        if( description != null )
            ApplicationUtils.writeString( new File( resultDir, name + "_description.txt" ), description );
        if( tags != null )
            ApplicationUtils.writeString( new File( resultDir, name + "_tags.txt" ), StreamEx.of(tags).joining(",") );
        if( generatedWDL != null )
            ApplicationUtils.writeString( new File( resultDir, name + "_exported.wdl" ), generatedWDL );
        if( roundWDL != null )
            ApplicationUtils.writeString( new File( resultDir, name + "_round.wdl" ), roundWDL );
        ApplicationUtils.writeString( new File( resultDir, name + ".html" ), workflowReportGenerator.generate( name, wdlPath, resultDir ) );
    }

    private static String runNextFlow(File baseDir, File testDir, File resultDir, String name, String script, Map<String,String> scripts, String jsonName)
    {
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        try
        {
            NextFlowRunner.generateFunctions( resultDir.getCanonicalPath() );  
            File configFile = NextFlowRunner.generateConfig( "nextflow", resultDir , settings);
            File f = new File( resultDir, name + ".nf" );
            ApplicationUtils.writeString( f, script );
            
            for (Entry<String, String> additionalScript: scripts.entrySet())
            {       
                File additional = new File( resultDir, additionalScript.getKey() + ".nf" );
                ApplicationUtils.writeString( additional, additionalScript.getValue() );
            }
            Path basePath = baseDir.toPath();
            String configPath = basePath.relativize(configFile.toPath() ).toString().replace( "\\", "/" );
            String wdlRelPath = basePath.relativize( f.toPath() ).toString().replace( "\\", "/" );
           
            ProcessBuilder builder = null;
            if( jsonName != null )
            {
                File oldJson = new File( resultDir, jsonName );
                File nfJson = new File( resultDir, f.getName() + ".json" );
                ApplicationUtils.writeString( nfJson, NextFlowPreprocessor.processJson( ApplicationUtils.readAsString( oldJson ) ) );
               
                String jsonRelPath = basePath.relativize( nfJson.toPath() ).toString().replace( "\\", "/" );
              
                if( isWindows )
                {//, "NXF_SYNTAX_PARSER=v1"
                    builder = new ProcessBuilder( "wsl", "--cd", baseDir.getAbsolutePath(),"nextflow", wdlRelPath, "-params-file",
                            jsonRelPath, "-c", configPath );
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
                    builder = new ProcessBuilder( "wsl", "--cd", baseDir.getAbsolutePath(), "nextflow", wdlRelPath, "-c", configPath);
                }
                else
                {
                    builder = new ProcessBuilder( "nextflow", f.getName() );
                    builder.directory( resultDir );
                }
            }
            System.out.println( "COMMAND "+ StreamEx.of(builder.command()).joining(" ") );
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
        if (resultValue == null)
            return file == null? "": "Expected null but was "+ file.getName();
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
        if( result.type.equals( "File" ) || result.type.equals( "File?" ))
        {
            JSONObject jsonObj = (JSONObject)jsonValue;
            JSONObject element = (JSONObject)jsonObj.get( result.name );
            Object generatedValue = element.get("value");
            File generated = generatedValue.equals( "null" )?  null: new File( generatedValue.toString() );
            return checkFile( generated, result.value );
        }
        else if( result.type.equals( "Array[File]" ) ||  result.type.equals( "Array[File?]" ))
        {
            JSONObject jsonObj = (JSONObject)jsonValue;
            JSONObject element = (JSONObject)jsonObj.get( result.name );
            Object generatedValue = element.get("value");
            if (!(generatedValue instanceof JSONArray))
            {
                return "Incorrect result type "+generatedValue.getClass()+". Arrray expected";
            }
            JSONArray array = (JSONArray)generatedValue;
            ArrayList<String> checkArray = (ArrayList<String>)result.value;
            for( int i = 0; i < array.length(); i++ )
            {
                String path = array.get( i ).toString();
                File generated = path.equals( "null" )? null:new File( path );
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
            JSONObject element = (JSONObject)jsonObj.get( result.name );
            Object generatedValue = element.get("value");
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