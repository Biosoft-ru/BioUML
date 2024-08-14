package biouml.plugins.keynodes._test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.keynodes.DirectionEditor;
import biouml.plugins.keynodes.KeyNodeAnalysis;
import biouml.plugins.keynodes.KeyNodeVisualization;
import biouml.plugins.keynodes.KeyNodeVisualizationParameters;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Substance;
import junit.framework.Test;
import junit.framework.TestSuite;

public class KeyNodeVisualizationTest extends KeyNodeAnalysisTest
{

    public KeyNodeVisualizationTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( KeyNodeVisualizationTest.class.getName() );
        suite.addTest( new KeyNodeVisualizationTest( "testKeyNodeVisualizationFull" ) );
        suite.addTest( new KeyNodeVisualizationTest( "testKeyNodeVisualizationSelection" ) );
        suite.addTest( new KeyNodeVisualizationTest( "testKeyNodeVisualizationSeparate" ) );
        suite.addTest( new KeyNodeVisualizationTest( "testKeyNodeVisualizationWithAllPaths" ) );
        suite.addTest( new KeyNodeVisualizationTest( "testKeyNodeVisualizationNoReactants" ) );
        suite.addTest( new KeyNodeVisualizationTest( "testKeyNodeVisualizationEmptyInput" ) );
        suite.addTest( new KeyNodeVisualizationTest( "testKeyNodeVisualizationEmptyInputSelection" ) );
        return suite;
    }

    public void testKeyNodeVisualizationFull() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        KeyNodeVisualization knv = AnalysisMethodRegistry.getAnalysisMethod( KeyNodeVisualization.class );
        KeyNodeVisualizationParameters parameters = knv.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 2 );
        parameters.setRankColumn( "Score" );
        knv.validateParameters();

        Diagram[] result = knv.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 1, result.length );
        Diagram rD = result[0];
        assertEquals( "Incorrect diagram size", 16, rD.getSize() );
        assertEquals( "Incorrect nodes number", 9, rD.getNodes().length );

        StringSet substances = new StringSet( Arrays.asList( "E03", "E04", "E05", "E11", "E13", "E14" ) );
        checkNodesWithSpecificKernel( rD, substances, Substance.class );
        StringSet reactions = new StringSet( Arrays.asList( "X06", "X08", "X11" ) );
        checkNodesWithSpecificKernel( rD, reactions, Reaction.class );

        Map<String, String> relations = new HashMap<>();
        relations.put( "E03", TestsHub.REACTANT );
        relations.put( "E04", TestsHub.PRODUCT );
        checkReactionNode( rD, "X06", relations );

        relations = new HashMap<>();
        relations.put( "E04", TestsHub.REACTANT );
        relations.put( "E05", TestsHub.PRODUCT );
        checkReactionNode( rD, "X08", relations );

        relations = new HashMap<>();
        relations.put( "E11", TestsHub.REACTANT );
        relations.put( "E13", TestsHub.MODIFIER );
        relations.put( "E14", TestsHub.PRODUCT );
        checkReactionNode( rD, "X11", relations );
    }

    public void testKeyNodeVisualizationEmptyInput() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );
        for( String rdeName : knResult.getNameList() )
            knResult.remove( rdeName );
        checkResultTable( knResult, 0 );

        KeyNodeVisualization knv = AnalysisMethodRegistry.getAnalysisMethod( KeyNodeVisualization.class );
        KeyNodeVisualizationParameters parameters = knv.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 2 );
        parameters.setRankColumn( "Score" );
        knv.validateParameters();

        Diagram[] result = knv.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 1, result.length );
        Diagram rD = result[0];
        assertEquals( "Incorrect diagram size", 0, rD.getSize() );
        assertEquals( "Incorrect nodes number", 0, rD.getNodes().length );

        parameters.setSeparateResults( true );
        knv.validateParameters();

        result = knv.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 1, result.length );
        rD = result[0];
        assertEquals( "Incorrect diagram size", 0, rD.getSize() );
        assertEquals( "Incorrect nodes number", 0, rD.getNodes().length );
    }

    public void testKeyNodeVisualizationEmptyInputSelection() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        KeyNodeVisualization knv = AnalysisMethodRegistry.getAnalysisMethod( KeyNodeVisualization.class );
        KeyNodeVisualizationParameters parameters = knv.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 2 );
        parameters.setSelectedItems( new ArrayList<DataElement>() );
        knv.validateParameters();

        Diagram[] result = knv.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 1, result.length );
        Diagram rD = result[0];
        assertEquals( "Incorrect diagram size", 0, rD.getSize() );
        assertEquals( "Incorrect nodes number", 0, rD.getNodes().length );
    }

    public void testKeyNodeVisualizationSelection() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        List<DataElement> selectedItems = new ArrayList<>();
        selectedItems.add( knResult.get( "E03" ) );
        selectedItems.add( knResult.get( "E13" ) );

        KeyNodeVisualization knv = AnalysisMethodRegistry.getAnalysisMethod( KeyNodeVisualization.class );
        KeyNodeVisualizationParameters parameters = knv.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 2 );
        parameters.setSelectedItems( selectedItems );
        knv.validateParameters();

        Diagram[] result = knv.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 1, result.length );
        Diagram rD = result[0];
        assertEquals( "Incorrect diagram size", 12, rD.getSize() );
        assertEquals( "Incorrect nodes number", 7, rD.getNodes().length );

        StringSet substances = new StringSet( Arrays.asList( "E01", "E03", "E11", "E13", "E14" ) );
        checkNodesWithSpecificKernel( rD, substances, Substance.class );
        StringSet reactions = new StringSet( Arrays.asList( "X02", "X11" ) );
        checkNodesWithSpecificKernel( rD, reactions, Reaction.class );

        Map<String, String> relations = new HashMap<>();
        relations.put( "E01", TestsHub.REACTANT );
        relations.put( "E03", TestsHub.PRODUCT );
        checkReactionNode( rD, "X02", relations );

        relations = new HashMap<>();
        relations.put( "E11", TestsHub.REACTANT );
        relations.put( "E13", TestsHub.MODIFIER );
        relations.put( "E14", TestsHub.PRODUCT );
        checkReactionNode( rD, "X11", relations );
    }

    public void testKeyNodeVisualizationSeparate() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.DOWNSTREAM );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        KeyNodeVisualization knv = AnalysisMethodRegistry.getAnalysisMethod( KeyNodeVisualization.class );
        KeyNodeVisualizationParameters parameters = knv.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 2 );
        parameters.setRankColumn( "Score" );
        parameters.setSeparateResults( true );
        knv.validateParameters();

        Diagram[] result = knv.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 2, result.length );
        Diagram rD1 = result[0];
        assertEquals( "Incorrect diagram #1 size", 7, rD1.getSize() );
        assertEquals( "Incorrect nodes number in diagram #1", 4, rD1.getNodes().length );

        StringSet substances1 = new StringSet( Arrays.asList( "E11", "E13", "E14" ) );
        checkNodesWithSpecificKernel( rD1, substances1, Substance.class );
        StringSet reactions1 = new StringSet( Arrays.asList( "X11" ) );
        checkNodesWithSpecificKernel( rD1, reactions1, Reaction.class );

        Map<String, String> relations = new HashMap<>();
        relations.put( "E11", TestsHub.REACTANT );
        relations.put( "E13", TestsHub.MODIFIER );
        relations.put( "E14", TestsHub.PRODUCT );
        checkReactionNode( rD1, "X11", relations );

        Diagram rD2 = result[1];
        assertEquals( "Incorrect diagram #2 size", 9, rD2.getSize() );
        assertEquals( "Incorrect nodes number in diagram #2", 5, rD2.getNodes().length );

        StringSet substances2 = new StringSet( Arrays.asList( "E03", "E04", "E05" ) );
        checkNodesWithSpecificKernel( rD2, substances2, Substance.class );
        StringSet reactions2 = new StringSet( Arrays.asList( "X06", "X08" ) );
        checkNodesWithSpecificKernel( rD2, reactions2, Reaction.class );

        relations = new HashMap<>();
        relations.put( "E03", TestsHub.REACTANT );
        relations.put( "E04", TestsHub.PRODUCT );
        checkReactionNode( rD2, "X06", relations );

        relations = new HashMap<>();
        relations.put( "E04", TestsHub.REACTANT );
        relations.put( "E05", TestsHub.PRODUCT );
        checkReactionNode( rD2, "X08", relations );
    }

    public void testKeyNodeVisualizationWithAllPaths() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.UPSTREAM );
        analysis.getParameters().setMaxRadius( 4 );
        table.remove( "E03" );
        table.remove( "E05" );
        table.remove( "E07" );
        table.remove( "E11" );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        KeyNodeVisualization knv = AnalysisMethodRegistry.getAnalysisMethod( KeyNodeVisualization.class );
        KeyNodeVisualizationParameters parameters = knv.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 2 );
        parameters.setRankColumn( "Score" );
        parameters.setAddParticipants( true );
        parameters.setVisualizeAllPaths( true );
        knv.validateParameters();

        Diagram[] result = knv.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 1, result.length );
        Diagram rD = result[0];
        assertEquals( "Incorrect diagram size", 29, rD.getSize() );
        assertEquals( "Incorrect nodes number", 15, rD.getNodes().length );

        StringSet substances = new StringSet( Arrays.asList( "E01", "E02", "E03", "E04", "E06", "E11", "E12", "E13", "E14" ) );
        checkNodesWithSpecificKernel( rD, substances, Substance.class );
        StringSet reactions = new StringSet( Arrays.asList( "X01", "X02", "X03", "X05", "X06", "X11" ) );
        checkNodesWithSpecificKernel( rD, reactions, Reaction.class );

        Map<String, String> relations = new HashMap<>();
        relations.put( "E01", TestsHub.REACTANT );
        relations.put( "E02", TestsHub.PRODUCT );
        checkReactionNode( rD, "X01", relations );

        relations = new HashMap<>();
        relations.put( "E02", TestsHub.REACTANT );
        relations.put( "E04", TestsHub.PRODUCT );
        checkReactionNode( rD, "X05", relations );

        relations = new HashMap<>();
        relations.put( "E01", TestsHub.REACTANT );
        relations.put( "E03", TestsHub.PRODUCT );
        checkReactionNode( rD, "X02", relations );

        relations = new HashMap<>();
        relations.put( "E03", TestsHub.REACTANT );
        relations.put( "E04", TestsHub.PRODUCT );
        checkReactionNode( rD, "X06", relations );

        relations = new HashMap<>();
        relations.put( "E01", TestsHub.REACTANT );
        relations.put( "E06", TestsHub.PRODUCT );
        checkReactionNode( rD, "X03", relations );

        relations = new HashMap<>();
        relations.put( "E06", TestsHub.PRODUCT );
        relations.put( "E01", TestsHub.REACTANT );
        checkReactionNode( rD, "X03", relations );

        relations = new HashMap<>();
        relations.put( "E11", TestsHub.REACTANT );
        relations.put( "E12", TestsHub.REACTANT );
        relations.put( "E13", TestsHub.MODIFIER );
        relations.put( "E14", TestsHub.PRODUCT );
        checkReactionNode( rD, "X11", relations );
    }

    public void testKeyNodeVisualizationNoReactants() throws Exception
    {
        KeyNodeAnalysis analysis = initKeyNodeAnalysis( DirectionEditor.UPSTREAM );
        analysis.getParameters().setMaxRadius( 4 );
        table.remove( "E03" );
        table.remove( "E05" );
        table.remove( "E07" );
        table.remove( "E11" );
        TableDataCollection knResult = analysis.justAnalyzeAndPut();
        checkResultTable( knResult, 10 );

        KeyNodeVisualization knv = AnalysisMethodRegistry.getAnalysisMethod( KeyNodeVisualization.class );
        KeyNodeVisualizationParameters parameters = knv.getParameters();
        parameters.setKnResultPath( DataElementPath.create( knResult ) );
        parameters.setNumTopRanking( 2 );
        parameters.setRankColumn( "Score" );
        parameters.setVisualizeAllPaths( true );
        parameters.setAddParticipants( false );
        knv.validateParameters();

        Diagram[] result = knv.justAnalyzeAndPut();
        assertNotNull( result );
        assertEquals( 1, result.length );
        Diagram rD = result[0];
        assertEquals( "Incorrect diagram size", 18, rD.getSize() );
        assertEquals( "Incorrect nodes number", 10, rD.getNodes().length );

        StringSet substances = new StringSet( Arrays.asList( "E01", "E02", "E04", "E06", "E13", "E14" ) );
        checkNodesWithSpecificKernel( rD, substances, Substance.class );
        StringSet reactions = new StringSet( Arrays.asList( "X01", "X03", "X05", "X11" ) );
        checkNodesWithSpecificKernel( rD, reactions, Reaction.class );

        Map<String, String> relations = new HashMap<>();
        relations.put( "E01", TestsHub.REACTANT );
        relations.put( "E02", TestsHub.PRODUCT );
        checkReactionNode( rD, "X01", relations );

        relations = new HashMap<>();
        relations.put( "E02", TestsHub.REACTANT );
        relations.put( "E04", TestsHub.PRODUCT );
        checkReactionNode( rD, "X05", relations );

        relations = new HashMap<>();
        relations.put( "E01", TestsHub.REACTANT );
        relations.put( "E06", TestsHub.PRODUCT );
        checkReactionNode( rD, "X03", relations );

        relations = new HashMap<>();
        relations.put( "E13", TestsHub.MODIFIER );
        relations.put( "E14", TestsHub.PRODUCT );
        checkReactionNode( rD, "X11", relations );
    }

    protected void checkNodesWithSpecificKernel(Diagram d, StringSet expected, Class<? extends Base> kernelClass)
    {
        StringSet nodeNames = StreamEx.of( d.getNodes() ).map( Node::getKernel ).select( kernelClass ).map( Base::getName )
                .toCollection( StringSet::new );
        StringSet unexpected = nodeNames.stream().remove( expected::contains ).toCollection( StringSet::new );
        StringSet notFound = expected.stream().remove( nodeNames::contains ).toCollection( StringSet::new );
        assertEquals( "\nCheck '" + kernelClass.getName() + "' failed\nNot found nodes: " + notFound + "\nExtra nodes found: " + unexpected
                + "\nWas", expected, nodeNames );
    }

}
