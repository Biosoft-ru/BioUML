package biouml.plugins.riboseq._test;


import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.riboseq.CountReadsInTranscripts;
import biouml.plugins.riboseq.CountReadsInTranscripts.Parameters;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestCountReadsInTranscripts extends AbstractBioUMLTest
{
    public void testGRIK1Gene() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        CollectionFactory.createRepository( "../data/test/biouml/plugins/ensembl" );
        CollectionFactory.registerRoot( new VectorDataCollection<>( "live" ) );
        
        CountReadsInTranscripts analysis = new CountReadsInTranscripts( null, "testing" );
        Parameters parameters = analysis.getParameters();
        parameters.setBamTrack( DataElementPath.create( "test_data/GRIK3.bam" ) );
        parameters.getTranscriptSet().setEnsembl( new EnsemblDatabase( DataElementPath.create( "databases/EnsemblHuman73_37" ) ) );
        
        DataElementPath subsetPath = DataElementPath.create( "live/transcripts" );
        TableDataCollection subset = TableDataCollectionUtils.createTableDataCollection( subsetPath );
        TableDataCollectionUtils.addRow( subset, "ENST00000373091", new Object[0] );
        TableDataCollectionUtils.addRow( subset, "ENST00000373093", new Object[0] );
        TableDataCollectionUtils.addRow( subset, "ENST00000462621", new Object[0] );
        TableDataCollectionUtils.addRow( subset, "ENST00000479620", new Object[0] );
        subsetPath.save( subset );
        parameters.getTranscriptSet().setTranscriptSubset( subsetPath );
        
        parameters.setOutputTable( DataElementPath.create( "live/resulting table" ) );
        analysis.getJobControl().run();
        
        TableDataCollection result = parameters.getOutputTable().getDataElement( TableDataCollection.class );
        assertEquals( 4, result.getSize() );
        
        RowDataElement row = result.get( "ENST00000462621" );
        assertNotNull( row );
        assertEquals( 4, row.getValue( "Count" ) );
        
        row = result.get( "ENST00000373093" );
        assertNotNull( row );
        assertEquals( 49, row.getValue( "Count" ) );
        
        row = result.get( "ENST00000479620" );
        assertNotNull( row );
        assertEquals( 2, row.getValue( "Count" ) );
        
        row = result.get( "ENST00000373091" );
        assertNotNull( row );
        assertEquals( 158, row.getValue( "Count" ) );
    }
    
    public void testGRIK1CDS() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        CollectionFactory.createRepository( "../data/test/biouml/plugins/ensembl" );
        CollectionFactory.registerRoot( new VectorDataCollection<>( "live" ) );
        
        CountReadsInTranscripts analysis = new CountReadsInTranscripts( null, "testing" );
        Parameters parameters = analysis.getParameters();
        parameters.setBamTrack( DataElementPath.create( "test_data/GRIK3.bam" ) );
        parameters.getTranscriptSet().setEnsembl( new EnsemblDatabase( DataElementPath.create( "databases/EnsemblHuman73_37" ) ) );
        
        DataElementPath subsetPath = DataElementPath.create( "live/transcripts" );
        TableDataCollection subset = TableDataCollectionUtils.createTableDataCollection( subsetPath );
        TableDataCollectionUtils.addRow( subset, "ENST00000373091", new Object[0] );
        TableDataCollectionUtils.addRow( subset, "ENST00000373093", new Object[0] );
        TableDataCollectionUtils.addRow( subset, "ENST00000462621", new Object[0] );
        TableDataCollectionUtils.addRow( subset, "ENST00000479620", new Object[0] );
        subsetPath.save( subset );
        parameters.getTranscriptSet().setTranscriptSubset( subsetPath );
        parameters.setOnlyOverlappingCDS( true );
        parameters.setOutputTable( DataElementPath.create( "live/resulting table" ) );
        
        analysis.getJobControl().run();
        
        TableDataCollection result = parameters.getOutputTable().getDataElement( TableDataCollection.class );
        assertEquals( 2, result.getSize() );
        
        RowDataElement row = result.get( "ENST00000373093" );
        assertNotNull( row );
        assertEquals( 46, row.getValue( "Count" ) );
        
        row = result.get( "ENST00000373091" );
        assertNotNull( row );
        assertEquals( 49, row.getValue( "Count" ) );
    }
    
}
