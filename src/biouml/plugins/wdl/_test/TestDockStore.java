package biouml.plugins.wdl._test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.CompilePhase;
import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.FileScriptLoader;
import biouml.plugins.wdl.ScriptLoader;
import biouml.plugins.wdl.diagram.DiagramGenerator;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.nextflow.ast.NextflowParser;
import one.util.streamex.StreamEx;

public class TestDockStore
{
    private static boolean doValidate = false;
    private static boolean skipInvalid = true;
    private static int success = 0;
    private static int total = 0;
    private static int limit = 6000;
    private static int incomplete = 0;
    private static int failed = 0;

    private static Set<String> invalidNextflows = new HashSet<>();
    private static Set<String> invalidDirs = new HashSet<>();
    private static String WOM_TOOL_PATH = "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/wdl/womtool-92.jar";
    private static String MINI_WDL_PATH = "wsl /home/damag/.local/bin/miniwdl check ";

    private static String name = null;//"GLNexus_main_9d7b30";//"RareVariantEnrichment_main_91c27d";//"colocboost_main_187086";//"DepthOfCoverageWorkflow_main_b19052";//"BamQC_2.0_0e26e1";//"Pangolin_Update_PHB_main_693d35";
    static File rootDir = new File( "C:/Users/Damag/wdl/dockstore/new3" );

    private static File invalidNextflowsFile = new File( rootDir, "invalid_nextflow.txt" );
    private static File invalidFile = new File( rootDir, "invalid.txt" );
    private static File failedFile = new File( rootDir, "failed.txt" );
    private static File inputDirs = null;//new File( rootDir, "invalid_inputs.txt" );
    private static File errorsFile = new File( rootDir, "error.txt" );
    private static Map<String, FailedTests> errors = new HashMap<>();


    public static void main(String[] args) throws IOException
    {
        success = 0;
        total = 0;

        errors = new HashMap<>();
        errorsFile.delete();
        errorsFile.createNewFile();
        invalidTests = new HashSet<>();
        failedFile.createNewFile();
        invalidNextflowsFile.createNewFile();
        if( doValidate )
            invalidFile.createNewFile();
        else
            invalidDirs = StreamEx.of( ApplicationUtils.readAsList( invalidFile ) ).toImmutableSet();

        File root = new File( rootDir, "downloads" );
        List<String> dirs = List.of( root.list() );
        if( inputDirs != null )
            dirs = ApplicationUtils.readAsList( inputDirs );
        for( String dirName : dirs )
        {
            if (name != null && !dirName.equals( name ))
                continue;
            if( skipInvalid && invalidDirs.contains( dirName ) )
            {
                System.out.println( "SKIPPED AS INVALID " + dirName );
                continue;
            }
            File dir = new File( root, dirName );
            processDirectory( dir );

            if( total >= limit )
                break;
        }
        File report = new File( rootDir, "report.txt" );
        report.createNewFile();
        String content = "Success: " + success + " | Incomplete: " + incomplete + " | Failed: " + failed + " | Invlid result: " + invalidNextflows.size()+ " | Total: " + total ;
        ApplicationUtils.writeString( report, content );
        System.out.println( content );

        List<FailedTests> failed = new ArrayList<>();
        for( Entry<String, FailedTests> e : errors.entrySet() )
            failed.add( e.getValue() );
        failed.sort( new Comparator<FailedTests>()
        {
            @Override
            public int compare(FailedTests o1, FailedTests o2)
            {
                return o1.getNumber() > o2.getNumber() ? -1 : o1.getNumber() < o2.getNumber() ? -1 : 0;
            }
        } );

        try
        {
            int totalErrors = StreamEx.of( failed ).mapToInt( e -> e.getNumber() ).sum();
            System.out.println( "TOTAL ERROR " + totalErrors );
            BufferedWriter bw = ApplicationUtils.utfWriter( errorsFile );
            for( FailedTests tests : failed )
            {
                bw.append( "\nMESSAGE " + tests.message );
                bw.append( "\nNUMBER " + tests.getNumber() );
                bw.append( "\nTESTS " + StreamEx.of( tests.tests ).joining( "\t" ) );
                bw.append( "\n" );
                bw.append( "\n" );
                bw.append( "\n" );
            }
            bw.close();

            bw = ApplicationUtils.utfWriter( failedFile );
            for( FailedTests tests : failed )
            {
                for( String test : tests.tests )
                    bw.write( test + "\n" );
            }
            bw.close();

            if( doValidate )
            {
                bw = ApplicationUtils.utfWriter( invalidFile );
                for( String test : invalidTests )
                {
                    bw.write( test + "\n" );
                }
                bw.close();
            }

//            File folder = new File( rootDir, "invalid_nextflows" );
//            folder.mkdir();
            bw = ApplicationUtils.utfWriter( invalidNextflowsFile );
            for( String test : invalidNextflows )
            {
//                File nf = new File( test.contains( "\t" ) ? test.split( "\t" )[0] : test );
//                ApplicationUtils.copyFile( new File( folder, nf.getName() ), nf );
                bw.write( test + "\n" );
            }
            bw.close();
        }
        catch( Exception ex )
        {

        }
    }

    private static void processDirectory(File directory)
    {
        Path dirPath = directory.toPath();
        Path metadataFile = dirPath.resolve( "metadata.json" );

        if( !Files.isRegularFile( metadataFile ) )
            return;

        try
        {
            JSONObject metadata = new JSONObject( Files.readString( metadataFile, StandardCharsets.UTF_8 ) );
            String primaryWdl = metadata.getString( "primary_wdl" );
            Path wdlFile = dirPath.resolve( primaryWdl ).toAbsolutePath().normalize();

            if( !wdlFile.startsWith( dirPath ) )
                throw new IOException( "Invalid WDL path: " + primaryWdl );

            if( !Files.isRegularFile( wdlFile ) )
                throw new IOException( "WDL file not found: " + wdlFile );

            System.out.println( total + " Processing: " + directory );
            processWdl( wdlFile, directory.getName() );
        }
        catch( Exception e )
        {
            String shortMessage = e.getMessage();
            if( shortMessage != null && shortMessage.contains( "at line" ) )
                shortMessage = shortMessage.substring( 0, shortMessage.indexOf( "at line" ) );
            else if( shortMessage != null && shortMessage.contains( "at index" ) )
                shortMessage = shortMessage.substring( 0, shortMessage.indexOf( "at index" ) );

            System.out.println( "Failed to process " + directory + ": " + e.getMessage() );
            errors.computeIfAbsent( shortMessage, k -> new FailedTests( k ) ).addTest( directory.getName() );
            failed++;
        }
        catch( Error er )
        {
            String shortMessage = er.getMessage();
            if( shortMessage != null && shortMessage.contains( "at line" ) )
                shortMessage = shortMessage.substring( 0, shortMessage.indexOf( "at line" ) );
            System.out.println( "Failed to process " + directory + ": " + er.getMessage() );
            errors.computeIfAbsent( shortMessage, k -> new FailedTests( k ) ).addTest( directory.getName() );
            failed++;
        }
    }

    private static class FailedTests
    {
        String message;
        List<String> tests;

        public FailedTests(String message)
        {
            this.message = message;
            tests = new ArrayList<String>();
        }

        public void addTest(String test)
        {
            tests.add( test );
        }

        public int getNumber()
        {
            return tests.size();
        }
    }

    private static Set<String> invalidTests = new HashSet<>();

    private static void processWdl(Path wdlFile, String dir) throws Exception
    {
        
//        if (!checkVersion(wdlFile.toFile()))
//            return;
        
        if( doValidate )
        {
            //            String validated = TestUtil.validateWDL( wdlFile.toFile().getAbsolutePath(), WOM_TOOL_PATH );
            String validated = TestUtil
                    .execute( MINI_WDL_PATH + wdlFile.toFile().getAbsolutePath().replace( "\\", "/" ).replace( "C:/", "/mnt/c/" ) );
            if( !validated.equals( "Ok" ) && !validated.equals( "" ) )
            {
                System.out.println( "NOT VALIDATED: " + validated );
                invalidTests.add( dir );
                return;
            }
        }
        //        
        total++;
        String name = wdlFile.toFile().getName();
        name = name.substring( 0, name.lastIndexOf( "." ) );
        String wdlContent = Files.readString( wdlFile, StandardCharsets.UTF_8 );

        WDLImporter importer = new WDLImporter();
        importer.setScriptLoader( new FileScriptLoader( ScriptLoader.WDL_TYPE, wdlFile.getParent().toFile() ) );
        ScriptInfo info = importer.readScript( name, wdlContent );
        DiagramGenerator generator = new DiagramGenerator();
        generator.setDoLayout( false );
        Diagram diagram = generator.generateDiagram( info, null, name );

        NextFlowGenerator nextFlowGenerator = new NextFlowGenerator();
        nextFlowGenerator.setAllDiagrams( generator.getAllImports() );
        String nextflow = nextFlowGenerator.generate( diagram );
        File converted = new File( wdlFile.getParent().toFile(), name + ".nf" );
        converted.createNewFile();

        if( nextflow.contains( "$rhs" ) )
        {
            invalidNextflows.add( dir );
        }
        try
        {
            List<ASTNode> nodes = NextflowParser.parse( nextflow, false, CompilePhase.SEMANTIC_ANALYSIS );
        }
        catch( Exception ex )
        {
            invalidNextflows.add( dir );
            ex.printStackTrace();
        }

        ApplicationUtils.writeString( converted, nextflow );
        success++;
    }

    private static boolean checkVersion(File file) throws IOException
    {
        List<String> content = ApplicationUtils.readAsList( file );
        for( String s : content )
        {
            if( s.equals( "version 1.0" ) || s.equals( "version 1.1" ) || s.equals( "version 1.2" ))
                return true;
        }
        return false;
    }
}