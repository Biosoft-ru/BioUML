package biouml.plugins.keynodes._test;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.keynodes.DirectionEditor;
import biouml.plugins.keynodes.EffectorKeyNodes;
import biouml.plugins.keynodes.KeyNodeAnalysis;
import biouml.plugins.keynodes.KeyNodeAnalysisParameters;
import biouml.plugins.keynodes.RegulatorKeyNodes;
import junit.framework.Test;
import junit.framework.TestSuite;

public class KeyNodeAnalysisTest extends AnalysisTest
{

    public KeyNodeAnalysisTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( KeyNodeAnalysisTest.class.getName() );
        suite.addTest( new KeyNodeAnalysisTest( "testKNAPathGenerator" ) );
        suite.addTest( new KeyNodeAnalysisTest( "testEffectorKeyNodes" ) );
        suite.addTest( new KeyNodeAnalysisTest( "testRegulatorKeyNodes" ) );
        suite.addTest( new KeyNodeAnalysisTest( "testEffectorKeyNodesWithFDR" ) );
        suite.addTest( new KeyNodeAnalysisTest( "testRegulatorKeyNodesWithFDR" ) );
        suite.addTest( new KeyNodeAnalysisTest( "testActionsKNA" ) );
        return suite;
    }

    public void testKNAPathGenerator()
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        analysis.getParameters().setMaxRadius( 4 );
        assertEquals( bioHubInfo.getBioHub(), analysis.getKeyNodesHub() );

        String name = "E13";
        //test getKeysFromName
        assertEquals( "Get keys from name: ", "[\"E13\"]", analysis.getKeysFromName( name ).toString() );
        //test generatePaths
        StringSet hits = new StringSet( Arrays.asList( "E11", "E14" ) );
        List<String> paths = StreamEx.of( analysis.generatePaths( name, hits ) )
                .map( elements -> StreamEx.of( elements ).map( Element::getAccession ).toCollection( StringSet::new ).toString() ).toList();
        assertEquals( 2, paths.size() );
        assertEquals( "[\"E11\",\"X11\",\"E13\"]", paths.get( 0 ) );
        assertEquals( "[\"E14\",\"X11\",\"E13\"]", paths.get( 1 ) );
        //test getAllReactions
        StringSet expectedR = new StringSet( Arrays.asList( "X11" ) );
        StringSet reactions = StreamEx.of( analysis.getAllReactions( name, hits ) ).map( Element::getAccession )
                .toCollection( StringSet::new );
        assertEquals( expectedR.size(), reactions.size() );
        assertEquals( expectedR, reactions ); //compare StringSets

        name = "E06";
        //test getKeysFromName
        assertEquals( "Get keys from name: ", "[\"E06\"]", analysis.getKeysFromName( name ).toString() );
        //test generatePaths
        hits = new StringSet( Arrays.asList( "E01", "E03", "E05" ) );
        paths = StreamEx.of( analysis.generatePaths( name, hits ) )
                .map( elements -> StreamEx.of( elements ).map( Element::getAccession ).toCollection( StringSet::new ).toString() ).toList();
        assertEquals( 3, paths.size() );
        assertEquals( "[\"E01\",\"X03\",\"E06\"]", paths.get( 0 ) );
        assertEquals( "[\"E03\",\"X07\",\"E06\"]", paths.get( 1 ) );
        assertEquals( "[\"E05\",\"X09\",\"E06\"]", paths.get( 2 ) );
        //test getAllReactions
        expectedR = new StringSet( Arrays.asList( "X03", "X07", "X09", "X02" ) );
        reactions = StreamEx.of( analysis.getAllReactions( name, hits ) ).map( Element::getAccession ).toCollection( StringSet::new );
        assertEquals( expectedR.size(), reactions.size() );
        assertEquals( expectedR, reactions ); //compare StringSets
    }

    public void testEffectorKeyNodes() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 10 );
        assertNull( "Score cutoff ", result.get( "E02" ) );
        assertNull( "Element which is not effector detected", result.get( "E07" ) );

        checkResultRow( result, "E03", "E03", 2, 3, 0.5651875734329224, "[\"E01\",\"E03\"]" );
        checkResultRow( result, "E04", "E04", 3, 4, 0.7200928926467896, "[\"E03\",\"E04\",\"E05\"]" );
        checkResultRow( result, "E05", "E05", 2, 3, 0.5651875734329224, "[\"E04\",\"E05\"]" );
        checkResultRow( result, "E06", "E06", 3, 3, 0.4819277226924896, "[\"E01\",\"E03\",\"E05\"]" );
        checkResultRow( result, "E09", "E09", 1, 2, 0.40069687366485596, "[\"E07\"]" );
        checkResultRow( result, "E10", "E10", 1, 2, 0.40069687366485596, "[\"E07\"]" );
        checkResultRow( result, "E11", "E11", 3, 8, 0.6818181872367859, "[\"E07\",\"E11\",\"E14\"]" );
        checkResultRow( result, "E13", "E13", 2, 5, 0.7788417935371399, "[\"E11\",\"E14\"]" );
        checkResultRow( result, "E14", "E14", 2, 4, 0.5555555820465088, "[\"E11\",\"E14\"]" );
    }

    public void testRegulatorKeyNodes() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.UPSTREAM );
        table.remove( "E11" );
        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 7 );
        assertNull( "Score cutoff ", result.get( "E11" ) );
        assertNull( "Score cutoff ", result.get( "E12" ) );
        assertNull( "Element which is not regulator detected", result.get( "E14" ) );

        checkResultRow( result, "E01", "E01", 2, 4, 0.9384164214134216, "[\"E01\",\"E03\"]" );
        checkResultRow( result, "E02", "E02", 2, 2, 0.4761904776096344, "[\"E03\",\"E04\"]" );
        checkResultRow( result, "E03", "E03", 2, 3, 0.9489872455596924, "[\"E03\",\"E04\"]" );
        checkResultRow( result, "E04", "E04", 2, 2, 0.9600614309310913, "[\"E04\",\"E05\"]" );
        checkResultRow( result, "E05", "E05", 2, 3, 0.9489872455596924, "[\"E04\",\"E05\"]" );
        checkResultRow( result, "E06", "E06", 2, 2, 0.4761904776096344, "[\"E01\",\"E05\"]" );
        checkResultRow( result, "E13", "E13", 1, 3, 0.6871036291122437, "[\"E14\"]" );
    }

    public void testEffectorKeyNodesWithFDR() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        table.remove( "E11" );
        analysis.getParameters().setCalculatingFDR( true );
        analysis.getParameters().setFDRcutoff( 0.03 );
        analysis.getParameters().setSeed( 1L );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 1 );
        assertNull( "Score cutoff ", result.get( "E02" ) );
        assertNull( "Element which is not effector detected", result.get( "E07" ) );
        assertNull( "FDR score cutoff ", result.get( "E03" ) );
        assertNull( "FDR score cutoff ", result.get( "E05" ) );
        assertNull( "FDR score cutoff ", result.get( "E06" ) );
        assertNull( "FDR score cutoff ", result.get( "E09" ) );
        assertNull( "FDR score cutoff ", result.get( "E10" ) );
        assertNull( "FDR score cutoff ", result.get( "E11" ) );
        assertNull( "FDR score cutoff ", result.get( "E13" ) );
        assertNull( "FDR score cutoff ", result.get( "E14" ) );

        checkResultRow( result, "E04", "E04", 3, 4, 0.956843912601471, "[\"E03\",\"E04\",\"E05\"]" );
        checkFDR( result, "E04", "E04", 0.024000000208616257, 2.1294875144958496, 0 );
    }

    public void testRegulatorKeyNodesWithFDR() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.UPSTREAM );
        table.remove( "E11" );
        analysis.getParameters().setCalculatingFDR( true );
        analysis.getParameters().setSeed( 1L );
        analysis.getParameters().setFDRcutoff( 0.03 );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 1 );
        assertNull( "Score cutoff ", result.get( "E06" ) );
        assertNull( "Score cutoff ", result.get( "E11" ) );
        assertNull( "Score cutoff ", result.get( "E12" ) );
        assertNull( "Element which is not regulator detected", result.get( "E14" ) );
        assertNull( "FDR score cutoff ", result.get( "E01" ) );
        assertNull( "FDR score cutoff ", result.get( "E02" ) );
        assertNull( "FDR score cutoff ", result.get( "E03" ) );
        assertNull( "FDR score cutoff ", result.get( "E05" ) );
        assertNull( "FDR score cutoff ", result.get( "E13" ) );

        checkResultRow( result, "E04", "E04", 2, 2, 0.9600614309310913, "[\"E04\",\"E05\"]" );
        checkFDR( result, "E04", "E04", 0.013000000268220901, 2.141629695892334, 0 );
    }

    public void testActionsKNA() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.UPSTREAM );
        table.remove( "E11" );
        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 7 );

        checkActions( result, new StringSet( Arrays.asList( "E13", "E03" ) ) );
    }

    protected KeyNodeAnalysis initKeyNodeAnalysis(String direction)
    {
        Class<? extends KeyNodeAnalysis> analysisClass = DirectionEditor.DOWNSTREAM.equals( direction ) ? EffectorKeyNodes.class
                : RegulatorKeyNodes.class;
        KeyNodeAnalysis analysis = AnalysisMethodRegistry.getAnalysisMethod( analysisClass );
        KeyNodeAnalysisParameters parameters = analysis.getParameters();
        parameters.setBioHub( bioHubInfo );
        parameters.setSourcePath( table.getCompletePath() );
        parameters.setScoreCutoff( 0.3 );
        parameters.setSpecies( species );
        analysis.validateParameters();
        return analysis;
    }

    protected void checkFDR(@Nonnull TableDataCollection result, @Nonnull String rdeName, @Nonnull String prefix, double fdr,
            double zScore, int ranksSum) throws Exception
    {
        int fdrIndex = result.getColumnModel().optColumnIndex( "FDR" );
        assertTrue( "FDR column missed ", fdrIndex > 0 );

        RowDataElement rde = result.get( rdeName );
        assertNotNull( prefix + " is absent", rde );

        assertTrue( "Incorrect FDR type", rde.getValues()[fdrIndex] instanceof Double );
        assertTrue( "Incorrect Z-Score type", rde.getValues()[fdrIndex + 1] instanceof Double );
        assertTrue( "Incorrect Ranks sum type", rde.getValues()[fdrIndex + 2] instanceof Integer );

        assertEquals( "FDR ", fdr, (Double)rde.getValues()[fdrIndex], 0.000001 );
        assertEquals( "Z-Score ", zScore, (Double)rde.getValues()[fdrIndex + 1], 0.000001 );
        assertEquals( "Ranks sum ", ranksSum, (int)rde.getValues()[fdrIndex + 2] );
    }

}
