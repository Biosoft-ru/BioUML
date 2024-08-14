package biouml.plugins.pharm._test;

import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;

public class Copy extends AbstractBioUMLTest
{
    public Copy(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( Copy.class.getName() );
        suite.addTest( new Copy( "test" ) );
        return suite;
    }


    private static final DataElementPath DATA_PATH = DataElementPath.create("data/Collaboration/pharm/Data/Tables");

    private static final String destination = "../data/test/biouml/plugins/Pharm/data/";
    private static final String THEOPH_DATA = "theophdata";

    public void test() throws Exception
    {
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection collection = DATA_PATH.getDataCollection();
        TableDataCollection tdc = collection.get( THEOPH_DATA ).cast( TableDataCollection.class );

        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository( destination );
        collection = CollectionFactory.getDataCollection( "data/Data" );
        collection.put( tdc.clone( collection, tdc.getName() ) );
    }

    public void get() throws Exception
    {
        //        CollectionFactory.createRepository( "../data_resources" );
        //        DataCollection collection = CollectionFactory.getDataCollection( dataPath );
        //        DataElement de = collection.get( "theophdata" );

        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository( destination );
        DataCollection<?> collection = CollectionFactory.getDataCollection( "data/Data" );
        DataElement de = collection.get( THEOPH_DATA );
        assert ( de != null );
        assert ( de instanceof TableDataCollection );
    }
}
