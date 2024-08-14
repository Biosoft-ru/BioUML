package biouml.model.util._test;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.util.DiagramPairGraphExporter;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;

/**
 * @author lan
 *
 */
public class TestDiagramPairGraphExporter extends AbstractBioUMLTest
{
    public void testDiagramPairGraphExporterRelation() throws Exception
    {
        Diagram d = new Diagram(null, "test");
        Node n1 = new Node(d, new Stub(null, "n1"));
        d.put(n1);
        Node n2 = new Node(d, new Stub(null, "n2"));
        d.put(n2);
        Node n3 = new Node(d, new Stub(null, "n3"));
        d.put(n3);
        Edge e = new Edge(new SemanticRelation(null, "e1"), n1, n2);
        d.put(e);
        Edge e2 = new Edge(new Stub(null, "e2", "relation"), n1, n3);
        d.put(e2);
        Edge e3 = new Edge(new Stub(null, "e3", "note-link"), n2, n3);
        d.put(e3);
        
        DiagramPairGraphExporter exporter = new DiagramPairGraphExporter();
        assertTrue(exporter.accept(d));
        try(TempFile file = TempFiles.file("test.pair"))
        {
            exporter.doExport(d, file);
            assertTrue(file.exists());
            assertFileContentEquals("n1\tn2\nn1\tn3", file);
        }
    }

    public void testDiagramPairGraphExporterReaction() throws Exception
    {
        Diagram d = new Diagram(null, "test");
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
        
        DiagramPairGraphExporter exporter = new DiagramPairGraphExporter();
        assertTrue(exporter.accept(d));
        try(TempFile file = TempFiles.file("test.pair"))
        {
            exporter.doExport(d, file);
            assertTrue(file.exists());
            assertFileContentEquals("//r1: i1 + i2 -m1-> o1\n"+
                    "i1\tr1\ni2\tr1\nr1\to1\nm1\tr1\n"+
                    "\n"+
                    "//r2: i1 + i2 -> o1\n"+
                    "i1\tr2\ni2\tr2\nr2\to1", file);
        }
    }
}
