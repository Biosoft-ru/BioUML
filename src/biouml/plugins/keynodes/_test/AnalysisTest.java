package biouml.plugins.keynodes._test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.keynodes.KeyNodesResultVisualizer;
import biouml.plugins.keynodes.SaveHitsAction;
import biouml.plugins.keynodes.SaveNetworkAction;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Species;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public abstract class AnalysisTest extends AbstractBioUMLTest
{

    public AnalysisTest(String name)
    {
        super( name );
    }

    protected BioHubInfo bioHubInfo;
    protected TableDataCollection table;
    protected Species species;
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        DataCollection<?> root = CollectionFactory.createRepository( "../data/test/biouml/plugins/keynodes" );
        assertNotNull( "Root init failed", root );
        BioHubRegistry.addCollectionHub( root );
        species = Species.getSpecies( "Homo sapiens" );
        assertNotNull( "Cannot get species", species );
        bioHubInfo = BioHubRegistry.getBioHubInfo( "TestsHub" );
        assertNotNull( "Cannot get biohub", bioHubInfo );

        FolderVectorCollection fvc = new FolderVectorCollection( "test", null );
        CollectionFactory.registerRoot( fvc );
        table = TableDataCollectionUtils.createTableDataCollection( fvc, "tbl" );
        fvc.put( table );
        ReferenceTypeRegistry.setCollectionReferenceType( table, ReferenceTypeRegistry.getDefaultReferenceType() );
        table.getColumnModel().addColumn( "Name", String.class );
        table.getColumnModel().addColumn( "Comment", String.class );
        TableDataCollectionUtils.addRow( table, "E01", new Object[] {"E01", "1st"} );
        TableDataCollectionUtils.addRow( table, "E03", new Object[] {"E03", "3rd"} );
        TableDataCollectionUtils.addRow( table, "E04", new Object[] {"E04", "4th"} );
        TableDataCollectionUtils.addRow( table, "E05", new Object[] {"E05", "5th"} );

        TableDataCollectionUtils.addRow( table, "E07", new Object[] {"E07", "7th"} );
        TableDataCollectionUtils.addRow( table, "E11", new Object[] {"E11", "11th"} );
        TableDataCollectionUtils.addRow( table, "E14", new Object[] {"E14", "14th"} );

        ( (TestsHub)bioHubInfo.getBioHub() ).initCollections();
    }

    protected void checkResultRow(@Nonnull TableDataCollection result, @Nonnull String rdeName, @Nonnull String prefix, int inputElements,
            int totalElements, double score, @Nonnull String hitNames) throws Exception
    {
        RowDataElement rde = result.get( rdeName );
        assertNotNull( prefix + " is absent", rde );
        assertEquals( prefix + ": from input", inputElements, rde.getValues()[fromInputIndex] );
        assertEquals( prefix + ": total elements", totalElements, rde.getValues()[totalIndex] );
        assertEquals( prefix + ": score", score, (Double)rde.getValues()[scoreIndex], 0.000001 );
        assertEquals( prefix + ": hit names", hitNames, rde.getValues()[hitsIndex].toString() );
    }

    private int fromInputIndex;
    private int totalIndex;
    private int scoreIndex;
    private int hitsIndex;

    protected void checkResultTable(TableDataCollection result, int expectedSize)
    {
        assertNotNull( "Result table is absent", result );
        assertEquals( "Result table size", expectedSize, result.getSize() );

        ColumnModel columnModel = result.getColumnModel();
        fromInputIndex = result.columns().filter( column -> column.getValueClass() == Integer.class )
                .mapToInt( column -> columnModel.optColumnIndex( column.getName() ) ).findFirst().orElse( -1 );
        assertTrue( fromInputIndex >= 0 );
        totalIndex = fromInputIndex + 1;
        scoreIndex = columnModel.optColumnIndex( "Score" );
        assertTrue( "Score column missed", scoreIndex > 0 );
        hitsIndex = columnModel.optColumnIndex( "Hits" );
        assertTrue( "Hits column missed", hitsIndex > 0 );
    }

    protected void checkActions(TableDataCollection result, StringSet selected) throws Exception
    {
        BackgroundDynamicAction action = new KeyNodesResultVisualizer();
        assertTrue( "KeyNodesResultVisualizer not applicable", action.isApplicable( result ) );
        List<DataElement> selectedItems = result.stream().filter( rde -> selected.contains( rde.getName() ) )
                .collect( Collectors.toList() );
        action.validateParameters( result, selectedItems );
        assertNotNull( action.getJobControl( result, selectedItems, action.getProperties( result, selectedItems ) ) );

        action = new SaveHitsAction();
        assertTrue( "SaveHitsAction not applicable", action.isApplicable( result ) );
        selectedItems = result.stream().filter( rde -> selected.contains( rde.getName() ) ).collect( Collectors.toList() );
        action.validateParameters( result, selectedItems );
        assertNotNull( action.getJobControl( result, selectedItems, action.getProperties( result, selectedItems ) ) );

        action = new SaveNetworkAction();
        assertTrue( "SaveNetworkAction not applicable", action.isApplicable( result ) );
        selectedItems = result.stream().filter( rde -> selected.contains( rde.getName() ) ).collect( Collectors.toList() );
        action.validateParameters( result, selectedItems );
        assertNotNull( action.getJobControl( result, selectedItems, action.getProperties( result, selectedItems ) ) );
    }

    protected void checkReactionNode(Diagram diagram, @Nonnull String nodeName, Map<String, String> relations)
    {
        checkReactionNode( diagram, nodeName, relations.size(), relations );
    }

    private void checkReactionNode(Diagram diagram, @Nonnull String nodeName, int edgesNumber, Map<String, String> relations)
    {
        Node node = diagram.findNode( nodeName );
        assertNotNull( node );
        assertEquals( Reaction.class, node.getKernel().getClass() );
        assertEquals( edgesNumber, node.getEdges().length );
        for( Edge edge : node.getEdges() )
            assertEquals( ( (SpecieReference)edge.getKernel() ).getRole(), relations.get( edge.getOtherEnd( node ).getName() ) );
    }

}
