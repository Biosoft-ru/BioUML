package biouml.plugins.keynodes._test;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.keynodes.DirectionEditor;
import biouml.plugins.keynodes.KeyNodeAnalysis;
import biouml.plugins.keynodes.SaveHitsAnalysis;
import biouml.plugins.keynodes.SaveHitsAnalysisParameters;
import junit.framework.Test;
import junit.framework.TestSuite;

public class SaveHitsAnalysisTest extends KeyNodeAnalysisTest
{

    public SaveHitsAnalysisTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( SaveHitsAnalysisTest.class.getName() );
        suite.addTest( new SaveHitsAnalysisTest( "testSaveHitsAnalysisFull" ) );
        suite.addTest( new SaveHitsAnalysisTest( "testSaveHitsAnalysisSelection" ) );
        return suite;
    }

    public void testSaveHitsAnalysisFull() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        SaveHitsAnalysis sha = AnalysisMethodRegistry.getAnalysisMethod( SaveHitsAnalysis.class );
        SaveHitsAnalysisParameters parameters = sha.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 9 );
        parameters.setRankColumn( "Score" );
        sha.validateParameters();

        TableDataCollection result = sha.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 7, result.getSize() );

        checkHit( result, "E01" );
        checkHit( result, "E03" );
        checkHit( result, "E04" );
        checkHit( result, "E05" );
        checkHit( result, "E07" );
        checkHit( result, "E11" );
        checkHit( result, "E14" );
    }

    public void testSaveHitsAnalysisSelection() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        List<DataElement> selectedItems = new ArrayList<>();
        selectedItems.add( knResult.get( "E04" ) );
        selectedItems.add( knResult.get( "E10" ) );

        SaveHitsAnalysis sha = AnalysisMethodRegistry.getAnalysisMethod( SaveHitsAnalysis.class );
        SaveHitsAnalysisParameters parameters = sha.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 8 );
        parameters.setRankColumn( "Score" );
        parameters.setSelectedItems( selectedItems );
        sha.validateParameters();

        TableDataCollection result = sha.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 4, result.getSize() );

        checkHit( result, "E03" );
        checkHit( result, "E04" );
        checkHit( result, "E05" );
        checkHit( result, "E07" );
    }

    protected void checkHit(@Nonnull TableDataCollection result, String rdeName) throws Exception
    {
        Object[] expectedVals = table.get( rdeName ).getValues();
        RowDataElement rdeResult = result.get( rdeName );
        assertNotNull( rdeName + " is absent", rdeResult );
        assertEquals( rdeName + " values size ", expectedVals.length, rdeResult.getValues().length );
        for( int i = 0; i < expectedVals.length; i++ )
            assertEquals( "Value in column #" + i + " missmatch", expectedVals[i], rdeResult.getValues()[i] );
    }

}
