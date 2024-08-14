package biouml.plugins.keynodes._test;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.keynodes.DirectionEditor;
import biouml.plugins.keynodes.KeyNodeAnalysis;
import biouml.plugins.keynodes.SaveNetworkAnalysis;
import biouml.plugins.keynodes.SaveNetworkAnalysisParameters;

public class SaveNetworkAnalysisTest extends KeyNodeAnalysisTest
{

    public SaveNetworkAnalysisTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( SaveNetworkAnalysisTest.class.getName() );
        suite.addTest( new SaveNetworkAnalysisTest( "testSaveNetworkAnalysisFull" ) );
        suite.addTest( new SaveNetworkAnalysisTest( "testSaveNetworkAnalysisFullSeparate" ) );
        suite.addTest( new SaveNetworkAnalysisTest( "testSaveNetworkAnalysisSelection" ) );
        suite.addTest( new SaveNetworkAnalysisTest( "testSaveNetworkAnalysisSelectionSeparate" ) );
        return suite;
    }

    public void testSaveNetworkAnalysisFull() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        SaveNetworkAnalysis sna = AnalysisMethodRegistry.getAnalysisMethod( SaveNetworkAnalysis.class );
        SaveNetworkAnalysisParameters parameters = sna.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 5 );
        parameters.setRankColumn( "Score" );
        sna.validateParameters();

        TableDataCollection[] results = sna.justAnalyzeAndPut();
        assertNotNull( results );
        assertEquals( 1, results.length );
        TableDataCollection result = results[0];
        assertEquals( 9, result.getSize() );
        checkNetwork( result, "E01", "hit" );
        checkNetwork( result, "E03", "hit, master" );
        checkNetwork( result, "E04", "hit, master" );
        checkNetwork( result, "E05", "hit, master" );
        checkNetwork( result, "E07", "hit" );
        checkNetwork( result, "E09", "network element" );
        checkNetwork( result, "E11", "hit, master" );
        checkNetwork( result, "E13", "master" );
        checkNetwork( result, "E14", "hit" );
    }

    public void testSaveNetworkAnalysisFullSeparate() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        SaveNetworkAnalysis sna = AnalysisMethodRegistry.getAnalysisMethod( SaveNetworkAnalysis.class );
        SaveNetworkAnalysisParameters parameters = sna.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 5 );
        parameters.setSeparateResults( true );
        parameters.setRankColumn( "Score" );
        sna.validateParameters();

        TableDataCollection[] results = sna.justAnalyzeAndPut();
        assertNotNull( results );
        assertEquals( 5, results.length );
        for( TableDataCollection tdc : results )
        {
            String masterName = tdc.getName().substring( tdc.getName().lastIndexOf( '(' ) + 1, tdc.getName().lastIndexOf( ')' ) );
            switch( masterName )
            {
                case "E03":
                    assertEquals( "Invalid size for " + masterName,  2, tdc.getSize() );
                    checkNetwork( tdc, "E01", "hit" );
                    checkNetwork( tdc, "E03", "hit, master" );
                    break;
                case "E04":
                    assertEquals( "Invalid size for " + masterName, 3, tdc.getSize() );
                    checkNetwork( tdc, "E03", "hit" );
                    checkNetwork( tdc, "E04", "hit, master" );
                    checkNetwork( tdc, "E05", "hit" );
                    break;
                case "E05":
                    assertEquals( "Invalid size for " + masterName, 2, tdc.getSize() );
                    checkNetwork( tdc, "E04", "hit" );
                    checkNetwork( tdc, "E05", "hit, master" );
                    break;
                case "E11":
                    assertEquals( "Invalid size for " + masterName, 5, tdc.getSize() );
                    checkNetwork( tdc, "E07", "hit" );
                    checkNetwork( tdc, "E09", "network element" );
                    checkNetwork( tdc, "E11", "hit, master" );
                    checkNetwork( tdc, "E13", "network element" );
                    checkNetwork( tdc, "E14", "hit" );
                    break;
                case "E13":
                    assertEquals( "Invalid size for " + masterName, 3, tdc.getSize() );
                    checkNetwork( tdc, "E11", "hit" );
                    checkNetwork( tdc, "E13", "master" );
                    checkNetwork( tdc, "E14", "hit" );
                    break;
                default:
                    throw new Exception( "Network with unexpected master molecule '" + masterName + "' was found." );
            }
        }
    }

    public void testSaveNetworkAnalysisSelection() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        List<DataElement> selectedItems = new ArrayList<>();
        selectedItems.add( knResult.get( "E03" ) );
        selectedItems.add( knResult.get( "E13" ) );

        SaveNetworkAnalysis sna = AnalysisMethodRegistry.getAnalysisMethod( SaveNetworkAnalysis.class );
        SaveNetworkAnalysisParameters parameters = sna.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 5 );
        parameters.setRankColumn( "Score" );
        parameters.setSelectedItems( selectedItems );
        sna.validateParameters();

        TableDataCollection[] results = sna.justAnalyzeAndPut();
        assertNotNull( results );
        assertEquals( 1, results.length );
        TableDataCollection result = results[0];
        assertEquals( 5, result.getSize() );
        checkNetwork( result, "E01", "hit" );
        checkNetwork( result, "E03", "hit, master" );
        checkNetwork( result, "E11", "hit" );
        checkNetwork( result, "E13", "master" );
        checkNetwork( result, "E14", "hit" );
    }

    public void testSaveNetworkAnalysisSelectionSeparate() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        List<DataElement> selectedItems = new ArrayList<>();
        selectedItems.add( knResult.get( "E03" ) );
        selectedItems.add( knResult.get( "E13" ) );

        SaveNetworkAnalysis sna = AnalysisMethodRegistry.getAnalysisMethod( SaveNetworkAnalysis.class );
        SaveNetworkAnalysisParameters parameters = sna.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 5 );
        parameters.setSeparateResults( true );
        parameters.setRankColumn( "Score" );
        parameters.setSelectedItems( selectedItems );
        sna.validateParameters();

        TableDataCollection[] results = sna.justAnalyzeAndPut();
        assertNotNull( results );
        assertEquals( 2, results.length );
        for( TableDataCollection tdc : results )
        {
            String masterName = tdc.getName().substring( tdc.getName().lastIndexOf( '(' ) + 1, tdc.getName().lastIndexOf( ')' ) );
            switch( masterName )
            {
                case "E03":
                    assertEquals( "Invalid size for " + masterName, 2, tdc.getSize() );
                    checkNetwork( tdc, "E01", "hit" );
                    checkNetwork( tdc, "E03", "hit, master" );
                    break;
                case "E13":
                    assertEquals( "Invalid size for " + masterName, 3, tdc.getSize() );
                    checkNetwork( tdc, "E11", "hit" );
                    checkNetwork( tdc, "E13", "master" );
                    checkNetwork( tdc, "E14", "hit" );
                    break;
                default:
                    throw new Exception( "Network with unexpected master molecule '" + masterName + "' was found." );
            }
        }
    }

    protected void checkNetwork(@Nonnull TableDataCollection result, String rdeName, String role) throws Exception
    {
        RowDataElement rdeResult = result.get( rdeName );
        assertNotNull( rdeName + " is absent", rdeResult );
        Object[] values = rdeResult.getValues();
        assertEquals( rdeName + " values size ", 2, values.length );
        assertEquals( "Name", rdeName, values[0] );
        assertEquals( "Role", role, values[1] );
    }

}
