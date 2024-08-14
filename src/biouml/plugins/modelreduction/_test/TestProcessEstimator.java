package biouml.plugins.modelreduction._test;

import biouml.model.Diagram;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestProcessEstimator extends AbstractBioUMLTest
{
    public TestProcessEstimator(String name)
    {
        super( name );
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( TestProcessEstimator.class.getName() );
        suite.addTest( new TestProcessEstimator( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        Diagram d = getDiagram("data/Collaboration/Ilya/Data/Diagrams/CVS models/Fedors Disser/" , "agent" );
        assertNotNull( d );
        DataCollection collection = CollectionFactory.getDataCollection( "data/Collaboration/Ilya/Data/Diagrams/covid19/January 22/" );
        DataElement de = collection.get( "delay_to_fit" );
        assertNotNull( de );
        
        assert(de instanceof Diagram);
        
//        Diagram d = (Diagram)de;
//        
//        double[] quartiles = new double[] {8, 14, 20};
//        ProcessEstimator processEstimator = new ProcessEstimator();
//        processEstimator.fit(d, quartiles);
    }

    private Diagram getDiagram(String path, String name) throws Exception
    {
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection collection = CollectionFactory.getDataCollection( path );
        DataElement de = collection.get( name );
        return (Diagram)de;
    }
}
