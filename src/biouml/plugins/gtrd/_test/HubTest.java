package biouml.plugins.gtrd._test;

import java.util.Map;
import java.util.Properties;

import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;
import biouml.plugins.gtrd.GTRDHub;
import biouml.plugins.gtrd.IsoformGTRDType;
import biouml.plugins.gtrd.ProteinGTRDType;
import biouml.plugins.gtrd.SiteModelGTRDType;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

/**
 *  Testing of {@link GTRDHub}
 */
public class HubTest extends AbstractBioUMLTest
{
    static final String dataDirectory = "../data/";

    /** Standard JUnit constructor */
    public HubTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(HubTest.class.getName());
        suite.addTest(new HubTest("testHub"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test case
    //

    public void testHub() throws Exception
    {
        CollectionFactory.createRepository( dataDirectory );
        DataCollection<?> root = CollectionFactory.createRepository( "../data/test/biouml/plugins/gtrd" );
        assertNotNull( "Root init failed", root );
        BioHubRegistry.addCollectionHub( root );
        assertNotNull( BioHubRegistry.getBioHub( "GTRD" ) );

        Properties input = BioHubSupport.createProperties( "Homo sapiens", ReferenceTypeRegistry.getReferenceType(ProteinGTRDType.class) );
        Properties output = BioHubSupport.createProperties( "Homo sapiens", ReferenceTypeRegistry.getReferenceType(UniprotProteinTableType.class) );
        Map<String, String[]> result = BioHubRegistry.getReferences(new String[] {"1.1.1.1.1"}, input, output, null);
        assertNotNull( "No matching possible", result );
        String[] elements = result.get("1.1.1.1.1");
        assertTrue("Incorrent uniprot ID count", elements.length == 1);
        assertEquals("Incorrect uniprot ID value", elements[0], "P05412");

        output = BioHubSupport.createProperties( "Homo sapiens", ReferenceTypeRegistry.getReferenceType(IsoformGTRDType.class) );
        result = BioHubRegistry.getReferences(new String[] {"1.1.1.2.2"}, input, output, null);
        elements = result.get("1.1.1.2.2");
        assertTrue("Incorrent isoform count", elements.length == 2);
        
        output = BioHubSupport.createProperties( "Homo sapiens", ReferenceTypeRegistry.getReferenceType(SiteModelGTRDType.class) );
        result = BioHubRegistry.getReferences(new String[] {"1.1.1.1.1"}, input, output, null);
        elements = result.get("1.1.1.1.1");
        assertTrue("Incorrent site model count", elements.length == 1);
        
        input = BioHubSupport.createProperties( "Homo sapiens", ReferenceTypeRegistry.getReferenceType(UniprotProteinTableType.class) );
        output = BioHubSupport.createProperties( "Homo sapiens", ReferenceTypeRegistry.getReferenceType(ProteinGTRDType.class) );
        result = BioHubRegistry.getReferences( new String[] {"Q06413"}, input, output, null );
        elements = result.get( "Q06413" );
        assertEquals( 1, elements.length );
        assertEquals( "5.1.1.1.3", elements[0] );
        
    }
}
