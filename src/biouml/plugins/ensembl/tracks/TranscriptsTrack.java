package biouml.plugins.ensembl.tracks;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.util.LazyValue;
import ru.biosoft.util.bean.StaticDescriptor;

public class TranscriptsTrack extends SQLBasedEnsemblTrack
{
    private static final PropertyDescriptor ID_PD = StaticDescriptor.create("id");
    private static final PropertyDescriptor EXONS_PD = StaticDescriptor.create("exons");
    private static final PropertyDescriptor TRANSLATION_PD = StaticDescriptor.create("translation");
    
    public TranscriptsTrack(DataCollection<?> origin)
    {
        super( "Transcripts", origin, 300000000, "transcript" );
        viewBuilder = new TranscriptsTrackViewBuilder();
    }

    @Override
    protected String getSliceQueryTemplate()
    {
        return "SELECT t.transcript_id,seq_region_start,seq_region_end,biotype,seq_region_strand,ts.stable_id "
                + "FROM {table} t JOIN transcript_stable_id ts USING(transcript_id) WHERE {range}";
    }

    @Override
    protected String getSiteQueryTemplate()
    {
        return getSliceQueryTemplate() + " AND transcript_id={site}";
    }

    @Override
    protected Site createSite(ResultSet rs, Sequence sequence) throws Exception
    {
        Site result;
        String name = rs.getString(1);
        int length = rs.getInt(3) - rs.getInt(2) + 1;
        int strand = rs.getInt(5) == 1 ? Site.STRAND_PLUS : Site.STRAND_MINUS;
        int start = rs.getInt(5) == 1 ? rs.getInt(2) : rs.getInt(3);
        result = new SiteImpl(null, name, rs.getString(4), Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY, strand, sequence,
                new DynamicPropertySetAsMap());
        result.getProperties().add(new DynamicProperty(ID_PD, String.class, rs.getString(6)));
        fillTranscriptProperties(result, name);
        return result;
    }

    private void fillTranscriptProperties(Site result, String name)
    {
        result.getProperties().add( new ExonsProperty( name ) );
        if(result.getType().equals( "protein_coding" ))
            result.getProperties().add( new TranslationProperty( name ) );
    }
    
    private class ExonsProperty extends DynamicProperty
    {
        private String transcriptId;
        private LazyValue<Interval[]> lazyValue = new LazyValue<>( ()->{
            Query query = new Query(
                    "SELECT seq_region_start,seq_region_end FROM exon join exon_transcript using(exon_id) WHERE transcript_id=$id$ ORDER BY exon_transcript.rank" )
                    .str("id", transcriptId);
            return SqlUtil.stream(getConnection(), query, rs -> new Interval(rs.getInt(1), rs.getInt(2))).toArray(Interval[]::new);
        } );
        
        
        public ExonsProperty(String transcriptId)
        {
            super( EXONS_PD, Interval[].class );
            this.transcriptId = transcriptId;
        }

        @Override
        public Interval[] getValue()
        {
            return lazyValue.get();
        }
    }
    
    private class TranslationProperty extends DynamicProperty
    {
        private String transcriptId;
        
        private LazyValue<Translation> lazyValue = new LazyValue<>(()->{
            String queryStr = "SELECT seq_start, e1.rank, seq_end, e2.rank FROM translation t "
            + "JOIN exon_transcript e1 ON(t.transcript_id=e1.transcript_id AND t.start_exon_id=e1.exon_id) "
            + "JOIN exon_transcript e2 ON(t.transcript_id=e2.transcript_id AND t.end_exon_id=e2.exon_id) "
            + "WHERE t.transcript_id=$id$";
            Query query = new Query(queryStr).str("id", transcriptId);
            return SqlUtil.stream(getConnection(), query, rs->new Translation(rs.getInt(2), rs.getInt(1), rs.getInt(4), rs.getInt(3)) )
                    .findAny().get();
        });
        
        public TranslationProperty(String transcriptId)
        {
            super( TRANSLATION_PD, Translation.class );
            this.transcriptId=transcriptId;
        }
        
        @Override
        public Object getValue()
        {
            return lazyValue.get();
        }
    }
    
    public static class Translation
    {
        public final int firstExonRank;
        public final int offsetInFirstExon;
        public final int lastExonRank;
        public final int offsetInLastExon;
        
        public Translation(int firstExonRank, int offsetInFirstExon, int lastExonRank, int offsetInLastExon)
        {
            this.firstExonRank = firstExonRank;
            this.offsetInFirstExon = offsetInFirstExon;
            this.lastExonRank = lastExonRank;
            this.offsetInLastExon = offsetInLastExon;
        }
        
        @Override
        public String toString()
        {
            return firstExonRank + ":" + offsetInFirstExon + "-" + lastExonRank + ":" + offsetInLastExon;
        }
    }

}
