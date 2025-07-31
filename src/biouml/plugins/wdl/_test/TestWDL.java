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
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.Test;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.NextFlowGenerator;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.WDLUtil;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.diagram.WDLViewBuilder;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import biouml.workbench.graph.DiagramToGraphTransformer;
import junit.framework.TestCase;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.util.TempFiles;

public class TestWDL extends TestCase
{

    private static String[] list = new String[] {"hello", "scatter_range_2_steps", "scatter_simple", "scatter_range", "scatter_range2",
            "two_steps", "four_steps"};

    public static void main(String ... args) throws Exception
    {
        //        for (String name: list)
        //            test(name);

        //                test( "scatter_range_2_extra" );
        //                test( "scatter_range_2_steps" );
        //        test( "scatter_simple" );
        //        test( "scatter_range" );
        //        test( "scatter_range2" );
        //        test( "two_steps" );
        //        test( "four_steps" );
//        test( "private_declaration_task" );
                test( "pbmm2" );

        //        test( "lima" );
        //                test("faidx2");
        //        test("faidx_import");
        //        test( "fastqc1" );
        //        test( "test_scatter" );
    }

    @Test
    public static void test() throws Exception
    {
        test( "hello" );
    }

    public static void testOrder(String name) throws Exception
    {
        Diagram diagram = loadDiagram( name );
        WDLUtil.orderCallsScatters( diagram );
    }
    public static void test(String name) throws Exception
    {
        Diagram diagram = loadDiagram( name );
        WDLGenerator wdlGenerator = new WDLGenerator();
        NextFlowGenerator nextFlowGenerator = new NextFlowGenerator();
        String wdl = wdlGenerator.generateWDL( diagram );

        String nextFlow = nextFlowGenerator.generateNextFlow( diagram );

//        runNextFlow( name, nextFlow );
        printNextflow( nextFlow );
        //        printWDL( wdl );

        //        saveResult( name, nextFlow );

        //        checkScript( name, nextFlow );
    }

    private static void printWDL(String wdl) throws Exception
    {
        System.out.println( "Rexported WDL: " );
        System.out.println( wdl );
    }

    private static void printNextflow(String nextFlow) throws Exception
    {
        System.out.println( "Rexported Nextflow: " );
        System.out.println( nextFlow );
    }

    private static void checkScript(String name, String nextFlow) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/nextflow/" + name + ".nf" );
        File f = new File( url.getFile() );
        String test = ApplicationUtils.readAsString( new File( url.getFile() ) );
        assertEquals( test, nextFlow );
    }

    private static void saveResult(String name, String nextFlow) throws Exception
    {
        File f = new File( "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/nextflow/" + name + ".nf" );
        ApplicationUtils.writeString( f, nextFlow );
    }

    private static Diagram loadDiagram(String name) throws Exception
    {
        URL url = TestWDL.class.getResource( "../test_examples/" + name + ".wdl" );
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

    private static void layoutDiagram(Diagram diagram) throws Exception
    {
        HierarchicLayouter layouter = new HierarchicLayouter();
        //        Layouter layouter = new FastGridLayouter();
        DiagramToGraphTransformer.layout( diagram, layouter );
    }

    private static void showDiagram(String name) throws Exception
    {
        Diagram diagram = loadDiagram( name );
        showDiagram( diagram );
    }

    private static void showDiagram(Diagram diagram) throws Exception
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

    private static void runNextFlow(String name, String script, List<String> imports)
    {
        try
        {
            String outputDir = TempFiles.path( "nextflow" ).getAbsolutePath();
            new File( outputDir ).mkdirs();
            generateFunctions( outputDir );
            
            
            for( String imported: imports )
            {
                URL url = TestWDL.class.getResource( "../test_examples/nextflow/" + imported + ".nf" );
                if( url == null )
                    throw new IllegalArgumentException( "No input file exists: " + name );

                File file = new File( url.getFile() );
                File copy = new File(outputDir, file.getName());
                ApplicationUtils.copyFile( copy, file );
            }
            
            //            NextFlowPreprocessor preprocessor = new NextFlowPreprocessor();
            //            script = preprocessor.preprocess( script );
            File f = new File( outputDir, name + ".nf" );
            ApplicationUtils.writeString( f, script );
            String parent = new File( outputDir ).getAbsolutePath().replace( "\\", "/" );
            String[] command = new String[] {"wsl", "--cd", parent, "nextflow", f.getName()};
            executeCommand( command );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private static void executeCommand(String[] command) throws Exception
    {
        System.out.println( "Executing command " + StreamEx.of( command ).joining( " " ) );
        Process process = Runtime.getRuntime().exec( command );

        new Thread( new Runnable()
        {
            public void run()
            {
                BufferedReader input = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
                String line = null;

                try
                {
                    while( ( line = input.readLine() ) != null )
                        System.out.println( line );
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
        } ).start();

        process.waitFor();

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
