package biouml.plugins.pharm._test;

import java.io.File;
import java.util.Properties;

import biouml.plugins.pharm.nlme.StubModel;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ExProperties;

public class StubModelTest extends AbstractBioUMLTest
{
    public StubModelTest(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( StubModelTest.class.getName() );
        suite.addTest( new StubModelTest( "test" ) );
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {

        TableDataCollection tdc = getTable();

        double[] times = TableDataCollectionUtils.getColumn( tdc, "Time" );
        double[] subject = TableDataCollectionUtils.getColumn( tdc, "Subject" );
        double[] doses = TableDataCollectionUtils.getColumn( tdc, "Dose" );
        int[] subj = DoubleStreamEx.of(subject).mapToInt(x -> (int)x).toArray();

        double[] ka = createArray( 0.5, 132 );
        double[] ke = createArray( -2.5, 132 );
        double[] cl = createArray( -3.2, 132 );

        StubModel model = new StubModel( subj, doses, 1E-8, 1E-8 );
        double[] result = model.calc( ka, ke, cl, times, subj );
        assert ( result.length == testResult.length );
        for( int i = 0; i < result.length; i++ )
        {
//            assertTrue( Math.abs( result[i] - testResult[i] ) < ERROR );
                        System.out.println( result[i] + "\t"+ Math.abs( result[i] - testResult[i] ));

        }
    }

    private static final String TEST_RPOSITORY_PATH = "../data/test/biouml/plugins/Pharm/data/";
    private static final String THEOPH_DATA_NAME = "theophdata";
    
    public TableDataCollection getTable() throws Exception
    {
        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository( TEST_RPOSITORY_PATH );
        DataCollection collection1 = CollectionFactory.getDataCollection( "data/Data" );
        DataElement de = collection1.get( THEOPH_DATA_NAME );
        return (TableDataCollection)de;
    }

    private DataCollection createRepository(String path) throws Exception
    {
        File f = new File( path, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE );
        Properties propRepository = new ExProperties( f );
        return CollectionFactory.createCollection( null, propRepository );
    }

    public static double[] createArray(double value, int size)
    {
        double[] result = new double[size];
        for( int i = 0; i < size; i++ )
        {
            result[i] = value;
        }
        return result;
    }

    static double ERROR = 1E-7;

    static double[] testResult = new double[] {0.0000000, 2.7047788, 4.8013202, 6.4269535, 6.9129117, 6.2106519, 5.6034285, 4.7840002,
            4.0530965, 3.1502471, 1.1525071, 0.0000000, 3.1457627, 4.9787031, 6.7968024, 7.5716856, 6.9671878, 6.1732338, 5.2362192,
            4.4544695, 3.4821633, 1.2687197, 0.0000000, 3.2387057, 5.4642621, 7.0429912, 7.7899229, 7.1078075, 6.3246177, 5.3732595,
            4.5860789, 3.5411743, 1.3202178, 0.0000000, 3.8243461, 5.4091120, 6.9430353, 7.5506965, 6.9671878, 6.1732338, 5.2405177,
            4.4471627, 3.4878846, 1.2327883, 0.0000000, 4.5437051, 6.6307273, 9.0521050, 10.0770305, 9.2790274, 8.2216250, 6.9794168,
            5.8840462, 4.6376084, 1.6827831, 0.0000000, 2.8597843, 4.8249555, 6.4405056, 6.8775858, 6.3002474, 5.6211828, 4.7719321,
            3.9770460, 3.1397245, 1.1967821, 0.0000000, 3.3305112, 5.4683422, 7.6959838, 8.5121674, 7.8499218, 6.9562137, 5.9149654,
            5.0112782, 3.9013885, 1.4367133, 0.0000000, 3.0479224, 5.1258012, 6.9502591, 7.7899229, 7.1567653, 6.3401049, 5.3380997,
            4.5598034, 3.5557380, 1.3256474, 0.0000000, 2.4036665, 3.9134800, 4.8638351, 5.3308523, 4.8975656, 4.3493238, 3.6470135,
            3.1903233, 2.5352324, 0.8843828, 0.0000000, 4.9741658, 7.6670876, 8.5510931, 9.4538012, 8.6760412, 7.6976991, 6.5184737,
            5.3970881, 4.3171212, 1.6659621, 0.0000000, 3.3103263, 5.4352007, 7.5486258, 8.4641989, 7.7315714, 6.9027978, 5.8550451,
            4.9686564, 3.8555263, 1.4445111, 0.0000000, 3.5660019, 5.8549927, 8.1870574, 9.1162008, 8.3796066, 7.4056987, 6.2865950,
            5.3524144, 4.1772443, 1.5471636};

}
