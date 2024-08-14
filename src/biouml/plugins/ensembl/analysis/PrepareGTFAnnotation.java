package biouml.plugins.ensembl.analysis;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.sql.Query;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.Iteration;

@ClassIcon("resources/PrepareGTFAnnotation.gif")
public class PrepareGTFAnnotation extends AnalysisMethodSupport<PrepareGTFAnnotation.Parameters>
{
    public static final PropertyDescriptor PD_EXON_ID = StaticDescriptor.create( "exon_id" );
    public static final PropertyDescriptor PD_EXON_NUMBER = StaticDescriptor.create( "exon_number" );
    public static final PropertyDescriptor PD_GENE_ID = StaticDescriptor.create( "gene_id" );
    public static final PropertyDescriptor PD_GENE_NAME = StaticDescriptor.create( "gene_name" );
    public static final PropertyDescriptor PD_GENE_BIOTYPE = StaticDescriptor.create( "gene_biotype" );
    public static final PropertyDescriptor PD_TRANSCRIPT_ID = StaticDescriptor.create( "transcript_id" );
    public static final PropertyDescriptor PD_TRANSCRIPT_NAME = StaticDescriptor.create( "transcript_name" );
    public static final PropertyDescriptor PD_PROTEIN_ID = StaticDescriptor.create( "protein_id" );
    public static final PropertyDescriptor PD_SOURCE = StaticDescriptor.create( "source" );
    public static final PropertyDescriptor PD_TSS_ID = StaticDescriptor.create( "tss_id" );
    public static final PropertyDescriptor PD_P_ID = StaticDescriptor.create( "p_id" );

    public PrepareGTFAnnotation(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, parameters.getOutputAnnotation().getName() );
        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, parameters.getEnsembl().getPrimarySequencesPath().toString() );
        properties.setProperty( Track.GENOME_ID_PROPERTY, parameters.getEnsembl().getGenomeBuild() );
        WritableTrack result = TrackUtils.createTrack( parameters.getOutputAnnotation().optParentCollection(), properties );


        Map<String, String> transcriptToTSS = findTssClusters();

        Connection con = parameters.getEnsembl().getConnection();

        try( Statement st = con.createStatement() )
        {
            String exonQuery = "SELECT seq_region.name, exon.seq_region_start, exon.seq_region_end, exon.seq_region_strand,"
                         + "       gene.stable_id, gene_xref.display_label, gene.biotype,"
                         + "       transcript.stable_id, transcript_xref.display_label, transcript.biotype,"
                         + "       exon_transcript.rank, exon.stable_id"
                         + " FROM gene JOIN seq_region using(seq_region_id)"
                         + "          JOIN transcript using(gene_id)"
                         + "          JOIN exon_transcript using(transcript_id)"
                         + "          JOIN exon using(exon_id)"
                         + "          JOIN xref gene_xref on(gene.display_xref_id=xref_id)"
                         + "          JOIN xref transcript_xref on(transcript.display_xref_id=transcript_xref.xref_id)";
                         //+ "WHERE gene.stable_id ='ENSG00000141510';
            try( ResultSet rs = st.executeQuery( exonQuery ) )
            {
                while( rs.next() )
                {
                    int i = 1;
                    String chr = rs.getString( i++ );
                    int from = rs.getInt( i++ );
                    int to = rs.getInt( i++ );
                    int strand = rs.getInt( i++ );
                    String geneId = rs.getString( i++ );
                    String geneName = rs.getString( i++ );
                    String geneBiotype = rs.getString( i++ );
                    String transcriptId = rs.getString( i++ );
                    String transcriptName = rs.getString( i++ );
                    String transcriptBiotype = rs.getString( i++ );
                    int exon_number = rs.getInt( i++ );
                    String exonId = rs.getString( i++ );

                    Site exon = new SiteImpl( null, chr, SiteType.TYPE_EXON, Basis.BASIS_ANNOTATED, strand == 1 ? from : to, to - from + 1, strand==1?StrandType.STRAND_PLUS:StrandType.STRAND_MINUS, null );

                    DynamicPropertySet dps = exon.getProperties();
                    dps.add( new DynamicProperty( PD_SOURCE, String.class, transcriptBiotype ) );
                    dps.add( new DynamicProperty( PD_EXON_ID, String.class, exonId ) );
                    dps.add( new DynamicProperty( PD_EXON_NUMBER, Integer.class, exon_number ) );
                    dps.add( new DynamicProperty( PD_GENE_ID, String.class, geneId ) );
                    dps.add( new DynamicProperty( PD_GENE_NAME, String.class, geneName ) );
                    dps.add( new DynamicProperty( PD_GENE_BIOTYPE, String.class, geneBiotype ) );
                    dps.add( new DynamicProperty( PD_TRANSCRIPT_ID, String.class, transcriptId ) );
                    dps.add( new DynamicProperty( PD_TRANSCRIPT_NAME, String.class, transcriptName ) );

                    String tssId = transcriptToTSS.get( transcriptId );
                    dps.add( new DynamicProperty( PD_TSS_ID, String.class, tssId ) );

                    result.addSite( exon );
                }
            }
        }


        try( Statement st = con.createStatement() )
        {
            String cdsQuery =  " SELECT "
                +"  seq_region.name, exon.seq_region_start, exon.seq_region_end, exon.seq_region_strand,"
                +"  seq_start, seq_end,"
                +"  translation.start_exon_id, translation.end_exon_id, exon.exon_id,"
                +"  gene.stable_id, gene_xref.display_label, gene.biotype,"
                +"  transcript.stable_id, transcript_xref.display_label, transcript.biotype, exon_transcript.rank, exon.stable_id,"
                +"  start.rank start_rank, end.rank end_rank"
                +" FROM gene"
                +"  JOIN seq_region using(seq_region_id)"
                +"  JOIN transcript using(gene_id) "
                +"  JOIN translation using(transcript_id)"
                +"  JOIN exon_transcript using(transcript_id)"
                +"  JOIN exon using(exon_id)"
                +"  JOIN xref gene_xref on(gene.display_xref_id=xref_id)"
                +"  JOIN xref transcript_xref on(transcript.display_xref_id=transcript_xref.xref_id)"
                +"  JOIN exon_transcript start on(start_exon_id=start.exon_id and start.transcript_id=translation.transcript_id)"
                +"  JOIN exon_transcript end on (end_exon_id=end.exon_id and end.transcript_id=translation.transcript_id)"
                //+" WHERE gene_xref.display_label='TP53'"
                +" HAVING start_rank<=exon_transcript.rank and exon_transcript.rank<=end_rank";
            try (ResultSet rs = st.executeQuery( cdsQuery ))
            {
                while(rs.next())
                {
                    int i = 1;
                    String chr = rs.getString( i++ );
                    int exonStart = rs.getInt( i++ );
                    int exonEnd = rs.getInt( i++ );
                    int strand = rs.getInt( i++ );
                    int offsetInFirstExon = rs.getInt( i++ );
                    int offsetInLastExon = rs.getInt( i++ );
                    int startExonId = rs.getInt( i++ );
                    int endExonId = rs.getInt( i++ );
                    int exonId = rs.getInt( i++ );
                    String geneId = rs.getString( i++ );
                    String geneName = rs.getString( i++ );
                    String geneBiotype = rs.getString( i++ );
                    String transcriptId = rs.getString( i++ );
                    String transcriptName = rs.getString( i++ );
                    String transcriptBiotype = rs.getString( i++ );
                    int exon_number = rs.getInt( i++ );
                    String exonStableId = rs.getString( i++ );

                    int cdsStart = exonStart;
                    int cdsEnd = exonEnd;
                    if( exonId == startExonId )
                    {

                        if( strand == 1 )
                            cdsStart = exonStart + offsetInFirstExon - 1;
                        else
                            cdsEnd = exonEnd - offsetInFirstExon + 1;
                    }
                    if( exonId == endExonId )
                    {
                        if( strand == 1 )
                            cdsEnd = exonStart + offsetInLastExon - 1;
                        else
                            cdsStart = exonEnd - offsetInLastExon + 1;
                    }
                    Site cds = new SiteImpl( null, chr, SiteType.TYPE_CDS, Basis.BASIS_ANNOTATED, strand == 1 ? cdsStart : cdsEnd, cdsEnd - cdsStart + 1, strand==1?StrandType.STRAND_PLUS:StrandType.STRAND_MINUS, null );

                    DynamicPropertySet dps = cds.getProperties();
                    dps.add( new DynamicProperty( PD_SOURCE, String.class, transcriptBiotype ) );
                    dps.add( new DynamicProperty( PD_EXON_ID, String.class, exonStableId ) );
                    dps.add( new DynamicProperty( PD_EXON_NUMBER, Integer.class, exon_number ) );
                    dps.add( new DynamicProperty( PD_GENE_ID, String.class, geneId ) );
                    dps.add( new DynamicProperty( PD_GENE_NAME, String.class, geneName ) );
                    dps.add( new DynamicProperty( PD_GENE_BIOTYPE, String.class, geneBiotype ) );
                    dps.add( new DynamicProperty( PD_TRANSCRIPT_ID, String.class, transcriptId ) );
                    dps.add( new DynamicProperty( PD_TRANSCRIPT_NAME, String.class, transcriptName ) );

                    result.addSite( cds );
                }
            }
        }
        result.finalizeAddition();
        parameters.getOutputAnnotation().save( result );
        return result;
    }

    private Map<String, String> findTssClusters()
    {
        final Map<String, String> result = new HashMap<>();

        final AtomicInteger clusterId = new AtomicInteger( 0 );

        final TableDataCollection tssGroupsTable = TableDataCollectionUtils.createTableDataCollection( parameters.getTssGroups() );
        tssGroupsTable.getColumnModel().addColumn( "chr", String.class );
        tssGroupsTable.getColumnModel().addColumn( "from", Integer.class );
        tssGroupsTable.getColumnModel().addColumn( "to", Integer.class );

        List<String> chromosomes = parameters.getEnsembl().getPrimarySequencesPath().getDataCollection().getNameList();
        jobControl.forCollection( chromosomes, new Iteration<String>() {
            @Override
            public boolean run(String chr)
            {
                Connection con = parameters.getEnsembl().getConnection();
                try(Statement st = con.createStatement())
                {
                    Query query = new Query("SELECT if(seq_region_strand=1,seq_region_start,seq_region_end) tss, stable_id "
                            + " FROM transcript JOIN seq_region using (seq_region_id) WHERE seq_region.name=$chr$ ORDER BY tss").str( chr );
                    try( ResultSet rs = st.executeQuery( query.get() ) )
                    {
                        if(!rs.next())
                            return true;
                        int tss = rs.getInt( 1 );
                        String transcriptId = rs.getString( 2 );
                        int clusterStart = tss;
                        int clusterEnd = tss;
                        List<String> cluster = new ArrayList<>();
                        cluster.add( transcriptId );
                        while(rs.next())
                        {
                            tss = rs.getInt( 1 );
                            transcriptId = rs.getString( 2 );
                            if(tss > clusterEnd + parameters.getTssDistance())
                            {
                                addCluster( cluster, chr, clusterStart, clusterEnd );
                                clusterStart = tss;
                                cluster.clear();
                            }
                            cluster.add( transcriptId );
                            clusterEnd = tss;
                        }
                        addCluster( cluster, chr, clusterStart, clusterEnd );
                    }
                }
                catch( SQLException e )
                {
                    throw ExceptionRegistry.translateException( e );
                }
                return true;
            }

            private void addCluster(List<String> transcripts, String chr, int clusterStart, int clusterEnd)
            {
                String curClusterId = "TSS" + clusterId.incrementAndGet();
                for(String transcript : transcripts)
                {
                    result.put( transcript, curClusterId );
                }
                TableDataCollectionUtils.addRow( tssGroupsTable, curClusterId, new Object[]{chr, clusterStart, clusterEnd}, true );
            }

        } );

        tssGroupsTable.finalizeAddition();
        parameters.getTssGroups().save( tssGroupsTable );

        return result;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private EnsemblDatabase ensembl;
        private int tssDistance;
        private DataElementPath tssGroups;
        private DataElementPath outputAnnotation;

        @PropertyName("Ensembl database")
        @PropertyDescription("Ensembl database to use")
        public EnsemblDatabase getEnsembl()
        {
            return ensembl;
        }
        public void setEnsembl(EnsemblDatabase ensembl)
        {
            Object oldValue = this.ensembl;
            this.ensembl = ensembl;
            firePropertyChange( "ensembl", oldValue, ensembl );
        }

        @PropertyName("TSS distance")
        @PropertyDescription("Maximal distance between neighboring transcription start sites in cluster")
        public int getTssDistance()
        {
            return tssDistance;
        }
        public void setTssDistance(int tssDistance)
        {
            Object oldValue = this.tssDistance;
            this.tssDistance = tssDistance;
            firePropertyChange( "tssDistance", oldValue, tssDistance );
        }

        @PropertyName("TSS groups")
        @PropertyDescription("Table describing TSS groups")
        public DataElementPath getTssGroups()
        {
            return tssGroups;
        }
        public void setTssGroups(DataElementPath tssGroups)
        {
            Object oldValue = this.tssGroups;
            this.tssGroups = tssGroups;
            firePropertyChange( "tssGroups", oldValue, tssGroups );
        }

        @PropertyName("Resulting annotation")
        @PropertyDescription("Resulting annotation of genome in GTF format")
        public DataElementPath getOutputAnnotation()
        {
            return outputAnnotation;
        }
        public void setOutputAnnotation(DataElementPath outputAnnotation)
        {
            Object oldValue = this.outputAnnotation;
            this.outputAnnotation = outputAnnotation;
            firePropertyChange( "outputAnnotation", oldValue, outputAnnotation );
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "ensembl", EnsemblDatabaseSelector.class );
            add( "tssDistance" );
            property( "tssGroups" ).outputElement( TableDataCollection.class ).add();
            property( "outputAnnotation" ).outputElement( Track.class ).add();
        }
    }
}
