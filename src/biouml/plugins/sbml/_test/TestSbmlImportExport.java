package biouml.plugins.sbml._test;

import java.io.File;

import biouml.model.Diagram;
import biouml.plugins.sbml.SbmlExporter;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestSbmlImportExport extends AbstractBioUMLTest
{

    public static final String OUTPUT_COLLECTION = "data/SBML/Diagarms";
    public TestSbmlImportExport(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( TestSbmlImportExport.class.getName() );
        suite.addTest( new TestSbmlImportExport( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        String repositoryPath = "../data/test/SBML";
        String diagramName = "SBGNmodel";
        DataCollection<?> repository = CollectionFactory.createRepository( repositoryPath );
        DataCollection<?> diagarmCol = (DataCollection<?>)repository.get( "Diagrams" );
        DataElement de = diagarmCol.get( diagramName );
        
        assertNotNull( de );
        assert( de instanceof Diagram );
        
        File f = new File("biouml/plugins/sbml/_test/SBGNExported");
        SbmlExporter exporter = new SbmlExporter();
        exporter.doExport( de, f );
        
        assert(f.exists());
    }
}
