package biouml.plugins.keynodes._test;

import java.util.Arrays;
import java.util.List;

import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.keynodes.DirectionEditor;
import biouml.plugins.keynodes.LongestChainFinder;
import biouml.plugins.keynodes.LongestChainFinderParameters;
import junit.framework.Test;
import junit.framework.TestSuite;

public class LongestChainFinderTest extends AnalysisTest
{

    public LongestChainFinderTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( LongestChainFinderTest.class.getName() );
        suite.addTest( new LongestChainFinderTest( "testLCFPathGenerator" ) );
        suite.addTest( new LongestChainFinderTest( "testLongestChainFinderDownstream" ) );
        suite.addTest( new LongestChainFinderTest( "testLongestChainFinderUpstream" ) );
        suite.addTest( new LongestChainFinderTest( "testLongestChainFinderBoth" ) );
        suite.addTest( new LongestChainFinderTest( "testActionsLCF" ) );
        return suite;
    }

    public void testLCFPathGenerator()
    {
        LongestChainFinder analysis = initLongestChainFinder( DirectionEditor.DOWNSTREAM );
        analysis.getParameters().setMaxRadius( 4 );
        assertEquals( bioHubInfo.getBioHub(), analysis.getKeyNodesHub() );

        String chainName = "E01 -> E05";
        //test getKeysFromName
        assertEquals( "Get keys from name: ", "[\"E01\",\"E05\"]", analysis.getKeysFromName( chainName ).toString() );
        //test generatePaths
        StringSet hits = new StringSet( Arrays.asList( "E05", "E04", "E01" ) );
        List<String> paths = StreamEx.of( analysis.generatePaths( chainName, hits ) )
                .map( elements -> StreamEx.of( elements ).map( Element::getAccession ).toCollection( StringSet::new ).toString() ).toList();
        assertEquals( 2, paths.size() );
        assertEquals( "[\"E04\",\"X08\",\"E05\"]", paths.get( 0 ) );
        assertEquals( "[\"E01\",\"X01\",\"E02\",\"X05\",\"E04\"]", paths.get( 1 ) );
        //test getAllReactions
        StringSet expectedR = new StringSet( Arrays.asList( "X08", "X06", "X05", "X02", "X01" ) );
        StringSet reactions = StreamEx.of( analysis.getAllReactions( chainName, hits ) ).map( Element::getAccession )
                .toCollection( StringSet::new );
        assertEquals( expectedR.size(), reactions.size() );
        assertEquals( expectedR, reactions ); //compare StringSets
    }

    public void testLongestChainFinderDownstream() throws Exception
    {
        LongestChainFinder analysis = initLongestChainFinder( DirectionEditor.DOWNSTREAM );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 11 );
        assertNull( result.get( "E01 -> E06" ) );
        assertNull( result.get( "E11 -> E07" ) );

        checkResultRow( result, "E01 -> E05", "Chain #1", 4, 4, 0.8, "[\"E05\",\"E04\",\"E03\",\"E01\"]" );
        checkResultRow( result, "E07 -> E14", "Chain #2", 3, 5, 0.5, "[\"E14\",\"E11\",\"E07\"]" );
        checkResultRow( result, "E05 -> E04", "Chain #3", 2, 2, 2 / 3.0, "[\"E04\",\"E05\"]" );
        checkResultRow( result, "E14 -> E11", "Chain #4", 2, 3, 0.5, "[\"E11\",\"E14\"]" );
    }

    public void testLongestChainFinderUpstream() throws Exception
    {
        LongestChainFinder analysis = initLongestChainFinder( DirectionEditor.UPSTREAM );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 11 );
        assertNull( result.get( "E01 -> E06" ) );
        assertNull( result.get( "E07 -> E11" ) );

        checkResultRow( result, "E05 -> E01", "Chain #1", 4, 4, 0.8, "[\"E01\",\"E03\",\"E04\",\"E05\"]" );
        checkResultRow( result, "E14 -> E07", "Chain #2", 3, 5, 0.5, "[\"E07\",\"E11\",\"E14\"]" );
        checkResultRow( result, "E04 -> E05", "Chain #3", 2, 2, 2 / 3.0, "[\"E05\",\"E04\"]" );
        checkResultRow( result, "E11 -> E14", "Chain #4", 2, 3, 0.5, "[\"E14\",\"E11\"]" );
    }

    public void testLongestChainFinderBoth() throws Exception
    {
        LongestChainFinder analysis = initLongestChainFinder( DirectionEditor.BOTH );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 18 );
        assertNull( result.get( "E01 -> E06" ) );

        checkResultRow( result, "E01 -> E05", "Chain #1", 4, 4, 0.8, "[\"E05\",\"E04\",\"E03\",\"E01\"]" );
        checkResultRow( result, "E07 -> E14", "Chain #2", 3, 5, 0.5, "[\"E14\",\"E11\",\"E07\"]" );
        checkResultRow( result, "E01 -> E03", "Chain #3", 2, 2, 2 / 3.0, "[\"E03\",\"E01\"]" );
        checkResultRow( result, "E07 -> E11", "Chain #4", 2, 3, 0.5, "[\"E11\",\"E07\"]" );
        checkResultRow( result, "E05 -> E01", "Chain #5", 4, 4, 0.8, "[\"E01\",\"E03\",\"E04\",\"E05\"]" );
        checkResultRow( result, "E14 -> E07", "Chain #6", 3, 5, 0.5, "[\"E07\",\"E11\",\"E14\"]" );
        checkResultRow( result, "E03 -> E01", "Chain #7", 2, 2, 2 / 3.0, "[\"E01\",\"E03\"]" );
        checkResultRow( result, "E11 -> E07", "Chain #8", 2, 3, 0.5, "[\"E07\",\"E11\"]" );
    }

    public void testActionsLCF() throws Exception
    {
        LongestChainFinder analysis = initLongestChainFinder( DirectionEditor.BOTH );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 18 );

        checkActions( result, new StringSet( Arrays.asList( "E01 -> E05", "E07 -> E14" ) ) );
    }

    private LongestChainFinder initLongestChainFinder(String searchDirection)
    {
        LongestChainFinder analysis = AnalysisMethodRegistry.getAnalysisMethod( LongestChainFinder.class );
        LongestChainFinderParameters parameters = analysis.getParameters();
        parameters.setBioHub( bioHubInfo );
        parameters.setSourcePath( table.getCompletePath() );
        parameters.setDirection( searchDirection );
        parameters.setMaxDijkstraDepth( 10 );
        parameters.setMaxRadius( 2 );
        parameters.setScoreCoeff( 1.0 );
        parameters.setSpecies( species );
        analysis.validateParameters();
        return analysis;
    }

}
