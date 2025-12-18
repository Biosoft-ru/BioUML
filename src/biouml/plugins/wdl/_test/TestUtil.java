package biouml.plugins.wdl._test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.CWLParser;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.diagram.WDLLayouter;
import biouml.plugins.wdl.diagram.WDLViewBuilder;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import biouml.workbench.diagram.ImageExporter;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.graphics.CompositeView;

public class TestUtil
{
    public static final String TEST_OK = "Ok";

    public static Diagram generateDiagram(String name, String wdl) throws Exception
    {
        WDLParser parser = new WDLParser();

        wdl = wdl.replace( "<<<", "{" ).replace( ">>>", "}" );
        AstStart start = parser.parse( new StringReader( wdl ) );
        WDLImporter importer = new WDLImporter();
        return importer.generateDiagram( start, null, name );
    }

    public static String loadDescription(String name) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/descriptions/" + name + ".txt" );
        if( url == null )
            return "TBA";

        File file = new File( url.getFile() );
        return ApplicationUtils.readAsString( file );
    }

    public static File loadTestFolder(String name) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/tests/" + name );
        if( url == null )
            throw new IllegalArgumentException( "No folder exists: " + name );

        return new File( url.getFile() );
    }

    public static String loadWDL(String name) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/wdl/" + name + ".wdl" );
        if( url == null )
            throw new IllegalArgumentException( "No input file exists: " + name );

        File file = new File( url.getFile() );
        return ApplicationUtils.readAsString( file );
    }

    public static Diagram loadDiagram(String name, String wdl) throws Exception
    {
        return generateDiagram( loadWDL( name ), wdl );
    }

    public static void exportImage(File imageFile, Diagram diagram) throws Exception
    {
        new WDLLayouter().layout( diagram );
        ImageExporter imageWriter = new ImageExporter();
        //        File file = new File( imagesDir, diagram.getName() + ".png" );
        Properties properties = new Properties();
        properties.setProperty( DataElementExporterRegistry.FORMAT, "PNG" );
        properties.setProperty( DataElementExporterRegistry.SUFFIX, ".png" );
        imageWriter.init( properties );
        imageWriter.doExport( diagram, imageFile );
    }

    public static Diagram loadDiagramCWL(String name) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/cwl/" + name + ".cwl" );
        if( url == null )
            throw new IllegalArgumentException( "No input file exists: " + name );

        return new CWLParser().loadDiagram( new File( url.getFile() ), null, name );
    }

    public static void deleteDir(File dir)
    {
        if( dir.isDirectory() )
        {
            for( File f : dir.listFiles() )
                deleteDir( f );
        }
        dir.delete();
    }

    public static String executeProcess(Process process) throws Exception
    {
        CommandRunner r = new CommandRunner( process, new NextFlowResultChecker() );
        Thread thread = new Thread( r );
        thread.start();
        process.waitFor();
        return r.isSuccess() ? TEST_OK : r.getError();
    }

    public static String executeCommand(String[] command) throws Exception
    {
        System.out.println( "Executing command " + StreamEx.of( command ).joining( " " ) );
        Process process = Runtime.getRuntime().exec( command );
        return executeProcess( process );
    }

    private static class CommandRunner implements Runnable
    {
        Process process;
        ResultChecker resultChecker;
        String error = "";
        boolean checked = false;

        public CommandRunner(Process process, ResultChecker resultChecker)
        {
            this.process = process;
            this.resultChecker = resultChecker;
        }

        public String getError()
        {
            return error;
        }

        public boolean isSuccess()
        {
            return error.isEmpty() && checked;
        }

        public void run()
        {
            BufferedReader input = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
            try
            {
                checked = resultChecker.check( input );
                error = StreamEx.of( resultChecker.getErrors() ).joining( "\n" );
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
            //                
            //for some reason cwl-runner outputs everything into error stream
            BufferedReader err = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
            String line = null;

            try
            {
                StringWriter sw = new StringWriter();
                err.transferTo( sw );
                error = error + sw.getBuffer().toString();

                while( ( line = err.readLine() ) != null )
                    System.out.println( line );
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }

        }
    }

    private static abstract class ResultChecker
    {
        List<String> errors = new ArrayList<>();
        public List<String> getErrors()
        {
            return errors;
        }
        public abstract boolean check(BufferedReader input) throws IOException;
    }

    private static class NextFlowResultChecker extends ResultChecker
    {

        public boolean check(BufferedReader input) throws IOException
        {
            String line = null;
            while( ( line = input.readLine() ) != null )
            {
                System.out.println( line );
                if( line.startsWith( "ERROR" ) || line.startsWith( "WARN" ) || line.startsWith( "Missing" ) )
                {
                    errors.add( line );
                }
            }
            return errors.isEmpty();
        }
    }

    private static class WDLValidationResultChecker extends ResultChecker
    {
        public boolean check(BufferedReader input) throws IOException
        {
            String line = null;
            while( ( line = input.readLine() ) != null )
            {
                System.out.println( line );
                if( line.startsWith( "Success" ) )
                    return true;
            }
            return false;
        }
    }

    public static BufferedImage generateImage(Diagram diagram, int width, int height)
    {
        new WDLLayouter().layout( diagram );
        BufferedImage image = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
        Graphics2D graphics2D = image.createGraphics();
        CompositeView view = new WDLViewBuilder().createDiagramView( diagram, graphics2D );
        view.paint( graphics2D );
        return image;
    }

    public static void showDiagram(Diagram diagram) throws Exception
    {
        int width = 1000;
        int height = 800;
        BufferedImage image = generateImage( diagram, width, height );

        JFrame frame = new JFrame( diagram.getName() );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        ImagePanel panel = new ImagePanel( image );
        frame.setSize( new Dimension( width, height ) );
        frame.add( panel );
        frame.setVisible( true );
    }


    public static class ImagePanel extends JPanel
    {
        private BufferedImage image;

        public ImagePanel(BufferedImage img)
        {
            this.image = img;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent( g );
            if( image != null )
                g.drawImage( image, 0, 0, this );
        }
    }


    public static String validateWDL(String wdlPath, String womtoolPath) throws Exception
    {
        String[] command = new String[] {"java", "-jar", womtoolPath, "validate", wdlPath};
        Process process = Runtime.getRuntime().exec( command );
        CommandRunner r = new CommandRunner( process, new WDLValidationResultChecker() );
        Thread thread = new Thread( r );
        thread.start();
        process.waitFor();
        if( r.isSuccess() )
            return TEST_OK;
        return r.getError();
    }
}
