package biouml.plugins.bionetgen._test;

import biouml.model.Diagram;
import biouml.plugins.bionetgen.diagram.BionetgenImporter;
import biouml.workbench.diagram.DiagramTextRepresentation;
import biouml.workbench.diagram.DiagramTextRepresentationFactory;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

public class BionetgenRepresentationFactoryTest extends AbstractBioUMLTest
{
    public void testBasics() throws Exception
    {
        try (TempFile tmp = TempFiles.file( "bionetgen",
                BionetgenRepresentationFactoryTest.class.getResourceAsStream( "test_suite/models/test_examples_2/bionetgen_res_2_1.bngl" ) ))
        {
            CollectionFactory.createRepository( "../data" );
            BionetgenImporter importer = new BionetgenImporter();
            VectorDataCollection<DataElement> root = new VectorDataCollection<>( "test" );
            CollectionFactory.registerRoot( root );
            Diagram dgr = (Diagram)importer.doImport( root, tmp, "test", null, null );
            DiagramTextRepresentation dtr = DiagramTextRepresentationFactory.getDiagramTextRepresentation( dgr );
            assertEquals( "text/x-bionetgen", dtr.getContentType() );
            assertEquals( "begin model\n\nbegin seed species\n    A(b!1).B(a!1)    25.0\nend seed species\n\nend model", dtr.getContent()
                    .trim() );
        }
    }
}
