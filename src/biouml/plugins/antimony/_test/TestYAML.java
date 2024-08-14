package biouml.plugins.antimony._test;

import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Simple test just to check that yaml file with SBGN specification is available
 * @author Damag
 *
 */
public class TestYAML extends AbstractBioUMLTest
{

    public TestYAML(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( TestYAML.class.getName() );
        suite.addTest( new TestYAML( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        DataCollection<?> repository = CollectionFactory.createRepository( "../data/test/biouml/plugins/antimony" );
        assertNotNull( repository );
        CollectionFactory.registerRoot( repository );
        DataCollection antimony = CollectionFactory.getDataCollection( "antimony" );
        assertNotNull( antimony );
        DataCollection yaml = CollectionFactory.getDataCollection( "antimony/yaml" );
        assertNotNull( yaml );
        DataElement de = CollectionFactory.getDataElement( "antimony/yaml/sbgn.yaml" );
        assertNotNull( de );
    }
}