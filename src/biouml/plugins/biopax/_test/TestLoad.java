package biouml.plugins.biopax._test;

import java.io.File;
import java.net.URI;

import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyURIMapper;
import org.semanticweb.owl.util.SimpleURIMapper;

import biouml.plugins.biopax.reader.BioPAXReader;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;

/**
 * @author axec
 *
 */
public class TestLoad extends AbstractBioUMLTest
{
    public static final String dcPath = "../data/test/biopax/";
    public TestLoad(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( TestLoad.class.getName() );
        suite.addTest( new TestLoad( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        String fileName = dcPath + "/biopax-example-short-pathway.owl";
        File file = new File(fileName);
        assertTrue( file.exists() );
        OWLOntologyManager manager = BioPAXReader.getOWLOntologyManager();
        OWLOntologyURIMapper mapper = new SimpleURIMapper( URI.create( "http://www.biopax.org/release/biopax-level2.owl" ),
                new File( dcPath + "/biopax-level2.owl" ).toURI() );
        manager.addURIMapper( mapper );
        System.out.println( "Try to load file " + file.toURI() );

        OWLOntology ontology = null;
        try
        {
            ontology = manager.loadOntologyFromPhysicalURI( file.toURI() );
        }
        catch( OWLOntologyCreationException ex )
        {
            ex.printStackTrace();
        }
        assertNotNull( ontology );
    }
}
