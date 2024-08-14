package biouml.plugins.biopax._test;

import java.io.File;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.plugins.biopax.BioPAXImporter;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class TestBioPaxImporter extends AbstractBioUMLTest
{
    private static final @Nonnull File TEST_FILE = new File("../data/test/biopax/biopax-example-short-pathway.owl");
    
    public void testBioPaxImporter() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        FolderVectorCollection folder = new FolderVectorCollection( "test", null );
        CollectionFactory.registerRoot( folder );
        FunctionJobControl jobControl = new FunctionJobControl( null );
        BioPAXImporter bioPAXImporter = new BioPAXImporter();
        assertTrue( TEST_FILE.exists() );
        int priority = bioPAXImporter.accept( folder, TEST_FILE );
        System.out.println( "PRIORITY: " + bioPAXImporter.accept( folder, TEST_FILE ) );
        assertTrue( priority >= DataElementImporter.ACCEPT_HIGH_PRIORITY );
        DataElement result = bioPAXImporter.doImport( folder, TEST_FILE, "element", jobControl, null );
        FolderCollection result2 = DataElementPath.create("test/element").getDataElement(FolderCollection.class);
        assertSame(result, result2);
        assertEquals(19, DataElementPath.create("test/element/pathway50").getDataElement( Diagram.class ).getSize());
    }
}
