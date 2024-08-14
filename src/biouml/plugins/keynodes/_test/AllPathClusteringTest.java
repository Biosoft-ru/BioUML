package biouml.plugins.keynodes._test;

import java.util.HashMap;
import java.util.Map;

import biouml.model.Diagram;
import biouml.plugins.keynodes.AllPathClustering;
import biouml.plugins.keynodes.DirectionEditor;
import biouml.plugins.keynodes.AllPathClustering.AllPathClusteringParameters;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;

public class AllPathClusteringTest extends ShortestPathClusteringTest
{
    public AllPathClusteringTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( AllPathClusteringTest.class.getName() );
        suite.addTest( new AllPathClusteringTest( "testAllPathClustering" ) );
        return suite;
    }

    public void testAllPathClustering() throws Exception
    {
        AllPathClustering apc = initAllPathClustering();
        table.remove( "E04" );
        table.remove( "E05" );

        ru.biosoft.access.core.DataElement[] results = apc.justAnalyzeAndPut();
        assertNotNull( results );
        assertEquals( 3, results.length );
        assertTrue( results[0] instanceof DataCollection );
        DataCollection<?> collection = (DataCollection<?>)results[0];
        DataElement elem = collection.get( "Clusters" );
        assertTrue( elem instanceof TableDataCollection );
        TableDataCollection summary = (TableDataCollection)elem;
        assertEquals( 2, summary.getSize() );

        RowDataElement rde = summary.get( "1" );
        assertNotNull( rde );
        assertEquals( 3, rde.getValue( "Size" ) );
        assertArrayEquals( "Hits 1", new String[] {"E07", "E11", "E14"}, ( (StringSet)rde.getValue( "Hits" ) ).toArray() );

        rde = summary.get( "2" );
        assertNotNull( rde );
        assertEquals( 2, rde.getValue( "Size" ) );
        assertArrayEquals( "Hits 2", new String[] {"E01", "E03"}, ( (StringSet)rde.getValue( "Hits" ) ).toArray() );

        elem = collection.get( "Cluster 1" );
        assertTrue( elem instanceof Diagram );
        Diagram d = (Diagram)elem;
        assertEquals( 20, d.getSize() );
        assertEquals( 10, d.getNodes().length );

        Map<String, String> relations = new HashMap<>();
        relations.put( "E11", TestsHub.REACTANT );
        relations.put( "E12", TestsHub.REACTANT );
        relations.put( "E13", TestsHub.MODIFIER );
        relations.put( "E14", TestsHub.PRODUCT );
        checkReactionNode( d, "X11", relations );

        elem = collection.get( "Cluster 2" );
        assertTrue( elem instanceof Diagram );
        d = (Diagram)elem;
        assertEquals( 19, d.getSize() );
        assertEquals( 9, d.getNodes().length );

        relations = new HashMap<>();
        relations.put( "E01", TestsHub.REACTANT );
        relations.put( "E03", TestsHub.PRODUCT );
        checkReactionNode( d, "X02", relations );

        relations = new HashMap<>();
        relations.put( "E01", TestsHub.REACTANT );
        relations.put( "E02", TestsHub.PRODUCT );
        checkReactionNode( d, "X01", relations );

        relations = new HashMap<>();
        relations.put( "E02", TestsHub.REACTANT );
        relations.put( "E03", TestsHub.PRODUCT );
        checkReactionNode( d, "X04", relations );

        relations = new HashMap<>();
        relations.put( "E03", TestsHub.REACTANT );
        relations.put( "E06", TestsHub.PRODUCT );
        checkReactionNode( d, "X07", relations );
        relations = new HashMap<>();

        relations.put( "E06", TestsHub.PRODUCT );
        relations.put( "E01", TestsHub.REACTANT );
        checkReactionNode( d, "X03", relations );
    }

    protected AllPathClustering initAllPathClustering()
    {
        AllPathClustering analysis = AnalysisMethodRegistry.getAnalysisMethod( AllPathClustering.class );
        AllPathClusteringParameters parameters = (AllPathClusteringParameters)analysis.getParameters();
        parameters.setDirection( DirectionEditor.BOTH );
        parameters.setBioHub( bioHubInfo );
        parameters.setSourcePath( table.getCompletePath() );
        parameters.setSpecies( species );
        parameters.setMaxRadius( 4 );
        analysis.validateParameters();
        return analysis;
    }
}
