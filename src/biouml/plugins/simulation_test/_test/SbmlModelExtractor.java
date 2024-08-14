package biouml.plugins.simulation_test._test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Utility class for sbml test result extraction
 *1. into list of models with given sbml level to create data collection in BioUML
 *2. into list of csv files for uploading to SBML site 
 * @author Ilya
 *
 */
public class SbmlModelExtractor extends TestCase
{
    /** Standart JUnit constructor */
    public SbmlModelExtractor(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( SbmlModelExtractor.class.getName() );
        suite.addTest( new SbmlModelExtractor( "extract" ) );
        return suite;
    }

    public static final String OUTPUT_FOLDER = "C://SBML tests 3.0/";
    public static final String INPUT_FOLDER = "C:/projects//java//BioUML//data_resources//SBML tests//cases//semantic";

//    public static void extract() throws Exception
//    {
//        extractModels( "l1v2", INPUT_FOLDER, OUTPUT_FOLDER + "l1v2" );
//        extractModels( "l2v1", INPUT_FOLDER, OUTPUT_FOLDER + "l1v2" );
//        extractModels( "l2v1", INPUT_FOLDER, OUTPUT_FOLDER + "l2v1" );
//        extractModels( "l2v2", INPUT_FOLDER, OUTPUT_FOLDER + "l2v2" );
//        extractModels( "l2v3", INPUT_FOLDER, OUTPUT_FOLDER + "l2v3" );
//        extractModels( "l2v4", INPUT_FOLDER, OUTPUT_FOLDER + "l2v4" );
//        extractModels( "l3v1", INPUT_FOLDER, OUTPUT_FOLDER + "l3v1" );
//    }

    public static void extract() throws Exception
    {
        //        extractModels( "l1v2", INPUT_FOLDER, OUTPUT_FOLDER + "l1v2" );
        //        extractModels( "l2v1", INPUT_FOLDER, OUTPUT_FOLDER + "l1v2" );
        //        extractModels( "l2v1", INPUT_FOLDER, OUTPUT_FOLDER + "l2v1" );
        //        extractModels( "l2v2", INPUT_FOLDER, OUTPUT_FOLDER + "l2v2" );
        //        extractModels( "l2v3", INPUT_FOLDER, OUTPUT_FOLDER + "l2v3" );
        //        extractModels( "l2v4", INPUT_FOLDER, OUTPUT_FOLDER + "l2v4" );
        //        extractModels( "l3v1", INPUT_FOLDER, OUTPUT_FOLDER + "l3v1" );

//        extractResults( "C:/BDF_newton_dense_l2v4/csvResults", "C:/BioUML_results/l2v4" );
//        extractResults( "C:/BDF_newton_dense_l3v1/csvResults", "C:/BioUML_results/l3v1" );
        
        File fileDir = new File( "C:/details/" );
        String toReplace = "_plain.png\">";
        String replacement = toReplace + "<br>" + "Diagrams are generated in BioUML using Systems Biology Graphic Notation extended by <a href=\"../../../modular notation.png\">modular diagram graphic notation <a/>.";
        File[] files = fileDir.listFiles();
        if( files != null )
        {
            for( File f : files )
                replace(toReplace, replacement, f);
        }
    }

    public static void extractResults(String csvResultDir, String outputPath)
    {
        File inputFile = new File( csvResultDir );
        if( !inputFile.exists() || !inputFile.isDirectory() )
        {
            System.out.println( "input directory " + csvResultDir + " not found" );
            return;
        }
        File outputDir = new File( outputPath );
        if( !outputDir.exists() && !outputDir.mkdirs() )
        {
            System.out.println("Cannot create directory: '" + outputPath + "'");
            return;
        }

        File[] testDirs = inputFile.listFiles();
        if( testDirs == null )
            return;
        for( File testDir : testDirs )
        {
            if( testDir.isDirectory() )
            {
                for( File testResult : testDir.listFiles() )
                {
                    try
                    {
                        if( testResult.getName().contains( "csv" ) )
                        {
                            String name = testResult.getName();
                            name = name.substring(0, name.indexOf( "." ) );
                            name = "BioUML"+name+".csv";
                            File extracted = new File( outputDir,name );
                            ApplicationUtils.copyFile(extracted, testResult);
                        }
                    }
                    catch( Exception ex )
                    {
                        System.out.println( "Error during " + testResult + " extracting: " + ex.getMessage() );
                    }
                }
            }
        }
    }

    public static void extractModels(String level, String modeListDir, String outputPath)
    {
        File inputFile = new File( modeListDir );
        if( !inputFile.exists() || !inputFile.isDirectory() )
        {
            System.out.println( "input directory " + modeListDir + " not found" );
            return;
        }
        File outputDir = new File( outputPath );
        if( !outputDir.exists() && !outputDir.mkdirs() )
        {
            System.out.println("Cannot create directory: '" + outputPath + "'");
            return;
        }

        File[] modelDirs = inputFile.listFiles();
        if( modelDirs == null )
            return;
        for( File modelDir : modelDirs )
        {
            if( modelDir.isDirectory() )
            {
                for( File modelFile : modelDir.listFiles() )
                {
                    try
                    {
                        String fileName = modelFile.getName();
                        if( fileName.contains( level ) && fileName.contains( ".xml" ) && ! ( fileName.contains( "sedml" ) ) )
                        {
                            File extracted = new File( outputDir, modelFile.getName() );
                            ApplicationUtils.copyFile( extracted, modelFile );
                        }
                    }
                    catch( Exception ex )
                    {
                        System.out.println( "Error during " + modelFile + " extracting: " + ex.getMessage() );
                    }
                }
            }
        }

    }

    public static void replace(String modeListDir) throws Exception
    {
        File fileDir = new File( modeListDir );
        String toReplace = "_plain.png";
        String replacement = toReplace + "<br>" + "Diagrams are generated using BioUML with SBGN extended by <a href=\"../../../modular notation.png\">modular diagram graphic notation </a>";
        File[] files = fileDir.listFiles();
        if( files != null )
        {
            for( File f : files )
                replace(toReplace, replacement, f);
        }
    }

    public static void replace(String from, String to, File d) throws Exception
    {
        File copy = new File( d.getParent(), d.getName() + "_replaced" );
        try (BufferedReader br = ApplicationUtils.utfReader( d ); BufferedWriter bw = ApplicationUtils.utfWriter( copy ))
        {
            String line = br.readLine();
            while( line != null )
            {
                if (line.contains( from ))
                {
                    line = line.replace( from, to );
                }
                bw.write( line );
                bw.newLine();
                line = br.readLine();
            }
        }
        ApplicationUtils.copyFile( d, copy );
        copy.delete();
    }
}
