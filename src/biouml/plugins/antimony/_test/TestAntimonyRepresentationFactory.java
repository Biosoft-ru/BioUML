package biouml.plugins.antimony._test;

import biouml.model.Diagram;
import biouml.plugins.antimony.AntimonyImporter;
import biouml.workbench.diagram.DiagramTextRepresentation;
import biouml.workbench.diagram.DiagramTextRepresentationFactory;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

public class TestAntimonyRepresentationFactory extends AbstractBioUMLTest
{
    public void testBasics() throws Exception
    {
        try (TempFile tmp = TempFiles.file("antimony",
                TestAntimonyRepresentationFactory.class.getResourceAsStream("example_1/antimony_ex1.txt")))
        {
            CollectionFactory.createRepository("../data");
            AntimonyImporter importer = new AntimonyImporter();
            VectorDataCollection<DataElement> root = new VectorDataCollection<>("test");
            CollectionFactory.registerRoot(root);
            Diagram dgr = (Diagram)importer.doImport(root, tmp, "test", null, null);
            DiagramTextRepresentation dtr = DiagramTextRepresentationFactory.getDiagramTextRepresentation(dgr);
            assertEquals("text/x-antimony", dtr.getContentType());
            String expected = "model diagramTest\n   compartment default;\nend";
            assertEquals( expected, dtr.getContent().trim() );
        }
    }
}
