package biouml.plugins.wdl._test;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.StringReader;

import javax.swing.JPanel;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.parser.AstBashString;
import biouml.plugins.wdl.parser.AstCall;
import biouml.plugins.wdl.parser.AstCommand;
import biouml.plugins.wdl.parser.AstDeclaration;
import biouml.plugins.wdl.parser.AstInput;
import biouml.plugins.wdl.parser.AstOutput;
import biouml.plugins.wdl.parser.AstRuntime;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.AstSymbol;
import biouml.plugins.wdl.parser.AstTask;
import biouml.plugins.wdl.parser.AstVersion;
import biouml.plugins.wdl.parser.AstWorkflow;
import biouml.plugins.wdl.parser.SimpleNode;
import biouml.plugins.wdl.parser.WDLParser;

public class TestImage
{

    private static String name = "Example";
    private static String path = "C:/Users/Damag/eclipse_2024_6/biouml_wdl/test_examples/hello.wdl";
    private static Diagram diagram;

    public static void main(String ... args) throws Exception
    {
        WDLParser parser = new WDLParser();
        String wdl = ApplicationUtils.readAsString( new File( path ) );
        wdl = wdl.replace( "<<<", "{" ).replace( ">>>", "}" );
        AstStart start = parser.parse( new StringReader( wdl ) );
        WDLImporter importer = new WDLImporter();
        diagram = importer.generateDiagram( start, null, name );

        WDLGenerator generator = new WDLGenerator();
        String s = generator.generateWDL( diagram );
        System.out.println( "Rexported WDL: " );
        System.out.println( s);
    }

    public static class Visitor
    {
        public void visitNode(SimpleNode node)
        {
            if( node instanceof AstVersion )
            {
                System.out.println( ( (AstVersion)node ).jjtGetLastToken() );
            }
            else if( node instanceof AstTask )
            {
                AstTask taskNode = ( (AstTask)node );
                String name = taskNode.getName();
                System.out.println( "TASK " + name );
            }
            else if( node instanceof AstInput )
            {
                AstInput inputNode = ( (AstInput)node );
                System.out.println( "INPUT " );
            }
            else if( node instanceof AstOutput )
            {
                AstOutput outputNode = ( (AstOutput)node );
                System.out.println( "OUTPUT " );
            }
            else if( node instanceof AstDeclaration )
            {
                AstDeclaration declarationNode = ( (AstDeclaration)node );
                System.out.println( "DECLARATION " + declarationNode.toString() );
            }
            else if( node instanceof AstCommand )
            {
                AstCommand commandNode = ( (AstCommand)node );
                System.out.println( "COMMAND " );
            }
            else if( node instanceof AstWorkflow )
            {
                AstWorkflow workflowNode = ( (AstWorkflow)node );
                System.out.println( "WORKFLOW " + workflowNode.getName() );
            }
            else if( node instanceof AstCall )
            {
                AstCall callNode = ( (AstCall)node );
                AstSymbol[] inputs = callNode.getInputs();
                System.out.println( "CALL " + callNode.getName() + " as " + callNode.getAlias() );
                for( AstSymbol input : inputs )
                    System.out.println( input.toString() );
            }
            else if( node instanceof AstBashString )
            {
                AstBashString bashNode = ( (AstBashString)node );
                System.out.println( "BASH " + bashNode.getCommand() );
            }
            else if( node instanceof AstRuntime )
            {
                AstRuntime runtimeNode = (AstRuntime)node;
                System.out.println( runtimeNode.toString() );
            }


            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            {
                visitNode( (SimpleNode)node.jjtGetChild( i ) );
            }
        }
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
