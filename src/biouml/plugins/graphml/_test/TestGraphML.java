package biouml.plugins.graphml._test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.graphics.Brush;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementStyle;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.graphml.GraphMLExporter;
import biouml.plugins.graphml.GraphMLExporter.GraphMLExporterProperties;
import biouml.plugins.graphml.GraphMLWriter;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

public class TestGraphML extends AbstractBioUMLTest
{
    public TestGraphML(String name)
    {
        super(name);
    }

    public void testWriter() throws Exception
    {
        Diagram diagram = new Diagram(null, new Stub(null, "test"), new PathwaySimulationDiagramType());
        Node note = new Node(diagram, new Stub.Note(null, "note_node"));
        note.setShapeSize(new Dimension(50, 30));
        note.setLocation(10, 50);
        diagram.put(note);
        Node substance1 = new Node(diagram, new Stub(null, "subst_node", Type.TYPE_MOLECULE));
        substance1.setShapeSize(new Dimension(60, 30));
        substance1.setLocation(70, 10);
        diagram.put(substance1);
        Node substance2 = new Node(diagram, new Stub(null, "subst2_node", Type.TYPE_MOLECULE));
        substance2.setShapeSize(new Dimension(60, 30));
        substance2.setLocation(70,90);
        diagram.put(substance2);
        Node reactionNode = new Node(diagram, new Stub(null, "reaction_node", Type.TYPE_REACTION));
        reactionNode.setShapeSize(new Dimension(10, 10));
        reactionNode.setLocation(80, 50);
        diagram.put(reactionNode);
        Edge e1 = new Edge(diagram, new SpecieReference(null, "edge_reactant", SpecieReference.REACTANT), substance1, reactionNode);
        diagram.put(e1);
        Edge e2 = new Edge(diagram, new SpecieReference(null, "edge_product", SpecieReference.PRODUCT), reactionNode, substance2);
        diagram.put(e2);
        Edge e3 = new Edge(diagram, new Stub.NoteLink(null, "edge_notelink"), note, reactionNode);
        diagram.put(e3);

        String fileName = "../data/test/graphml/out.graphml";
        File file = new File(fileName);
        if( file.exists() )
            file.delete();
        try (OutputStream os = new FileOutputStream( file ))
        {
            new GraphMLWriter().writeGraph( diagram, os, true );
        }

        assertTrue("GraphML model file was not created", file.exists());
        
        File origFile = new File("../data/test/graphml/in.graphml");
        assertFileEquals(origFile, file);
        file.delete();
    }
    
    public void testExporter() throws Exception
    {
        Diagram d = new Diagram(null, "test");
        d.setType(new PathwayDiagramType());
        Compartment c = new Compartment(d, "cell", new Stub(null, "cell", Type.TYPE_COMPARTMENT));
        c.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
        c.getCustomStyle().setBrush(new Brush(Color.RED));
        d.put(c);
        Compartment c2 = new Compartment(d, "cell2", new Stub(null, "cell2", Type.TYPE_COMPARTMENT));
        d.put(c2);
        Compartment c3 = new Compartment(d, "cell3", new Stub(null, "cell3", Type.TYPE_COMPARTMENT));
        c3.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
        c3.getCustomStyle().setBrush(new Brush(new GradientPaint(0, 0, Color.BLUE, 1, 1, Color.RED)));
        d.put(c3);
        Reaction r1 = new Reaction(null, "r1");
        SpecieReference i1 = new SpecieReference(null, "i1", SpecieReference.REACTANT);
        SpecieReference i2 = new SpecieReference(null, "i2", SpecieReference.REACTANT);
        SpecieReference o1 = new SpecieReference(null, "o1", SpecieReference.PRODUCT);
        SpecieReference m1 = new SpecieReference(null, "m1", SpecieReference.MODIFIER);
        r1.setSpecieReferences(new SpecieReference[] {i1, i2, o1, m1});
        Node nr1 = new Node(d, r1);
        d.put(nr1);

        Reaction r2 = new Reaction(null, "r2");
        r2.setSpecieReferences(new SpecieReference[] {i1, i2, o1});
        Node nr2 = new Node(d, new Stub(null, "r2", "reaction"));
        d.put(nr2);

        Node ni1 = new Node(d, new Stub(null, "i1"));
        d.put(ni1);
        Node ni2 = new Node(d, new Stub(null, "i2"));
        d.put(ni2);
        Node no1 = new Node(d, new Stub(null, "o1"));
        d.put(no1);
        Node nm1 = new Node(d, new Stub(null, "m1"));
        d.put(nm1);
        
        Edge ei1 = new Edge(d, new Stub(null, "i1", "consumption"), ni1, nr1);
        d.put(ei1);
        Edge ei2 = new Edge(d, new Stub(null, "i2", "consumption"), ni2, nr1);
        d.put(ei2);
        Edge em1 = new Edge(d, new Stub(null, "m1", "regulation"), nm1, nr1);
        d.put(em1);
        Edge eo1 = new Edge(d, new Stub(null, "o1", "production"), nr1, no1);
        d.put(eo1);
        
        Edge ei11 = new Edge(d, i1, ni1, nr2);
        d.put(ei11);
        Edge ei21 = new Edge(d, i2, ni2, nr2);
        d.put(ei21);
        Edge eo11 = new Edge(d, o1, nr2, no1);
        d.put(eo11);
        
        GraphMLExporter exporter = new GraphMLExporter();
        assertTrue(exporter.accept(d));
        try(TempFile file = TempFiles.file("test.graphml"))
        {
            ((GraphMLExporterProperties)exporter.getProperties(d, file)).setUseYSchema(true);
            exporter.doExport(d, file);
            assertTrue(file.exists());
            assertFileEquals(new File("../data/test/graphml/test_export.graphml"), file);
        }
    }
}
