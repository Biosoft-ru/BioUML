package ru.biosoft.bsa.analysis._test;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa._test.BSATestUtils;
import ru.biosoft.bsa.analysis.GeneSetToTrack;
import ru.biosoft.bsa.analysis.GeneSetToTrackParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestGeneSetToTrack extends AbstractBioUMLTest
{
    public void testGeneSetToTrack() throws Exception
    {
        BSATestUtils.createRepository();
        VectorDataCollection<TableDataCollection> tables = new VectorDataCollection<>( "tables", TableDataCollection.class, null );
        VectorDataCollection<Track> tracks = new VectorDataCollection<>( "tracks", Track.class, null );
        CollectionFactory.registerRoot( tables );
        CollectionFactory.registerRoot( tracks );
        TableDataCollection genes = TableDataCollectionUtils.createTableDataCollection( tables, "genes" );
        TableDataCollectionUtils.addRow( genes, "ENSG00000004468", new Object[0] );
        TableDataCollectionUtils.addRow( genes, "ENSG00000006075", new Object[0] );
        CollectionFactoryUtils.save(genes);
        
        GeneSetToTrack analysis = new GeneSetToTrack( null, "" );
        GeneSetToTrackParameters parameters = analysis.getParameters();
        parameters.setFrom( -1000 );
        parameters.setTo( 100 );
        parameters.setSourcePath( genes.getCompletePath() );
        parameters.setDestPath( tracks.getCompletePath().getChildPath( "result" ) );
        
        Track track = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(track);
        assertEquals("id", ((DataCollection<?>)track).getInfo().getProperty( SqlTrack.LABEL_PROPERTY ));
        DataCollection<Site> allSites = track.getAllSites();
        assertNotNull(allSites);
        assertEquals(2, allSites.getSize());
        for(Site site : allSites)
        {
            if(site.getProperties().getValueAsString( GeneSetToTrack.GENE_NAME_PROPERTY ).equals( "CCL3" ))
            {
                assertEquals(31442619, site.getStart());
                assertEquals("17", site.getSequence().getName());
            } else if(site.getProperties().getValueAsString( GeneSetToTrack.GENE_NAME_PROPERTY ).equals( "CD38" ))
            {
                assertEquals(15388029, site.getStart());
                assertEquals("4", site.getSequence().getName());
            } else
            {
                fail("Unknown site: "+site.getProperties().getValueAsString( GeneSetToTrack.GENE_NAME_PROPERTY ));
            }
            assertEquals(1100, site.getLength());
        }
        
    }
}
