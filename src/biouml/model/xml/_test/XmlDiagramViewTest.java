package biouml.model.xml._test;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramViewBuilder;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Stub;
import biouml.workbench.graph.DiagramToGraphTransformer;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;

/**
 * Batch unit test for biouml.model package.
 */
public class XmlDiagramViewTest extends TestCase
{
    /** Standard JUnit constructor */
    public XmlDiagramViewTest(String name)
    {
        super(name);
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main(String[] args)
    {
        if( args != null && args.length > 0 && args[0].startsWith("text") )
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(XmlDiagramViewTest.class);
        }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(XmlDiagramViewTest.class.getName());
        suite.addTest(new XmlDiagramViewTest("testXmlDiagram"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testXmlDiagram() throws Exception
    {
        Diagram diagram = generateTestDiagram(new XmlDiagramType());

        try
        {
            JFrame frame = new JFrame("XmlDiagram test");
            frame.show();

            Container content = frame.getContentPane();
            ViewPane viewPane = new ViewPane();

            Graphics g = frame.getGraphics();
            DiagramViewBuilder dvb = diagram.getType().getDiagramViewBuilder();
            fillXmlDiagramViewBuilder((XmlDiagramViewBuilder)dvb);
            CompositeView view = dvb.createDiagramView(diagram, g);

            viewPane.setView(view);

            content.add(viewPane);
            frame.setSize(600, 600);
            frame.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosed(WindowEvent e)
                {
                    System.exit(0);
                }
            });
            while( true )
            {
                Thread.sleep(100);
            }
        }
        catch( java.awt.HeadlessException ignore ) {}        
    }

    private Diagram generateTestDiagram(DiagramType diagramType) throws Exception
    {
        Diagram diagram = new Diagram(null, new DiagramInfo("test"), diagramType);

        Stub gene = new Stub(null, "gene", "gene_type");
        Node node1 = new Node(diagram, gene);
        diagram.put(node1);

        Stub protein = new Stub(null, "protein", "protein_type");
        Node node2 = new Node(diagram, protein);
        diagram.put(node2);

        Stub substance = new Stub(null, "substance", "substance_type");
        Node node3 = new Node(diagram, substance);
        diagram.put(node3);

        Edge edge1 = new Edge(new SemanticRelation(null, "edge1"), node1, node2);
        diagram.put(edge1);

        Edge edge2 = new Edge(new SemanticRelation(null, "edge2"), node2, node3);
        diagram.put(edge2);

        ForceDirectedLayouter layouter = new ForceDirectedLayouter();
        DiagramToGraphTransformer.layout(diagram, layouter);

        return diagram;
    }

    private void fillXmlDiagramViewBuilder(XmlDiagramViewBuilder xdvb)
    {
        String proteinScript = ""
                + "function f(container, node, options, g)"
                + "{"
                + "    var type = new TextView(node.getKernel().getName(), options.getDefaultFont(), g); "
                + "    type.setLocation(5, type.getBounds().width/2-5); "
                + "    var ellipse = new EllipseView(options.getDefaultPen(), new Brush(Color.pink), 0, 0, type.getBounds().width + 10, type.getBounds().width + 10); "
                + "    container.add(ellipse); "
                + "    container.add(type); "
                + "    return false;"
                + "}";

        String substanceScript = ""
                + "function f(container, node, options, g)"
                + "{"
                + "    var type = new TextView(node.getKernel().getName(), options.getDefaultFont(), g); "
                + "    type.setLocation(5, type.getBounds().width/2-5); "
                + "    var box = new BoxView(options.getDefaultPen(), new Brush(Color.blue), 0, 0, type.getBounds().width + 10, type.getBounds().width + 10); "
                + "    container.add(box); "
                + "    container.add(type); "
                + "    return false;"
                + "}";

        String geneScript = ""
                + "function f(container, node, options, g)"
                + "{"
                + "    var type = new TextView(node.getKernel().getName(), options.getDefaultFont(), g); "
                + "    type.setLocation(5, type.getBounds().width/2-5); "
                + "    var ellipse = new BoxView(options.getDefaultPen(), new Brush(Color.orange.darker()), 0, 0, type.getBounds().width + 10, type.getBounds().width + 10); "
                + "    container.add(ellipse); "
                + "    container.add(type); "
                + "    return false;"
                + "}";
        
        String semanticRelationScript = ""
            + "function f(container, edge, inPoint, outPoint, options, g)"
            + "{"
            + "    var arrow = new ArrowView(new Pen(2.5, Color.red), new Brush(Color.blue), inPoint.x, inPoint.y, outPoint.x, outPoint.y, 0, 0); "
            + "    container.add(arrow); "
            + "    return false;"
            + "}";

        xdvb.addFunction("protein_type", proteinScript);
        xdvb.addFunction("substance_type", substanceScript);
        xdvb.addFunction("gene_type", geneScript);
        xdvb.addFunction(SemanticRelation.class.getName(), semanticRelationScript);
    }
}