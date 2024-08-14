package biouml.plugins.agentmodeling.covid19._test;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.covid19.ProcessEstimator;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access._test.AbstractBioUMLTest;

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
        String repositoryPath = "../data_resources";
        CollectionFactory.createRepository(repositoryPath);
                
        DataCollection collection = CollectionFactory.getDataCollection( "data/Collaboration/Ilya/Data/Diagrams/covid19/January 22/" );
        DataElement de = collection.get( "delay_to_fit" );
        assertNotNull( de );

        assert(de instanceof Diagram);
        
        Diagram d = (Diagram)de;
        
        double[] quartiles = new double[] {8, 14, 20};
        ProcessEstimator processEstimator = new ProcessEstimator();
//        processEstimator.fit(d, quartiles);
    }


}
