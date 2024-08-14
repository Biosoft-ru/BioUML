package biouml.plugins.fbc._test;

import javax.annotation.Nonnull;

import biouml.plugins.fbc.analysis.FbcAnalysis;
import biouml.plugins.fbc.analysis.FbcAnalysisParameters;
import biouml.plugins.fbc.table.FbcBuilderDataTableAnalysis;
import biouml.plugins.fbc.table.FbcBuilderDataTableAnalysisParameters;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class FbcAnalysisTest extends AbstractBioUMLTest
{
    public FbcAnalysisTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( FbcAnalysisTest.class.getName() );
        suite.addTest( new FbcAnalysisTest( "testSimpleTableBuilderFbcDiagram" ) );
        suite.addTest( new FbcAnalysisTest( "testSimpleTableBuilderNonFbcDiagram" ) );
        suite.addTest( new FbcAnalysisTest( "testFbcAnalysis" ) );
        return suite;
    }

    public void testSimpleTableBuilderFbcDiagram() throws Exception
    {
        TableDataCollection fbcData = TableDataCollectionUtils.createTableDataCollection( fvc, "fbcData" );
        FbcBuilderDataTableAnalysis fbcBDT = initSimpleBuilder( "01187-sbml-l3v1", fbcData.getCompletePath() );

        fbcData = fbcBDT.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", fbcData );
        assertEquals( "Wrong fbcData size", 26, fbcData.getSize() );

        checkDataRow( fbcData, "R01", "0.0", "", "1.0", 0.0 );
        checkDataRow( fbcData, "R02", "-1000.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R03", "-1000.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R04", "-1000.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R05", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R06", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R07", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R08", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R09", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R10", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R11", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R12", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R13", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R14", "-1000.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R15", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R16", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R17", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R18", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R19", "-1000.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R20", "-1000.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R21", "-1000.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R22", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R23", "-1000.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R24", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R25", "0.0", "", "1000.0", 0.0 );
        checkDataRow( fbcData, "R26", "0.0", "", "1000.0", 1.0 );
    }

    public void testSimpleTableBuilderNonFbcDiagram() throws Exception
    {
        TableDataCollection fbcData = TableDataCollectionUtils.createTableDataCollection( fvc, "fbcData" );
        FbcBuilderDataTableAnalysis fbcBDT = initSimpleBuilder( "ffn", fbcData.getCompletePath() );
        fbcBDT.getParameters().setLowerBoundDefault( "0.0" );
        fbcBDT.getParameters().setUpperBoundDefault( "50.0" );
        fbcBDT.getParameters().setEqualsDefault( "" );
        fbcBDT.getParameters().setFbcObjective( 2.0 );

        fbcData = fbcBDT.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", fbcData );
        assertEquals( "Wrong fbcData size", 4, fbcData.getSize() );

        checkDataRow( fbcData, "antimonyNoName0", "0.0", "", "50.0", 2.0 );
        checkDataRow( fbcData, "antimonyNoName1", "0.0", "", "50.0", 2.0 );
        checkDataRow( fbcData, "r1", "0.0", "", "50.0", 2.0 );
        checkDataRow( fbcData, "r2", "0.0", "", "50.0", 2.0 );
    }

    public void testFbcAnalysis() throws Exception
    {
        TableDataCollection fbcData = TableDataCollectionUtils.createTableDataCollection( fvc, "fbcData" );
        TableDataCollection fbcResult = TableDataCollectionUtils.createTableDataCollection( fvc, "fbcResult" );
        FbcBuilderDataTableAnalysis fbcBDT = initSimpleBuilder( "01188-sbml-l3v1", fbcData.getCompletePath() );
        fbcData = fbcBDT.justAnalyzeAndPut();

        FbcAnalysis analysis = initFbcAnalysis( "01188-sbml-l3v1", fbcData.getCompletePath(), fbcResult.getCompletePath() );

        fbcResult = analysis.justAnalyzeAndPut();
        assertNotNull( "Result does not exist", fbcResult );
        assertEquals( "Wrong result size", 26, fbcResult.getSize() );
        assertEquals( "Wrong value function", 1.0, Double.parseDouble( fbcResult.getInfo().getProperty( "Value Function" ) ), 0.00001 );

        checkFbcRow( fbcResult, "R01", 1.0 );
        checkFbcRow( fbcResult, "R02", 1.0 );
        checkFbcRow( fbcResult, "R03", 0.0 );
        checkFbcRow( fbcResult, "R04", 0.0 );
        checkFbcRow( fbcResult, "R05", 1.0 );
        checkFbcRow( fbcResult, "R06", 1.0 );
        checkFbcRow( fbcResult, "R07", 1.0 );
        checkFbcRow( fbcResult, "R08", 1.0 );
        checkFbcRow( fbcResult, "R09", 0.0 );
        checkFbcRow( fbcResult, "R10", 0.0 );
        checkFbcRow( fbcResult, "R11", 0.0 );
        checkFbcRow( fbcResult, "R12", 1.0 );
        checkFbcRow( fbcResult, "R13", 1.0 );
        checkFbcRow( fbcResult, "R14", 1.0 );
        checkFbcRow( fbcResult, "R15", 1.0 );
        checkFbcRow( fbcResult, "R16", 0.0 );
        checkFbcRow( fbcResult, "R17", 0.0 );
        checkFbcRow( fbcResult, "R18", 0.0 );
        checkFbcRow( fbcResult, "R19", 0.0 );
        checkFbcRow( fbcResult, "R20", 0.0 );
        checkFbcRow( fbcResult, "R21", 0.0 );
        checkFbcRow( fbcResult, "R22", 1.0 );
        checkFbcRow( fbcResult, "R23", 1.0 );
        checkFbcRow( fbcResult, "R24", 0.0 );
        checkFbcRow( fbcResult, "R25", 0.0 );
        checkFbcRow( fbcResult, "R26", 1.0 );
    }

    private void checkFbcRow(@Nonnull TableDataCollection result, @Nonnull String rdeName, double optimalValue) throws Exception
    {
        RowDataElement rde = result.get( rdeName );
        assertNotNull( rdeName + " is absent", rde );
        assertEquals( rdeName + ": value", optimalValue, (Double)rde.getValues()[0], 0.000001 );
    }

    protected void checkDataRow(@Nonnull TableDataCollection result, @Nonnull String rdeName, @Nonnull String lowerBound,
            @Nonnull String eqBound, @Nonnull String upperBound, double coeff) throws Exception
    {
        RowDataElement rde = result.get( rdeName );
        assertNotNull( rdeName + " is absent", rde );
        Object[] values = rde.getValues();
        int lowerBoundIndex = result.getColumnModel().optColumnIndex( "Greater" );
        assertEquals( rdeName + ": lower bound", lowerBound, values[lowerBoundIndex] );
        assertEquals( rdeName + ": equals", eqBound, values[lowerBoundIndex + 1] );
        assertEquals( rdeName + ": upper bound", upperBound, values[lowerBoundIndex + 2] );
        assertEquals( rdeName + ": objective coefficient", coeff, (Double)values[lowerBoundIndex + 3], 0.000001 );
    }

    private FbcAnalysis initFbcAnalysis(String diagramName, DataElementPath fbcDataPath, DataElementPath fbcResultPath)
    {
        FbcAnalysis analysis = AnalysisMethodRegistry.getAnalysisMethod( FbcAnalysis.class );
        FbcAnalysisParameters parameters = analysis.getParameters();
        parameters.setDiagramPath( COLLECTION_NAME.getChildPath( diagramName ) );
        parameters.setFbcDataTablePath( fbcDataPath );
        parameters.setFbcResultPath( fbcResultPath );
        analysis.validateParameters();
        return analysis;
    }

    private FbcBuilderDataTableAnalysis initSimpleBuilder(String diagramName, DataElementPath fbcDataPath)
    {
        FbcBuilderDataTableAnalysis analysis = AnalysisMethodRegistry.getAnalysisMethod( FbcBuilderDataTableAnalysis.class );
        FbcBuilderDataTableAnalysisParameters parameters = analysis.getParameters();
        parameters.setDiagramPath( COLLECTION_NAME.getChildPath( diagramName ) );
        parameters.setFbcResultPath( fbcDataPath );
        analysis.validateParameters();
        return analysis;
    }

    protected static final DataElementPath COLLECTION_NAME = DataElementPath.create( "databases/FluxBalance/Diagrams" );
    protected FolderVectorCollection fvc;
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository( "../data" );
        fvc = new FolderVectorCollection( "test", null );
        CollectionFactory.registerRoot( fvc );
    }

}
