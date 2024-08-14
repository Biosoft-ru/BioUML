package biouml.plugins.keynodes._test;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.keynodes.ShortestPathClustering;
import biouml.plugins.keynodes.ShortestPathClusteringParameters;
import biouml.standard.type.Reaction;
import junit.framework.Test;
import junit.framework.TestSuite;

public class ShortestPathClusteringTest extends AnalysisTest
{

    public ShortestPathClusteringTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( ShortestPathClusteringTest.class.getName() );
        suite.addTest( new ShortestPathClusteringTest( "testShortestPathClusteringFullPath" ) );
        suite.addTest( new ShortestPathClusteringTest( "testShortestPathClustering" ) );
        return suite;
    }

    public void testShortestPathClusteringFullPath() throws Exception
    {
        ShortestPathClustering spc = initShortestPathClustering( true );
        table.remove( "E01" );
        table.remove( "E07" );

        ru.biosoft.access.core.DataElement[] results = spc.justAnalyzeAndPut();
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
        assertArrayEquals( "Hits 1", new String[] {"E03", "E04", "E05"}, ( (StringSet)rde.getValue( "Hits" ) ).toArray() );

        rde = summary.get( "2" );
        assertNotNull( rde );
        assertEquals( 2, rde.getValue( "Size" ) );
        assertArrayEquals( "Hits 2", new String[] {"E11", "E14"}, ( (StringSet)rde.getValue( "Hits" ) ).toArray() );

        elem = collection.get( "Cluster 1" );
        assertTrue( elem instanceof Diagram );
        Diagram d = (Diagram)elem;
        assertEquals( 9, d.getSize() );
        assertEquals( 5, d.getNodes().length );

        assertEquals( "Predefined styles number", 6, d.getViewOptions().getStyles().length );
        Node n = d.findNode( "E04" );
        assertNotNull( "Cannot find node 'E04'", n );
        assertEquals( "Incorrect style", "highlight1", n.getPredefinedStyle() );

        Map<String, String> relations = new HashMap<>();
        relations.put( "E03", TestsHub.REACTANT );
        relations.put( "E04", TestsHub.PRODUCT );
        checkReactionNode( d, "X06", relations );

        relations = new HashMap<>();
        relations.put( "E04", TestsHub.REACTANT );
        relations.put( "E05", TestsHub.PRODUCT );
        checkReactionNode( d, "X08", relations );

        elem = collection.get( "Cluster 2" );
        assertTrue( elem instanceof Diagram );
        d = (Diagram)elem;
        assertEquals( 7, d.getSize() );
        assertEquals( 4, d.getNodes().length );

        // Modifiers can be intermediate elements in this test hub.
        // Thus, we have 4 hub-edges here: E11->X11, X11->E13, E13->X11, X11->E14,
        // but X11->E13 and E13->X11 corresponds to one edge on the diagram
        relations = new HashMap<>();
        relations.put( "E11", TestsHub.REACTANT );
        relations.put( "E13", TestsHub.MODIFIER );
        relations.put( "E14", TestsHub.PRODUCT );
        checkReactionNode( d, "X11", relations );
    }

    public void testShortestPathClustering() throws Exception
    {
        ShortestPathClustering spc = initShortestPathClustering( false );
        table.remove( "E04" );
        table.remove( "E05" );

        ru.biosoft.access.core.DataElement[] results = spc.justAnalyzeAndPut();
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
        assertEquals( 5, d.getSize() );
        assertEquals( 3, d.getNodes().length );
        assertEquals( 0, d.stream( Node.class ).filter( n -> n.getKernel() instanceof Reaction ).count() );
        assertTrue( d.stream( Edge.class ).map( e -> e.getTitle() ).allMatch( "2"::equals ) );

        elem = collection.get( "Cluster 2" );
        assertTrue( elem instanceof Diagram );
        d = (Diagram)elem;
        assertEquals( 3, d.getSize() );
        assertEquals( 2, d.getNodes().length );
        assertEquals( 0, d.stream( Node.class ).filter( n -> n.getKernel() instanceof Reaction ).count() );
        assertTrue( d.stream( Edge.class ).map( e -> e.getTitle() ).allMatch( "1"::equals ) );
    }

    protected ShortestPathClustering initShortestPathClustering(boolean useFullPath)
    {
        ShortestPathClustering analysis = AnalysisMethodRegistry.getAnalysisMethod( ShortestPathClustering.class );
        ShortestPathClusteringParameters parameters = analysis.getParameters();
        parameters.setBioHub( bioHubInfo );
        parameters.setSourcePath( table.getCompletePath() );
        parameters.setSpecies( species );
        parameters.setUseFullPath( useFullPath );
        parameters.setMaxRadius( 2 );
        analysis.validateParameters();
        return analysis;
    }

}
