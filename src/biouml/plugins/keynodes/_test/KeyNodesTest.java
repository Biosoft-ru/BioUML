package biouml.plugins.keynodes._test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import biouml.model.Module;
import biouml.plugins.keynodes.KeyNodeAnalysis;
import biouml.plugins.keynodes.KeyNodeAnalysis.KeyNodeStats;
import biouml.plugins.keynodes.KeyNodeAnalysisParameters;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.ApplyContextDecorator;
import biouml.plugins.keynodes.graph.ElementConverter;
import biouml.plugins.keynodes.graph.GraphUtils;
import biouml.plugins.keynodes.graph.HubGraph;
import biouml.plugins.keynodes.graph.MemoryHubGraph;
import biouml.plugins.keynodes.graph.MemoryHubGraph.HubRelation;
import biouml.plugins.keynodes.graph.UserCollectionBioHub;
import biouml.plugins.keynodes.graph.UserHubEdge;
import biouml.plugins.server.access.ClientModule;
import biouml.standard.type.Species;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectFloatMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class KeyNodesTest extends AbstractBioUMLTest
{
    private DataCollection<?> repository = null;
    private Module module = null;
    private KeyNodesHub<?> bioHub;
    private TargetOptions dbOptions;
    private BioHubInfo bioHubInfo;


    public KeyNodesTest(String name)
    {
        super( name );
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run( KeyNodesTest.class );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( KeyNodesTest.class.getName() );
        suite.addTest( new KeyNodesTest( "testReachableNodesWithDistance" ) );
        suite.addTest( new KeyNodesTest( "testDirectedReferences" ) );
        suite.addTest( new KeyNodesTest( "testMinimalPaths" ) );
        suite.addTest( new KeyNodesTest( "testKeyNodes" ) );
        suite.addTest( new KeyNodesTest( "testContextAlgorithmNew" ) );
        //requires transpath
        //suite.addTest( new KeyNodesTest( "testBatchScore" ) );
        return suite;
    }

    public void testReachableNodesWithDistance()
    {
        MemoryHubGraph<Integer> hub = createHub();
        TObjectFloatMap<Integer> nodes = GraphUtils.getReachableNodesWithDistance( hub, 4, 3, false );
        assertEquals( 1, nodes.size() );
        assertTrue( nodes.containsKey( 4 ) );
        assertEquals( 0.0f, nodes.get( 4 ), 0.0f );

        nodes = GraphUtils.getReachableNodesWithDistance( hub, 4, 3, true );
        assertEquals( 3, nodes.size() );
        assertEquals( 1.8f, nodes.get( 1 ), 0.000001f );
        assertEquals( 0.8f, nodes.get( 2 ), 0.000001f );
        assertEquals( 0.0f, nodes.get( 4 ), 0.000001f );

        nodes = GraphUtils.getReachableNodesWithDistance( hub, 1, 3, false );
        assertEquals( 4, nodes.size() );
        assertEquals( 0.0f, nodes.get( 1 ), 0.000001f );
        assertEquals( 1.0f, nodes.get( 2 ), 0.000001f );
        assertEquals( 1.2f, nodes.get( 3 ), 0.000001f );
        assertEquals( 1.8f, nodes.get( 4 ), 0.000001f );

        nodes = GraphUtils.getReachableNodesWithDistance( hub, 1, 1.5f, false );
        assertEquals( 3, nodes.size() );
        assertEquals( 0.0f, nodes.get( 1 ), 0.000001f );
        assertEquals( 1.0f, nodes.get( 2 ), 0.000001f );
        assertEquals( 1.2f, nodes.get( 3 ), 0.000001f );
    }

    public void testDirectedReferences()
    {
        MemoryHubGraph<Integer> hub = createHub();
        assertEquals( "2, 3, 4",
                StreamEx.of( GraphUtils.getDirectedReferences( hub, new Element( "1" ), 2, BioHub.DIRECTION_DOWN, createConverter() ) )
                        .map( Element::getAccession ).sorted().joining( ", " ) );
    }
    
    public void testMinimalPaths()
    {
        MemoryHubGraph<Integer> hub = createHub();
        assertEquals("[4-[2->4]->2-[1->2]->1]", GraphUtils.getMinimalPaths( hub, 1, Collections.singleton( 4 ), 3, false ).toString());
    }

    protected ElementConverter<Integer> createConverter()
    {
        return ElementConverter.of( e -> Integer.valueOf( e.getAccession() ), n -> new Element( n.toString() ) );
    }

    protected MemoryHubGraph<Integer> createHub()
    {
        MemoryHubGraph<Integer> hub = StreamEx.of( new HubRelation<>( 1, 2, new UserHubEdge( "1->2" ), 1.0f ),
                new HubRelation<>( 1, 3, new UserHubEdge( "1->3" ), 1.2f ), new HubRelation<>( 1, 4, new UserHubEdge( "1->4" ), 2.5f ),
                new HubRelation<>( 2, 4, new UserHubEdge( "2->4" ), 0.8f ) ).collect( MemoryHubGraph.toMemoryHub() );
        return hub;
    }

    public void testDijkstraSearch() throws Exception
    {
        Element[] elems = new Element[] {new Element( "MO000090944" )};

        int dist = 4;
        String[] relTypes = new String[] {Species.getDefaultSpecies( null ).getLatinName()};
        DijkstraSearch search = new DijkstraSearch( bioHub, dbOptions, relTypes );
        search.setMaxRadius( dist );
        search.setDirection( BioHub.DIRECTION_UP );
        for( Element elem : elems )
        {
            search.shortestPath( elem );
        }
        //Get key nodes connected with all input nodes
        Map<String, Integer> res = search.getResult( elems.length );

        int size1 = 0;
        if( res != null && res.size() > 0 )
        {
            size1 = res.size();
            System.out.println( size1 + " upstream nodes connected with all targets" );
            for( Map.Entry<String, Integer> entry : res.entrySet() )
            {
                System.out.println( entry.getKey() + " " + entry.getValue() );
            }
        }
        else
        {
            System.out.println( "No common key node" );
        }
        //Get key nodes connected with all input nodes
        res = search.getResult( 0 );
        int size2 = 0;
        if( res != null && res.size() > 0 )
        {
            size2 = res.size();
            System.out.println( size2 + " upstream nodes found" );
        }
        else
        {
            System.out.println( "No upstream nodes at all" );
        }
    }

    public void testKeyNodes() throws Exception
    {
        final KeyNodeAnalysis analysis = new KeyNodeAnalysis( null, "test" );
        int radius = 2;
        int direction = BioHub.DIRECTION_UP;
        SplittableRandom r = new SplittableRandom();

        KeyNodeAnalysisParameters parameters = analysis.getParameters();
        parameters.setMaxRadius( radius );
        parameters.setDirection( "Upstream" );
        parameters.setCalculatingFDR( false );
        Species spec = Species.getDefaultSpecies( null );
        String[] relTypes = new String[] {spec.getLatinName()};

        BioHubInfo[] hubInfos = BioHubRegistry.getBioHubInfos( dbOptions );
        for( BioHubInfo hub : hubInfos )
        {
            /*
             * ignore UserCollectionBioHub, since it needs additional collection to work
             * TODO: create test collection to check UserCollectionBioHub
             */
            if( UserCollectionBioHub.CUSTOM_REPOSITORY_HUB_NAME.equals( hub.getName() ) )
                continue;
            final KeyNodesHub<?> knHub = (KeyNodesHub<?>)hub.getBioHub();
            parameters.setBioHub( hub );
            DijkstraSearch search = new DijkstraSearch( knHub, dbOptions, relTypes );
            search.setMaxRadius( radius );
            search.setDirection( direction );

            Element[] elems = knHub.getRandomSample( 100, dbOptions, relTypes, r::nextInt );
            assertTrue( "Can not get random elements in hub " + knHub.getName(), elems.length > 0 );
            final Set<String> targetNames = StreamEx.of( elems ).peek( search::shortestPath ).map( Element::getAccession ).toSet();
            Map<String, Integer> res = search.getResult();
            assertTrue( "Can not find key nodes in hub " + knHub.getName(), res.size() > 0 );
            List<KeyNodeStats> values = StreamEx.ofKeys( res ).limit( 100 )
                    .map( element -> analysis.calculateKeyNodeStats( targetNames, element, 0, dbOptions, null ) ).toList();
            analysis.calculateScores( values );
            for( KeyNodeStats stats : values )
            {
                assertTrue( "Score incorrect for node " + stats.getTitle() + " in hub " + knHub.getName(), stats.getScore() > 0 );
            }
        }
    }

    public void testRandomSet() throws Exception
    {
        KeyNodeAnalysis analysis = new KeyNodeAnalysis( null, "test" );

        KeyNodeAnalysisParameters parameters = analysis.getParameters();
        parameters.setMaxRadius( 3 );
        parameters.setDirection( "Upstream" );
        parameters.setBioHub( bioHubInfo );
        Map<String, int[]> randSet = analysis.getRandomSet( 100, 500, bioHub, new TargetOptions( new CollectionRecord(
                "databases/GeneWays", true ) ) );
        int cntmulti = 0;
        for( Entry<String, int[]> entry : randSet.entrySet() )
        {
            int[] vals = entry.getValue();
            assertTrue( "No set numbers found", vals.length > 0 );
            if( vals.length > 1 )
            {
                System.out.println( entry.getKey() + ": " + Arrays.toString( vals ) );
                cntmulti++;
            }
        }
        System.out.println( cntmulti + " " + randSet.size() );

    }

    public void testFDR() throws Exception
    {
        KeyNodeAnalysis analysis = new KeyNodeAnalysis( null, "test" );
        KeyNodeAnalysisParameters parameters = analysis.getParameters();
        int radius = 2;
        parameters.setMaxRadius( radius );
        parameters.setDirection( "Upstream" );
        parameters.setBioHub( bioHubInfo );
        String[] names = new String[] {"MO000000022", "MO000030897", "MO000023551"};
        float[] scores = new float[] {0.17f, 0.18f, 0.17f};
        double[][] res = analysis.getFDR( names, scores, 3, dbOptions );
        for( int i = 0; i < 3; i++ )
        {
            System.out.println( res[i][0] + " " + res[i][1] );
        }

    }

    public void testContextAlgorithmNew()
    {
        UserHubEdge edge = new UserHubEdge( "" );
        HubGraph<Integer> nw = Arrays
                .asList( new HubRelation<>( 0, 1, edge, 1.5f ), new HubRelation<>( 1, 2, edge, 1.5f ),
                        new HubRelation<>( 2, 3, edge, 2.0f ), new HubRelation<>( 3, 4, edge, 1.5f ), new HubRelation<>( 3, 5, edge, 2.5f ) )
                .stream().collect( MemoryHubGraph.toMemoryHub() );
        assertEquals( "0 --> 1 (1.5)\n1 --> 2 (1.5)\n2 --> 3 (2.0)\n3 --> 4 (1.5)\n3 --> 5 (2.5)\n", GraphUtils.toString( nw ) );
        TObjectDoubleMap<Integer> weights = new TObjectDoubleHashMap<>();
        weights.put( 2, 1.0f );
        HubGraph<Integer> g = ApplyContextDecorator.applyContext( nw, weights, 0.1, BioHub.DIRECTION_BOTH );
        assertEquals( "Changes", "0 --> 1 (1.483335)\n1 --> 2 (0.0)\n2 --> 3 (0.0)\n3 --> 4 (1.483335)\n3 --> 5 (2.472225)\n",
                GraphUtils.toString( g ) );
    }
    
    public void testBatchScore() throws Exception
    {
        KeyNodeAnalysis analysis = new KeyNodeAnalysis( null, "test" );

        KeyNodeAnalysisParameters parameters = analysis.getParameters();
        parameters.setMaxRadius( 10 );
        parameters.setDirection( "Upstream" );
        parameters.setBioHub( bioHubInfo );
        
        String keyNode = "MO000056883";
        String[] targets = {
                "MO000005568", "MO000006707", "MO000006710", "MO000019780", "MO000024771", "MO000024773", "MO000024896", "MO000024920",
                "MO000024966", "MO000025703", "MO000026153", "MO000026235", "MO000026253", "MO000026254", "MO000026263", "MO000026305",
                "MO000046239", "MO000056182", "MO000059196", "MO000059587", "MO000079060", "MO000079681", "MO000082972", "MO000087775",
                "MO000092624", "MO000093137", "MO000104096", "MO000104097", "MO000104098", "MO000104099", "MO000104104", "MO000104106",
                "MO000104108", "MO000115218", "MO000115223", "MO000115224", "MO000118016", "MO000118053", "MO000118194", "MO000125100",
                "MO000132137", "MO000141393", "MO000142704", "MO000142707", "MO000142717", "MO000142718", "MO000142722", "MO000142723",
                "MO000155762", "MO000156606", "MO000166746", "MO000204315", "MO000218959", "MO000256250", "MO000256251", "MO000256443",
                "MO000256660", "MO000257528", "MO000271309", "MO000271481", "MO000271593", "MO000271594", "MO000271630", "MO000281119",
                "MO000281212", "MO000286869", "MO000286911", "MO000312397", "MO000314870", "MO000328122", "MO000334598", "MO000334599",
                "MO000334787"};
        TObjectIntMap<String> isoformFactor = analysis.getIsoformsStatistics( Arrays.asList( targets ) );
        KeyNodeStats keyNodeStats = analysis.calculateKeyNodeStats( StreamEx.of( targets ).toSet(), keyNode, 0, dbOptions, isoformFactor );
        analysis.calculateScores( Collections.singletonList( keyNodeStats )  );
        float expectedScore = keyNodeStats.getScore();

        int[] allSamples = IntStream.range(0, KeyNodeAnalysis.NUM_SAMPLES).toArray();
        Map<String, int[]> randomSet = StreamEx.of( targets ).toMap( x -> allSamples );
        List<TObjectIntMap<String>> isoformFactors = analysis.gatherIsoformStatistics( randomSet );
        float[][] scores = analysis.calculateScores( new String[]{keyNode}, randomSet, dbOptions, isoformFactors  );
        float score = scores[0][0];
        assertEquals( KeyNodeAnalysis.NUM_SAMPLES, scores.length );
        for( float[] score2 : scores )
        {
            assertEquals( 1, score2.length );
            assertEquals( score, score2[0]);
        }
        
        assertEquals(expectedScore, score);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        repository = CollectionFactory.createRepository( "../data" );
        assertNotNull( "Can't create repository", repository );
        module = (Module)repository.get( "Transpath" );
        if( module instanceof ClientModule )
        {
            ( (ClientModule)module ).login( "transpath", "transpath" );
        }
        assertNotNull( "Can't find module 'Transpath'", module );

        dbOptions = new TargetOptions( new CollectionRecord( "KeyNodesHub", true ) );
        BioHubInfo[] hubs = BioHubRegistry.getBioHubInfos( dbOptions );
        bioHubInfo = hubs[1];
        bioHub = (KeyNodesHub<?>)bioHubInfo.getBioHub();
        assertNotNull( "Transpath BioHub is not accessible", bioHub.getPriority( dbOptions ) );
    }

}
