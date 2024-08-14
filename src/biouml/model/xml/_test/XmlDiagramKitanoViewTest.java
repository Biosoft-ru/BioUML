package biouml.model.xml._test;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

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
public class XmlDiagramKitanoViewTest extends TestCase
{
    static String repositoryPath = "../data_resources";
    static DataCollection repository;

    /** Standard JUnit constructor */
    public XmlDiagramKitanoViewTest(String name)
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
            junit.swingui.TestRunner.run(XmlDiagramKitanoViewTest.class);
        }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(XmlDiagramKitanoViewTest.class.getName());
        suite.addTest(new XmlDiagramKitanoViewTest("testXmlDiagram"));
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
        frame.setSize(1100, 900);
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

        Stub protein1 = new Stub(null, "protein1", "molecule-protein");
        node = new Node(diagram, protein1);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.setLocation(10, 10);
        diagram.put(node);

        Stub protein2 = new Stub(null, "protein2", "molecule-protein");
        node = new Node(diagram, protein2);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "receptor"));
        node.setLocation(10, 80);
        diagram.put(node);

        Stub protein3 = new Stub(null, "protein3", "molecule-protein");
        node = new Node(diagram, protein3);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "ion channel"));
        node.setLocation(10, 150);
        diagram.put(node);

        Stub protein4 = new Stub(null, "protein4", "molecule-protein");
        node = new Node(diagram, protein4);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "truncated"));
        node.setLocation(10, 220);
        diagram.put(node);

        Stub protein5 = new Stub(null, "protein5", "molecule-protein");
        node = new Node(diagram, protein5);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.setLocation(10, 290);
        diagram.put(node);

        Stub protein6 = new Stub(null, "protein6", "molecule-protein");
        node = new Node(diagram, protein6);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 5));
        node.setLocation(10, 360);
        diagram.put(node);

        Stub protein7 = new Stub(null, "protein7", "molecule-protein");
        node = new Node(diagram, protein7);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "truncated"));
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.setLocation(10, 430);
        diagram.put(node);

        Stub protein8 = new Stub(null, "protein8", "molecule-protein");
        node = new Node(diagram, protein8);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "receptor"));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(10, 500);
        diagram.put(node);

        Stub protein9 = new Stub(null, "protein9", "molecule-protein");
        node = new Node(diagram, protein9);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "ion channel"));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(10, 570);
        diagram.put(node);

        Stub protein10 = new Stub(null, "protein10", "molecule-protein");
        node = new Node(diagram, protein10);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "truncated"));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(10, 640);
        diagram.put(node);

        Stub protein11 = new Stub(null, "protein11", "molecule-protein");
        node = new Node(diagram, protein11);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        DynamicPropertySet residues[] = new DynamicPropertySet[1];
        residues[0] = new DynamicPropertySetAsMap();
        residues[0].add(new DynamicProperty("id", String.class, "id1"));
        residues[0].add(new DynamicProperty("name", String.class, "name1"));
        residues[0].add(new DynamicProperty("angle", Double.class, 0.5));
        residues[0].add(new DynamicProperty("modification", String.class, "phosporylated"));
        node.getAttributes().add(new DynamicProperty("residues", DynamicPropertySet[].class, residues));
        node.setLocation(10, 710);
        diagram.put(node);

        Stub protein12 = new Stub(null, "protein12", "molecule-protein");
        node = new Node(diagram, protein12);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        residues = new DynamicPropertySet[1];
        residues[0] = new DynamicPropertySetAsMap();
        residues[0].add(new DynamicProperty("id", String.class, "id1"));
        residues[0].add(new DynamicProperty("name", String.class, "name1"));
        residues[0].add(new DynamicProperty("angle", Double.class, 2.0));
        residues[0].add(new DynamicProperty("modification", String.class, "acetylated"));
        node.getAttributes().add(new DynamicProperty("residues", DynamicPropertySet[].class, residues));
        node.setLocation(120, 710);
        diagram.put(node);

        Stub protein13 = new Stub(null, "protein13", "molecule-protein");
        node = new Node(diagram, protein13);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 3));
        residues = new DynamicPropertySetAsMap[1];
        residues[0] = new DynamicPropertySetAsMap();
        residues[0].add(new DynamicProperty("id", String.class, "id1"));
        residues[0].add(new DynamicProperty("name", String.class, "name1"));
        residues[0].add(new DynamicProperty("angle", Double.class, -1.5));
        residues[0].add(new DynamicProperty("modification", String.class, "ubiquitinated"));
        node.getAttributes().add(new DynamicProperty("residues", DynamicPropertySet[].class, residues));
        node.setLocation(230, 710);
        diagram.put(node);

        Stub protein14 = new Stub(null, "protein14", "molecule-protein");
        node = new Node(diagram, protein14);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        residues = new DynamicPropertySetAsMap[1];
        residues[0] = new DynamicPropertySetAsMap();
        residues[0].add(new DynamicProperty("id", String.class, "id1"));
        residues[0].add(new DynamicProperty("name", String.class, "name1"));
        residues[0].add(new DynamicProperty("angle", Double.class, 3.0));
        residues[0].add(new DynamicProperty("modification", String.class, "methylated"));
        node.getAttributes().add(new DynamicProperty("residues", DynamicPropertySet[].class, residues));
        node.setLocation(340, 710);
        diagram.put(node);

        Stub protein15 = new Stub(null, "protein15", "molecule-protein");
        node = new Node(diagram, protein15);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "generic"));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        residues = new DynamicPropertySetAsMap[10];
        for( int i = 0; i < 10; i++ )
        {
            residues[i] = new DynamicPropertySetAsMap();
            residues[i].add(new DynamicProperty("id", String.class, "id" + i));
            residues[i].add(new DynamicProperty("name", String.class, "name" + i));
            residues[i].add(new DynamicProperty("angle", Double.class, ( i - 5 ) / 5.0 * Math.PI));
            residues[i].add(new DynamicProperty("modification", String.class, "hydroxylated"));
        }
        node.getAttributes().add(new DynamicProperty("residues", DynamicPropertySet[].class, residues));
        node.setLocation(450, 710);
        diagram.put(node);

        Stub protein16 = new Stub(null, "protein16", "molecule-protein");
        node = new Node(diagram, protein16);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "receptor"));
        residues = new DynamicPropertySetAsMap[10];
        for( int i = 0; i < 10; i++ )
        {
            residues[i] = new DynamicPropertySetAsMap();
            residues[i].add(new DynamicProperty("id", String.class, "id" + i));
            residues[i].add(new DynamicProperty("name", String.class, "name" + i));
            residues[i].add(new DynamicProperty("angle", Double.class, ( i - 5 ) / 5.0 * Math.PI));
            residues[i].add(new DynamicProperty("modification", String.class, "empty"));
        }
        node.getAttributes().add(new DynamicProperty("residues", DynamicPropertySet[].class, residues));
        node.setLocation(580, 710);
        diagram.put(node);

        Stub protein17 = new Stub(null, "protein17", "molecule-protein");
        node = new Node(diagram, protein17);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "truncated"));
        residues = new DynamicPropertySetAsMap[4];
        for( int i = 0; i < 2; i++ )
        {
            residues[i] = new DynamicPropertySetAsMap();
            residues[i].add(new DynamicProperty("id", String.class, "id" + i));
            residues[i].add(new DynamicProperty("name", String.class, "name" + i));
            residues[i].add(new DynamicProperty("angle", Double.class, ( i*2-1 ) * 0.2));
            residues[i].add(new DynamicProperty("modification", String.class, "don't care"));
        }
        for( int i = 0; i < 2; i++ )
        {
            residues[i+2] = new DynamicPropertySetAsMap();
            residues[i+2].add(new DynamicProperty("id", String.class, "id" + i));
            residues[i+2].add(new DynamicProperty("name", String.class, "name" + i));
            residues[i+2].add(new DynamicProperty("angle", Double.class, ( i*2-1 ) * 3.0));
            residues[i+2].add(new DynamicProperty("modification", String.class, "don't care"));
        }
        node.getAttributes().add(new DynamicProperty("residues", DynamicPropertySet[].class, residues));
        node.setLocation(710, 710);
        diagram.put(node);

        Stub protein18 = new Stub(null, "protein18", "molecule-protein");
        node = new Node(diagram, protein18);
        node.getAttributes().add(new DynamicProperty("proteinType", String.class, "ion channel"));
        residues = new DynamicPropertySetAsMap[1];
        residues[0] = new DynamicPropertySetAsMap();
        residues[0].add(new DynamicProperty("id", String.class, "id1"));
        residues[0].add(new DynamicProperty("name", String.class, "name1"));
        residues[0].add(new DynamicProperty("angle", Double.class, -2.0));
        residues[0].add(new DynamicProperty("modification", String.class, "unknown"));
        node.getAttributes().add(new DynamicProperty("residues", DynamicPropertySet[].class, residues));
        node.setLocation(840, 710);
        diagram.put(node);

        Stub gene1 = new Stub(null, "gene1", "molecule-gene");
        node = new Node(diagram, gene1);
        node.setLocation(120, 10);
        diagram.put(node);

        Stub gene2 = new Stub(null, "gene2", "molecule-gene");
        node = new Node(diagram, gene2);
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.setLocation(120, 80);
        diagram.put(node);

        Stub gene3 = new Stub(null, "gene3", "molecule-gene");
        node = new Node(diagram, gene3);
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(120, 150);
        diagram.put(node);

        Stub gene4 = new Stub(null, "gene4", "molecule-gene");
        node = new Node(diagram, gene4);
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(120, 220);
        diagram.put(node);

        Stub gene5 = new Stub(null, "gene5", "molecule-gene");
        node = new Node(diagram, gene5);
        DynamicPropertySet regions[] = new DynamicPropertySetAsMap[1];
        regions[0] = new DynamicPropertySetAsMap();
        regions[0].add(new DynamicProperty("id", String.class, "id1"));
        regions[0].add(new DynamicProperty("name", String.class, "name1"));
        regions[0].add(new DynamicProperty("type", String.class, "empty"));
        regions[0].add(new DynamicProperty("size", Integer.class, 40));
        regions[0].add(new DynamicProperty("position", Integer.class, 20));
        node.getAttributes().add(new DynamicProperty("geneRegions", DynamicPropertySet[].class, regions));
        node.setLocation(120, 290);
        diagram.put(node);

        Stub gene6 = new Stub(null, "gene6", "molecule-gene");
        node = new Node(diagram, gene6);
        regions = new DynamicPropertySetAsMap[1];
        regions[0] = new DynamicPropertySetAsMap();
        regions[0].add(new DynamicProperty("id", String.class, "id1"));
        regions[0].add(new DynamicProperty("name", String.class, "name1"));
        regions[0].add(new DynamicProperty("type", String.class, "promoter"));
        regions[0].add(new DynamicProperty("size", Integer.class, 20));
        regions[0].add(new DynamicProperty("position", Integer.class, 50));
        node.getAttributes().add(new DynamicProperty("geneRegions", DynamicPropertySet[].class, regions));
        node.setLocation(120, 360);
        diagram.put(node);

        Stub gene7 = new Stub(null, "gene7", "molecule-gene");
        node = new Node(diagram, gene7);
        regions = new DynamicPropertySetAsMap[1];
        regions[0] = new DynamicPropertySetAsMap();
        regions[0].add(new DynamicProperty("id", String.class, "id1"));
        regions[0].add(new DynamicProperty("name", String.class, "name1"));
        regions[0].add(new DynamicProperty("type", String.class, "codingRegion"));
        regions[0].add(new DynamicProperty("size", Integer.class, 30));
        regions[0].add(new DynamicProperty("position", Integer.class, 40));
        node.getAttributes().add(new DynamicProperty("geneRegions", DynamicPropertySet[].class, regions));
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.setLocation(120, 430);
        diagram.put(node);

        Stub gene8 = new Stub(null, "gene8", "molecule-gene");
        node = new Node(diagram, gene8);
        regions = new DynamicPropertySetAsMap[1];
        regions[0] = new DynamicPropertySetAsMap();
        regions[0].add(new DynamicProperty("id", String.class, "id1"));
        regions[0].add(new DynamicProperty("name", String.class, "name1"));
        regions[0].add(new DynamicProperty("type", String.class, "transcriptionStartingPointL"));
        regions[0].add(new DynamicProperty("size", Integer.class, 40));
        regions[0].add(new DynamicProperty("position", Integer.class, 10));
        node.getAttributes().add(new DynamicProperty("geneRegions", DynamicPropertySet[].class, regions));
        node.setLocation(120, 500);
        diagram.put(node);

        Stub gene9 = new Stub(null, "gene9", "molecule-gene");
        node = new Node(diagram, gene9);
        regions = new DynamicPropertySetAsMap[1];
        regions[0] = new DynamicPropertySetAsMap();
        regions[0].add(new DynamicProperty("id", String.class, "id1"));
        regions[0].add(new DynamicProperty("name", String.class, "name1"));
        regions[0].add(new DynamicProperty("type", String.class, "transcriptionStartingPointR"));
        regions[0].add(new DynamicProperty("size", Integer.class, 40));
        regions[0].add(new DynamicProperty("position", Integer.class, 30));
        node.getAttributes().add(new DynamicProperty("geneRegions", DynamicPropertySet[].class, regions));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(120, 570);
        diagram.put(node);

        Stub rna1 = new Stub(null, "rna1", "molecule-RNA");
        node = new Node(diagram, rna1);
        node.getAttributes().add(new DynamicProperty("rnaType", String.class, "generic"));
        node.setLocation(230, 10);
        diagram.put(node);

        Stub rna2 = new Stub(null, "rna2", "molecule-RNA");
        node = new Node(diagram, rna2);
        node.getAttributes().add(new DynamicProperty("rnaType", String.class, "antisence RNA"));
        node.setLocation(230, 80);
        diagram.put(node);

        Stub rna3 = new Stub(null, "rna3", "molecule-RNA");
        node = new Node(diagram, rna3);
        node.getAttributes().add(new DynamicProperty("rnaType", String.class, "generic"));
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.setLocation(230, 150);
        diagram.put(node);

        Stub rna4 = new Stub(null, "rna4", "molecule-RNA");
        node = new Node(diagram, rna4);
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(230, 220);
        diagram.put(node);

        Stub rna5 = new Stub(null, "rna5", "molecule-RNA");
        node = new Node(diagram, rna5);
        node.getAttributes().add(new DynamicProperty("rnaType", String.class, "antisence RNA"));
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(230, 290);
        diagram.put(node);

        Stub rna6 = new Stub(null, "rna6", "molecule-RNA");
        node = new Node(diagram, rna6);
        node.getAttributes().add(new DynamicProperty("rnaType", String.class, "generic"));
        regions = new DynamicPropertySetAsMap[1];
        regions[0] = new DynamicPropertySetAsMap();
        regions[0].add(new DynamicProperty("id", String.class, "id1"));
        regions[0].add(new DynamicProperty("name", String.class, "name1"));
        regions[0].add(new DynamicProperty("type", String.class, "exon"));
        regions[0].add(new DynamicProperty("size", Integer.class, 20));
        regions[0].add(new DynamicProperty("position", Integer.class, 0));
        node.getAttributes().add(new DynamicProperty("rnaRegions", DynamicPropertySet[].class, regions));
        node.setLocation(230, 360);
        diagram.put(node);

        Stub rna7 = new Stub(null, "rna7", "molecule-RNA");
        node = new Node(diagram, rna7);
        node.getAttributes().add(new DynamicProperty("rnaType", String.class, "generic"));
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        regions = new DynamicPropertySetAsMap[1];
        regions[0] = new DynamicPropertySetAsMap();
        regions[0].add(new DynamicProperty("id", String.class, "id1"));
        regions[0].add(new DynamicProperty("name", String.class, "name1"));
        regions[0].add(new DynamicProperty("type", String.class, "proteinBindingDomain"));
        regions[0].add(new DynamicProperty("size", Integer.class, 40));
        regions[0].add(new DynamicProperty("position", Integer.class, 10));
        node.getAttributes().add(new DynamicProperty("rnaRegions", DynamicPropertySet[].class, regions));
        node.setLocation(230, 430);
        diagram.put(node);

        Stub rna8 = new Stub(null, "rna8", "molecule-RNA");
        node = new Node(diagram, rna8);
        node.getAttributes().add(new DynamicProperty("rnaType", String.class, "antisence RNA"));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        regions = new DynamicPropertySetAsMap[1];
        regions[0] = new DynamicPropertySetAsMap();
        regions[0].add(new DynamicProperty("id", String.class, "id1"));
        regions[0].add(new DynamicProperty("name", String.class, "name1"));
        regions[0].add(new DynamicProperty("type", String.class, "exon"));
        regions[0].add(new DynamicProperty("size", Integer.class, 20));
        regions[0].add(new DynamicProperty("position", Integer.class, 5));
        node.getAttributes().add(new DynamicProperty("rnaRegions", DynamicPropertySet[].class, regions));
        node.setLocation(230, 500);
        diagram.put(node);

        Stub phenotype1 = new Stub(null, "phenotype1", "semantic-concept-state");
        node = new Node(diagram, phenotype1);
        node.setLocation(340, 10);
        diagram.put(node);

        Stub phenotype2 = new Stub(null, "phenotype2", "semantic-concept-state");
        node = new Node(diagram, phenotype2);
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.setLocation(340, 80);
        diagram.put(node);

        Stub phenotype3 = new Stub(null, "phenotype3", "semantic-concept-state");
        node = new Node(diagram, phenotype3);
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(340, 150);
        diagram.put(node);

        Stub substance1 = new Stub(null, "substance1", "molecule-substance");
        node = new Node(diagram, substance1);
        node.getAttributes().add(new DynamicProperty("moleculeType", String.class, "simple molecule"));
        node.setLocation(450, 10);
        diagram.put(node);

        Stub substance2 = new Stub(null, "C1", "molecule-substance");
        node = new Node(diagram, substance2);
        node.getAttributes().add(new DynamicProperty("moleculeType", String.class, "ion"));
        node.setLocation(450, 80);
        diagram.put(node);

        Stub substance3 = new Stub(null, "substance3", "molecule-substance");
        node = new Node(diagram, substance3);
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.setLocation(450, 150);
        diagram.put(node);

        Stub substance4 = new Stub(null, "C2", "molecule-substance");
        node = new Node(diagram, substance4);
        node.getAttributes().add(new DynamicProperty("moleculeType", String.class, "ion"));
        node.getAttributes().add(new DynamicProperty("hypothetical", Boolean.class, true));
        node.setLocation(450, 220);
        diagram.put(node);

        Stub substance5 = new Stub(null, "substance5", "molecule-substance");
        node = new Node(diagram, substance5);
        node.getAttributes().add(new DynamicProperty("moleculeType", String.class, "simple molecule"));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 3));
        node.setLocation(450, 270);
        diagram.put(node);

        Stub substance6 = new Stub(null, "C3", "molecule-substance");
        node = new Node(diagram, substance6);
        node.getAttributes().add(new DynamicProperty("moleculeType", String.class, "ion"));
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 2));
        node.setLocation(450, 340);
        diagram.put(node);

        Stub degraded1 = new Stub(null, "degraded1", "degraded");
        node = new Node(diagram, degraded1);
        node.setLocation(560, 10);
        diagram.put(node);

        Stub unknown1 = new Stub(null, "unknown1", "unknown");
        node = new Node(diagram, unknown1);
        node.setLocation(610, 10);
        diagram.put(node);

        Stub unknown2 = new Stub(null, "unknown2", "unknown");
        node = new Node(diagram, unknown2);
        node.getAttributes().add(new DynamicProperty("homomultimer", Integer.class, 3));
        node.setLocation(610, 80);
        diagram.put(node);

        Stub compartment1 = new Stub(null, "compartment1", "compartment");
        node = new Node(diagram, compartment1);
        node.getAttributes().add(new DynamicProperty("compartmentShape", Integer.class, 0));
        node.setLocation(710, 10);
        diagram.put(node);

        Stub compartment2 = new Stub(null, "compartment2", "compartment");
        node = new Node(diagram, compartment2);
        node.getAttributes().add(new DynamicProperty("compartmentShape", Integer.class, 1));
        node.setLocation(710, 210);
        diagram.put(node);

        Stub compartment3 = new Stub(null, "compartment3", "compartment");
        node = new Node(diagram, compartment3);
        node.getAttributes().add(new DynamicProperty("compartmentShape", Integer.class, 2));
        node.setLocation(710, 410);
        diagram.put(node);

        Stub complex1 = new Stub(null, "complex1", "complex");
        node = new Node(diagram, complex1);
        node.setLocation(940, 10);
        diagram.put(node);
        
        return diagram;
    }
}