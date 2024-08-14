package biouml.plugins.fbc._test;

import biouml.plugins.fbc.table.ScoreBasedFbcTableBuilder;
import biouml.plugins.fbc.table.ScoreBasedFbcTableBuilderParameters;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class ScoreBasedFbcTableBuilderTest extends FbcAnalysisTest
{
    public ScoreBasedFbcTableBuilderTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( ScoreBasedFbcTableBuilderTest.class.getName() );

        suite.addTest( new ScoreBasedFbcTableBuilderTest( "testOnlyScore" ) );
        suite.addTest( new ScoreBasedFbcTableBuilderTest( "testOnlyScoreCorrelation" ) );
        suite.addTest( new ScoreBasedFbcTableBuilderTest( "testScoreAndMax" ) );
        suite.addTest( new ScoreBasedFbcTableBuilderTest( "testScoreAndMaxNormalized" ) );
        suite.addTest( new ScoreBasedFbcTableBuilderTest( "testOnlyObjTable" ) );
        suite.addTest( new ScoreBasedFbcTableBuilderTest( "testObjTableAndColumn" ) );
        suite.addTest( new ScoreBasedFbcTableBuilderTest( "testScoreAndObjTableAndColumn" ) );

        return suite;
    }

    /**
     * Only score column is specified
     */
    public void testOnlyScore() throws Exception
    {
        ScoreBasedFbcTableBuilder analysis = initScoreBasedBuilder();
        analysis.getParameters().setInputEnzymes( enzymes.getCompletePath() );
        analysis.getParameters().setScoreColumnName( "Score" );
        analysis.validateParameters();

        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", result );
        assertEquals( "Wrong fbcData size", 5, result.getSize() );

        checkDataRow( result, "R000012", "-1.0", "", "1.0", 0.5 );
        checkDataRow( result, "R000014", "0.0", "", "1.0", -8.0 );
        checkDataRow( result, "R000017", "0.0", "", "1.0", 0.5 );
        checkDataRow( result, "R000021", "-1.0", "", "1.0", 0.2 );
        checkDataRow( result, "R000026", "-1.0", "", "1.0", 0.0 );
    }

    /**
     * Only score column is specified, correlation is true
     */
    public void testOnlyScoreCorrelation() throws Exception
    {
        ScoreBasedFbcTableBuilder analysis = initScoreBasedBuilder();
        analysis.getParameters().setInputEnzymes( enzymes.getCompletePath() );
        analysis.getParameters().setScoreColumnName( "Score" );
        analysis.getParameters().setCorrelation( true );
        analysis.validateParameters();

        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", result );
        assertEquals( "Wrong fbcData size", 5, result.getSize() );

        checkDataRow( result, "R000012", "-1.0", "", "1.0", 20.0 );
        checkDataRow( result, "R000014", "0.0", "", "1.0", -8.0 );
        checkDataRow( result, "R000017", "0.0", "", "1.0", 15.0 );
        checkDataRow( result, "R000021", "-1.0", "", "1.0", 10.0 );
        checkDataRow( result, "R000026", "-1.0", "", "1.0", 0.0 );
    }

    /**
     * Score and max column are specified
     */
    public void testScoreAndMax() throws Exception
    {
        ScoreBasedFbcTableBuilder analysis = initScoreBasedBuilder();
        analysis.getParameters().setInputEnzymes( enzymes.getCompletePath() );
        analysis.getParameters().setScoreColumnName( "Score" );
        analysis.getParameters().setMaxColumnName( "Max" );
        analysis.validateParameters();

        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", result );
        assertEquals( "Wrong fbcData size", 5, result.getSize() );

        checkDataRow( result, "R000012", "-3.0", "", "3.0", 0.5 );
        checkDataRow( result, "R000014", "0.0", "", "2.0", -8.0 );
        checkDataRow( result, "R000017", "0.0", "", "4.0", 0.5 );
        checkDataRow( result, "R000021", "-2.0", "", "2.0", 0.2 );
        checkDataRow( result, "R000026", "-2.0", "", "2.0", 0.0 );
    }

    /**
     * Score and max column are specified, norm is true
     */
    public void testScoreAndMaxNormalized() throws Exception
    {
        ScoreBasedFbcTableBuilder analysis = initScoreBasedBuilder();
        analysis.getParameters().setInputEnzymes( enzymes.getCompletePath() );
        analysis.getParameters().setScoreColumnName( "Score" );
        analysis.getParameters().setMaxColumnName( "Max" );
        analysis.getParameters().setNorm( true );
        analysis.validateParameters();

        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", result );
        assertEquals( "Wrong fbcData size", 5, result.getSize() );

        checkDataRow( result, "R000012", "-0.75", "", "0.75", 0.5 );
        checkDataRow( result, "R000014", "0.0", "", "0.5", -8.0 );
        checkDataRow( result, "R000017", "0.0", "", "1.0", 0.5 );
        checkDataRow( result, "R000021", "-0.5", "", "0.5", 0.2 );
        checkDataRow( result, "R000026", "-0.5", "", "0.5", 0.0 );
    }

    /**
     * Only objective table is specified
     */
    public void testOnlyObjTable() throws Exception
    {
        ScoreBasedFbcTableBuilder analysis = initScoreBasedBuilder();
        analysis.getParameters().setObjectiveTable( objective.getCompletePath() );
        analysis.validateParameters();

        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", result );
        assertEquals( "Wrong fbcData size", 5, result.getSize() );

        checkDataRow( result, "R000012", "-1.0", "", "1.0", 1.0 );
        checkDataRow( result, "R000014", "0.0", "", "1.0", 1.0 );
        checkDataRow( result, "R000017", "0.0", "", "1.0", 0.0 );
        checkDataRow( result, "R000021", "-1.0", "", "1.0", 0.0 );
        checkDataRow( result, "R000026", "-1.0", "", "1.0", 0.0 );
    }

    /**
     * Objective table and its column are specified
     */
    public void testObjTableAndColumn() throws Exception
    {
        ScoreBasedFbcTableBuilder analysis = initScoreBasedBuilder();
        analysis.getParameters().setObjectiveTable( objective.getCompletePath() );
        analysis.getParameters().setObjectiveColumnName( "Objective" );
        analysis.validateParameters();

        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", result );
        assertEquals( "Wrong fbcData size", 5, result.getSize() );

        checkDataRow( result, "R000012", "-1.0", "", "1.0", 2.0 );
        checkDataRow( result, "R000014", "0.0", "", "1.0", 1.0 );
        checkDataRow( result, "R000017", "0.0", "", "1.0", 0.0 );
        checkDataRow( result, "R000021", "-1.0", "", "1.0", 0.0 );
        checkDataRow( result, "R000026", "-1.0", "", "1.0", 0.0 );
    }

    /**
     * Score column, objective table and its column are specified
     */
    public void testScoreAndObjTableAndColumn() throws Exception
    {
        ScoreBasedFbcTableBuilder analysis = initScoreBasedBuilder();
        analysis.getParameters().setInputEnzymes( enzymes.getCompletePath() );
        analysis.getParameters().setScoreColumnName( "Score" );
        analysis.getParameters().setObjectiveTable( objective.getCompletePath() );
        analysis.getParameters().setObjectiveColumnName( "Objective" );
        analysis.validateParameters();

        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", result );
        assertEquals( "Wrong fbcData size", 5, result.getSize() );

        checkDataRow( result, "R000012", "-1.0", "", "1.0", 1.0 );
        checkDataRow( result, "R000014", "0.0", "", "1.0", -8.0 );
        checkDataRow( result, "R000017", "0.0", "", "1.0", 0.0 );
        checkDataRow( result, "R000021", "-1.0", "", "1.0", 0.0 );
        checkDataRow( result, "R000026", "-1.0", "", "1.0", 0.0 );
    }

    private ScoreBasedFbcTableBuilder initScoreBasedBuilder()
    {
        ScoreBasedFbcTableBuilder analysis = AnalysisMethodRegistry.getAnalysisMethod( ScoreBasedFbcTableBuilder.class );
        ScoreBasedFbcTableBuilderParameters parameters = analysis.getParameters();
        parameters.setInputDiagram( COLLECTION_NAME.getChildPath( DIAGRAM_NAME ) );
        parameters.setOutputPath( resultPath );
        return analysis;
    }

    private static final String DIAGRAM_NAME = "ScoreTestDiagram";
    private TableDataCollection enzymes;
    private TableDataCollection objective;
    private DataElementPath resultPath;
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        resultPath = TableDataCollectionUtils.createTableDataCollection( fvc, "fbcData" ).getCompletePath();

        enzymes = TableDataCollectionUtils.createTableDataCollection( fvc, "enzymes" );
        fvc.put( enzymes );
        enzymes.getColumnModel().addColumn( "Name", String.class );
        enzymes.getColumnModel().addColumn( "Max", Double.class );
        enzymes.getColumnModel().addColumn( "Score", Double.class );
        TableDataCollectionUtils.addRow( enzymes, "p1", new Object[] {"p1", 3.0, -0.8} );
        TableDataCollectionUtils.addRow( enzymes, "p2", new Object[] {"p2", 4.0, 0.5} );
        TableDataCollectionUtils.addRow( enzymes, "p3", new Object[] {"p3", 2.0, 0.2} );

        objective = TableDataCollectionUtils.createTableDataCollection( fvc, "objective" );
        fvc.put( objective );
        objective.getColumnModel().addColumn( "Name", String.class );
        objective.getColumnModel().addColumn( "Objective", Double.class );
        TableDataCollectionUtils.addRow( objective, "R000012", new Object[] {"R000012", 2.0} );
        TableDataCollectionUtils.addRow( objective, "R000014", new Object[] {"R000014", 1.0} );
    }
}
