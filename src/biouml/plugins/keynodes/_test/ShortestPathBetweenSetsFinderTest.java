package biouml.plugins.keynodes._test;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import biouml.plugins.keynodes.DirectionEditor;
import biouml.plugins.keynodes.ShortestPathsBetweenSetsFinder;
import biouml.plugins.keynodes.ShortestPathsBetweenSetsFinderParameters;
import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class ShortestPathBetweenSetsFinderTest extends AnalysisTest
{

    public ShortestPathBetweenSetsFinderTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( ShortestPathBetweenSetsFinderTest.class.getName() );
        suite.addTest( new ShortestPathBetweenSetsFinderTest( "testSPBSFPathGenerator" ) );
        suite.addTest( new ShortestPathBetweenSetsFinderTest( "testSPBSFDownstream" ) );
        suite.addTest( new ShortestPathBetweenSetsFinderTest( "testSPBSFUpstream" ) );
        suite.addTest( new ShortestPathBetweenSetsFinderTest( "testSPBSFBoth" ) );
        suite.addTest( new ShortestPathBetweenSetsFinderTest( "testActionsSPBSF" ) );
        return suite;
    }

    public void testSPBSFPathGenerator()
    {
        ShortestPathsBetweenSetsFinder analysis = initSPBSFinder( DirectionEditor.DOWNSTREAM );
        analysis.getParameters().setMaxRadius( 4 );
        assertEquals( bioHubInfo.getBioHub(), analysis.getKeyNodesHub() );

        String keyName = "E01";
        //test getKeysFromName
        assertEquals( "Get keys from name: ", "[\"E01\"]", analysis.getKeysFromName( keyName ).toString() );
        //test generatePaths
        StringSet hits = new StringSet( Arrays.asList( "E03", "E02" ) );
        List<String> paths = StreamEx.of( analysis.generatePaths( keyName, hits ) )
                .map( elements -> StreamEx.of( elements ).map( Element::getAccession ).toCollection( StringSet::new ).toString() ).toList();
        assertEquals( 2, paths.size() );
        assertEquals( "[\"E02\",\"X01\",\"E01\"]", paths.get( 0 ) );
        assertEquals( "[\"E03\",\"X02\",\"E01\"]", paths.get( 1 ) );
        //test getAllReactions
        StringSet expectedR = new StringSet( Arrays.asList( "X04", "X02", "X01" ) );
        StringSet reactions = StreamEx.of( analysis.getAllReactions( keyName, hits ) ).map( Element::getAccession )
                .toCollection( StringSet::new );
        assertEquals( expectedR.size(), reactions.size() );
        assertEquals( expectedR, reactions ); //compare StringSets
    }

    public void testSPBSFDownstream() throws Exception
    {
        ShortestPathsBetweenSetsFinder analysis = initSPBSFinder( DirectionEditor.DOWNSTREAM );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 4 );
        assertNull( result.get( "E02" ) );

        checkResultRow( result, "E01", "[\"E03\",\"E04\",\"E05\"]" );
        checkResultRow( result, "E03", "[\"E04\",\"E05\"]" );
        checkResultRow( result, "E06", "[\"E03\",\"E04\",\"E05\"]" );
        checkResultRow( result, "E11", "[]" );
    }

    public void testSPBSFUpstream() throws Exception
    {
        ShortestPathsBetweenSetsFinder analysis = initSPBSFinder( DirectionEditor.UPSTREAM );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 4 );
        assertNull( result.get( "E05" ) );

        checkResultRow( result, "E01", "[\"E03\",\"E05\"]" );
        checkResultRow( result, "E03", "[]" );
        checkResultRow( result, "E06", "[\"E03\",\"E04\",\"E05\"]" );
        checkResultRow( result, "E11", "[\"E07\"]" );
    }

    public void testSPBSFBoth() throws Exception
    {
        ShortestPathsBetweenSetsFinder analysis = initSPBSFinder( DirectionEditor.BOTH );
        analysis.getParameters().setSourcePath( endTable.getCompletePath() );
        analysis.getParameters().setEndSet( startTable.getCompletePath() );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 4 );
        assertNull( result.get( "E02" ) );

        checkResultRow( result, "E03", "[\"E01\",\"E06\"]" );
        checkResultRow( result, "E04", "[\"E01\",\"E03\",\"E06\"]" );
        checkResultRow( result, "E05", "[\"E01\",\"E03\",\"E06\"]" );
        checkResultRow( result, "E07", "[\"E11\"]" );
    }

    public void testActionsSPBSF() throws Exception
    {
        ShortestPathsBetweenSetsFinder analysis = initSPBSFinder( DirectionEditor.BOTH );

        TableDataCollection result = analysis.justAnalyzeAndPut();
        checkResultTable( result, 4 );

        checkActions( result, new StringSet( Arrays.asList( "E02", "E11" ) ) );
    }

    private void checkResultRow(@Nonnull TableDataCollection result, @Nonnull String rdeName, @Nonnull String hitNames) throws Exception
    {
        RowDataElement rde = result.get( rdeName );
        assertNotNull( rdeName + " is absent", rde );
        assertEquals( rdeName + ": hit names", hitNames, rde.getValues()[hitsIndex].toString() );
    }
    @Override
    protected void checkResultRow(@Nonnull TableDataCollection result, @Nonnull String rdeName, @Nonnull String prefix, int inputElements,
            int totalElements, double score, @Nonnull String hitNames) throws Exception
    {
        checkResultRow( result, rdeName, hitNames );
    }

    private int hitsIndex;
    @Override
    protected void checkResultTable(TableDataCollection result, int expectedSize)
    {
        assertNotNull( "Result table is absent", result );
        assertEquals( "Result table size", 4, result.getSize() );

        hitsIndex = result.getColumnModel().optColumnIndex( "Hits" );
        assertTrue( "Hits column missed", hitsIndex >= 0 );
    }

    protected TableDataCollection startTable;
    protected TableDataCollection endTable;
    private ShortestPathsBetweenSetsFinder initSPBSFinder(String searchDirection)
    {
        ShortestPathsBetweenSetsFinder analysis = AnalysisMethodRegistry.getAnalysisMethod( ShortestPathsBetweenSetsFinder.class );
        ShortestPathsBetweenSetsFinderParameters parameters = analysis.getParameters();
        parameters.setBioHub( bioHubInfo );
        parameters.setSourcePath( startTable.getCompletePath() );
        parameters.setEndSet( endTable.getCompletePath() );
        parameters.setDirection( searchDirection );
        parameters.setMaxRadius( 4 );
        parameters.setSpecies( species );
        analysis.validateParameters();
        return analysis;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        FolderVectorCollection fvc = (FolderVectorCollection)table.getOrigin();
        assertNotNull( fvc );
        startTable = TableDataCollectionUtils.createTableDataCollection( fvc, "startSet" );
        fvc.put( startTable );
        endTable = TableDataCollectionUtils.createTableDataCollection( fvc, "endSet" );
        fvc.put( endTable );

        startTable.getColumnModel().addColumn( "Name", String.class );
        startTable.getColumnModel().addColumn( "Comment", String.class );
        TableDataCollectionUtils.addRow( startTable, "E01", new Object[] {"E01", "1st"} );
        TableDataCollectionUtils.addRow( startTable, "E03", new Object[] {"E03", "3rd"} );
        TableDataCollectionUtils.addRow( startTable, "E06", new Object[] {"E06", "6th"} );

        TableDataCollectionUtils.addRow( startTable, "E11", new Object[] {"E11", "11th"} );

        endTable.getColumnModel().addColumn( "Name", String.class );
        endTable.getColumnModel().addColumn( "Comment", String.class );
        TableDataCollectionUtils.addRow( endTable, "E03", new Object[] {"E03", "3rd"} );
        TableDataCollectionUtils.addRow( endTable, "E04", new Object[] {"E02", "4th"} );
        TableDataCollectionUtils.addRow( endTable, "E05", new Object[] {"E03", "5th"} );

        TableDataCollectionUtils.addRow( endTable, "E07", new Object[] {"E07", "7th"} );
    }
}
