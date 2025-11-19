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
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.diagram.WDLViewBuilder;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import biouml.workbench.graph.DiagramToGraphTransformer;
import one.util.streamex.StreamEx;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graphics.CompositeView;

public class TestUtil
{

    public static Diagram loadDiagram(String name) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/wdl/" + name + ".wdl" );
        if( url == null )
            throw new IllegalArgumentException( "No input file exists: " + name );

        File file = new File( url.getFile() );
        WDLParser parser = new WDLParser();
        String wdl = ApplicationUtils.readAsString( file );
        wdl = wdl.replace( "<<<", "{" ).replace( ">>>", "}" );
        AstStart start = parser.parse( new StringReader( wdl ) );
        WDLImporter importer = new WDLImporter();
        return importer.generateDiagram( start, null, name );
    }

    public static void layoutDiagram(Diagram diagram) throws Exception
    {
        HierarchicLayouter layouter = new HierarchicLayouter();
        //        Layouter layouter = new FastGridLayouter();
        DiagramToGraphTransformer.layout( diagram, layouter );
    }

    public static void showDiagram(String name) throws Exception
    {
        Diagram diagram = loadDiagram( name );
        showDiagram( diagram );
    }


    public static boolean executeCommand(String[] command) throws Exception
    {
        System.out.println( "Executing command " + StreamEx.of( command ).joining( " " ) );
        Process process = Runtime.getRuntime().exec( command );
        CommandRunner r = new CommandRunner( process );
        Thread thread = new Thread( r );
        thread.start();
        process.waitFor();
        return r.isSuccess();
    }

    private static class CommandRunner implements Runnable
    {
        Process process;
        public CommandRunner(Process process)
        {
            this.process = process;
        }

        public boolean success;

        public boolean isSuccess()
        {
            return success;
        }
        public void run()
        {
            success = true;
            BufferedReader input = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
            String line = null;

            try
            {
                while( ( line = input.readLine() ) != null )
                {
                    System.out.println( line );
                    if( line.startsWith( "ERROR" ) || line.startsWith( "WARN" ) || line.startsWith( "Missing" ))
                        success = false;
                }
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
            //                
            //for some reason cwl-runner outputs everything into error stream
            BufferedReader err = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
            line = null;

            try
            {
                while( ( line = err.readLine() ) != null )
                    System.out.println( line );
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
        }
    }

    public static void showDiagram(Diagram diagram) throws Exception
    {
        int width = 1000;
        int height = 800;
        layoutDiagram( diagram );
        BufferedImage image = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
        Graphics2D graphics2D = image.createGraphics();

        CompositeView view = new WDLViewBuilder().createDiagramView( diagram, graphics2D );
        view.paint( graphics2D );

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
}
