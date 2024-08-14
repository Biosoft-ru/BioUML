package biouml.plugins.pharm._test;

import java.util.HashMap;

import biouml.model.Diagram;
import biouml.plugins.pharm.nlme.MixedEffectModel;
import biouml.plugins.pharm.nlme.MixedEffectModelRunner;
import biouml.plugins.pharm.nlme.PopulationModelSimulationEngine;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class PopulationSimulationEngineTest extends AbstractBioUMLTest
{
    public PopulationSimulationEngineTest(String name)
    {
        super( name );
    }

    private static final String TEST_RPOSITORY_PATH = "../data/test/biouml/plugins/Pharm/data/";
    private static final String THEOPH_DATA_NAME = "theophdata";

    private static final String DIAGRAM_PATH = "databases/PharmTest/Diagrams";
    private static final String DIAGRAM_NAME = "test";

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( PopulationSimulationEngineTest.class.getName() );
        suite.addTest( new PopulationSimulationEngineTest( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        PopulationModelSimulationEngine engine = new PopulationModelSimulationEngine();
        engine.setDiagram( getDiagram() );
        //        engine.setOutputDir( "../out" );
        MixedEffectModel model = engine.createModel();
        simulate( model );
    }

    public Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection collection = CollectionFactory.getDataCollection( DIAGRAM_PATH );
        DataElement de = collection.get( DIAGRAM_NAME );
        return (Diagram)de;
    }

    public TableDataCollection getTable() throws Exception
    {
        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository( TEST_RPOSITORY_PATH );
        DataCollection collection1 = CollectionFactory.getDataCollection( "data/Data" );
        DataElement de = collection1.get( THEOPH_DATA_NAME );
        return (TableDataCollection)de;
    }

    public void simulate(MixedEffectModel model) throws Exception
    {
        TableDataCollection tdc = getTable();

        double[] times = TableDataCollectionUtils.getColumn( tdc, "Time" );
        double[] subject = TableDataCollectionUtils.getColumn( tdc, "Subject" );

        int[] subj = DoubleStreamEx.of(subject).mapToInt(x -> (int)x).toArray();

        double[] ka = StubModelTest.createArray( 0.5, 132 );
        double[] ke = StubModelTest.createArray( -2.5, 132 );
        double[] cl = StubModelTest.createArray( -3.2, 132 );

        HashMap<String, double[]> params = new HashMap<>();
        params.put( "ka", ka );
        params.put( "ke", ke );
        params.put( "CL", cl );

        MixedEffectModelRunner runner = new MixedEffectModelRunner();
        runner.setMixedEffectModel( model );
        double[] result = runner.calculate( params, times, subj );

        assert ( result.length == StubModelTest.testResult.length );
        for( int i = 0; i < result.length; i++ )
        {
                        assertTrue( Math.abs( result[i] - StubModelTest.testResult[i] ) < 1E-7 );
//            System.out
//                    .println( result[i] + "__" + StubModelTest.testResult[i] + "__" + Math.abs( result[i] - StubModelTest.testResult[i] ) );
        }
    }
}
