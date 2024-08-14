package biouml.plugins.biopax._test;

import biouml.plugins.biopax.BioPAXTextModuleType;
import ru.biosoft.access.Repository;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class TestCollectionsGenerator extends AbstractBioUMLTest
{
    private static final String TEST_FILE_3 = "../data/test/biopax/biopax3-short-metabolic-pathway.owl";
    private static final String TEST_FILE_2 = "../data/test/biopax/biopax-example-short-pathway.owl";

    public static void main(String ... args) throws Exception
    {
        generateCollection( TEST_FILE_2, "TestCollectionLevel2" );
        generateCollection( TEST_FILE_3, "TestCollectionLevel3" );
    }

    public static void generateCollection(String owlFile, String name) throws Exception
    {
        DataCollection<?> repository = CollectionFactory.createRepository( "../data/test/biopax" );
        BioPAXTextModuleType moduleType = new BioPAXTextModuleType();
        moduleType.setFileNames( new String[] {owlFile} );
        moduleType.setJobControl( new FunctionJobControl( null ) );
        moduleType.createModule( (Repository)repository, name );
    }
}