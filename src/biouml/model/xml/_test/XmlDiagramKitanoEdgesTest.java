package biouml.model.xml._test;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramViewBuilder;
import biouml.model.Node;
import biouml.model.xml.XmlDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;

/**
 * Batch unit test for biouml.model package.
 */
public class XmlDiagramKitanoEdgesTest extends TestCase
{
    static String repositoryPath = "../data_resources";
    static DataCollection repository;

    /** Standard JUnit constructor */
    public XmlDiagramKitanoEdgesTest(String name)
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
            junit.swingui.TestRunner.run(XmlDiagramKitanoEdgesTest.class);
        }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(XmlDiagramKitanoEdgesTest.class.getName());
        suite.addTest(new XmlDiagramKitanoEdgesTest("testXmlDiagram"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testXmlDiagram() throws Exception
    {
        JFrame frame = new JFrame("Kitano test");
        frame.show();

        repository = CollectionFactory.createRepository(repositoryPath);
        XmlDiagramType xdt = XmlDiagramType.getTypeObject("kitano.xml");

        Container content = frame.getContentPane();
        ViewPane viewPane = new ViewPane();

        Diagram diagram = generateTestDiagram(xdt);

        Graphics g = frame.getGraphics();
        DiagramViewBuilder dvb = xdt.getDiagramViewBuilder();
        CompositeView view = dvb.createDiagramView(diagram, g);

        viewPane.setView(view);

        content.add(viewPane);
        frame.setSize(1100, 800);
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

    private Diagram generateTestDiagram(DiagramType diagramType) throws Exception
    {
        Diagram diagram = new Diagram(null, new DiagramInfo("test"), diagramType);

        Node node;
        Stub p1;
        Stub p2;
        Stub r1;
        String[] nodes;
        String[] roles;
        
///////////////////////////////
        p1 = new Stub(null, "p1", "molecule-protein");
        node = new Node(diagram, "pr_1_1", p1);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(10, 30);
        diagram.put(node);
        
        p2 = new Stub(null, "p2", "molecule-protein");
        node = new Node(diagram, "pr_2_1", p2);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(230, 30);
        diagram.put(node);
        
        r1 = new Stub(null, "r1", "reaction");
        node = new Node(diagram, r1);
        node.setLocation(150, 50);
        nodes = new String[2];
        roles = new String[2];
        nodes[0] = "pr_1_1";
        roles[0] = "reactant";
        nodes[1] = "pr_2_1";
        roles[1] = "product";
        node.getAttributes().add(new DynamicProperty("nodes", DynamicPropertySet[].class, nodes));
        node.getAttributes().add(new DynamicProperty("nodeRoles", DynamicPropertySet[].class, roles));
        diagram.put(node);
        
///////////////////////////////
        p1 = new Stub(null, "p1", "molecule-protein");
        node = new Node(diagram, "pr_1_2", p1);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(10, 80);
        diagram.put(node);
        
        p2 = new Stub(null, "p2", "molecule-protein");
        node = new Node(diagram, "pr_2_2", p2);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(230, 80);
        diagram.put(node);
        
        r1 = new Stub(null, "r2", "reaction");
        node = new Node(diagram, r1);
        node.setLocation(150, 100);
        nodes = new String[2];
        roles = new String[2];
        nodes[0] = "pr_1_2";
        roles[0] = "reactant";
        nodes[1] = "pr_2_2";
        roles[1] = "product";
        node.getAttributes().add(new DynamicProperty("nodes", DynamicPropertySet[].class, nodes));
        node.getAttributes().add(new DynamicProperty("nodeRoles", DynamicPropertySet[].class, roles));
        diagram.put(node);

///////////////////////////////
        p1 = new Stub(null, "p1", "molecule-protein");
        node = new Node(diagram, "pr_1_3", p1);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(10, 130);
        diagram.put(node);
        
        p2 = new Stub(null, "p2", "molecule-protein");
        node = new Node(diagram, "pr_2_3", p2);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(230, 130);
        diagram.put(node);
        
        r1 = new Stub(null, "r3", "reaction");
        node = new Node(diagram, r1);
        node.setLocation(150, 150);
        nodes = new String[2];
        roles = new String[2];
        nodes[0] = "pr_1_3";
        roles[0] = "reactant";
        nodes[1] = "pr_2_3";
        roles[1] = "product";
        node.getAttributes().add(new DynamicProperty("nodes", DynamicPropertySet[].class, nodes));
        node.getAttributes().add(new DynamicProperty("nodeRoles", DynamicPropertySet[].class, roles));
        diagram.put(node);

///////////////////////////////
        p1 = new Stub(null, "p1", "molecule-protein");
        node = new Node(diagram, "pr_1_4", p1);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(10, 180);
        diagram.put(node);
        
        p2 = new Stub(null, "p2", "molecule-protein");
        node = new Node(diagram, "pr_2_4", p2);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(230, 180);
        diagram.put(node);
        
        r1 = new Stub(null, "r4", "reaction");
        node = new Node(diagram, r1);
        node.setLocation(150, 200);
        nodes = new String[2];
        roles = new String[2];
        nodes[0] = "pr_1_4";
        roles[0] = "reactant";
        nodes[1] = "pr_2_4";
        roles[1] = "product";
        node.getAttributes().add(new DynamicProperty("nodes", DynamicPropertySet[].class, nodes));
        node.getAttributes().add(new DynamicProperty("nodeRoles", DynamicPropertySet[].class, roles));
        diagram.put(node);

///////////////////////////////
        p1 = new Stub(null, "p1", "molecule-protein");
        node = new Node(diagram, "pr_1_5", p1);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(10, 230);
        diagram.put(node);
        
        p2 = new Stub(null, "p2", "molecule-protein");
        node = new Node(diagram, "pr_2_5", p2);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(230, 230);
        diagram.put(node);
        
        r1 = new Stub(null, "r5", "reaction");
        node = new Node(diagram, r1);
        node.setLocation(150, 250);
        nodes = new String[2];
        roles = new String[2];
        nodes[0] = "pr_1_5";
        roles[0] = "reactant";
        nodes[1] = "pr_2_5";
        roles[1] = "product";
        node.getAttributes().add(new DynamicProperty("nodes", DynamicPropertySet[].class, nodes));
        node.getAttributes().add(new DynamicProperty("nodeRoles", DynamicPropertySet[].class, roles));
        diagram.put(node);
        
///////////////////////////////
        p1 = new Stub(null, "p1", "molecule-protein");
        node = new Node(diagram, "pr_1_6", p1);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(10, 280);
        diagram.put(node);
        
        p2 = new Stub(null, "p2", "molecule-protein");
        node = new Node(diagram, "pr_2_6", p2);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(230, 280);
        diagram.put(node);
        
        r1 = new Stub(null, "r6", "reaction");
        node = new Node(diagram, r1);
        node.setLocation(150, 300);
        nodes = new String[2];
        roles = new String[2];
        nodes[0] = "pr_1_6";
        roles[0] = "reactant";
        nodes[1] = "pr_2_6";
        roles[1] = "product";
        node.getAttributes().add(new DynamicProperty("nodes", DynamicPropertySet[].class, nodes));
        node.getAttributes().add(new DynamicProperty("nodeRoles", DynamicPropertySet[].class, roles));
        diagram.put(node);

///////////////////////////////
        p1 = new Stub(null, "p1", "molecule-protein");
        node = new Node(diagram, "pr_1_7", p1);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(10, 330);
        diagram.put(node);
        
        p2 = new Stub(null, "p2", "molecule-protein");
        node = new Node(diagram, "pr_2_7", p2);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(230, 330);
        diagram.put(node);
        
        r1 = new Stub(null, "r7", "reaction");
        node = new Node(diagram, r1);
        node.setLocation(150, 350);
        nodes = new String[2];
        roles = new String[2];
        nodes[0] = "pr_1_7";
        roles[0] = "reactant";
        nodes[1] = "pr_2_7";
        roles[1] = "product";
        node.getAttributes().add(new DynamicProperty("nodes", DynamicPropertySet[].class, nodes));
        node.getAttributes().add(new DynamicProperty("nodeRoles", DynamicPropertySet[].class, roles));
        diagram.put(node);

///////////////////////////////
        p1 = new Stub(null, "p1", "molecule-protein");
        node = new Node(diagram, "pr_1_8", p1);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(10, 380);
        diagram.put(node);
        
        p2 = new Stub(null, "p2", "molecule-protein");
        node = new Node(diagram, "pr_2_8", p2);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(230, 380);
        diagram.put(node);
        
        r1 = new Stub(null, "r8", "reaction");
        node = new Node(diagram, r1);
        node.setLocation(150, 400);
        nodes = new String[2];
        roles = new String[2];
        nodes[0] = "pr_1_8";
        roles[0] = "reactant";
        nodes[1] = "pr_2_8";
        roles[1] = "product";
        node.getAttributes().add(new DynamicProperty("nodes", DynamicPropertySet[].class, nodes));
        node.getAttributes().add(new DynamicProperty("nodeRoles", DynamicPropertySet[].class, roles));
        diagram.put(node);

        return diagram;
    }
}