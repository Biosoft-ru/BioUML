package biouml.plugins.riboseq.transcripts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.bsa.Interval;

/**
 * Transcripts from an Ensembl core database.
 * <p>
 * Exons, translations and (for a subset) the transcripts themselves are read in batches of {@link #BATCH_SIZE}
 * transcripts rather than with one query per transcript: profiles of FindRegulatoryRegions showed the per-transcript
 * round trips to MySQL as most of that analysis' time. The SQL and the result are the same as before.
 */
public class EnsemblTranscriptsProvider extends TranscriptsProvider
{
    /** Number of transcripts looked up by one batched query. */
    static final int BATCH_SIZE = 500;

    private static final String TRANSCRIPT_QUERY = "SELECT transcript_id, transcript_stable_id.stable_id, seq_region.name, seq_region_start, seq_region_end, seq_region_strand FROM transcript JOIN transcript_stable_id USING(transcript_id) JOIN seq_region USING(seq_region_id) JOIN coord_system USING(coord_system_id) WHERE coord_system.rank=1";

    private SqlConnectionHolder connectionHolder;

    public EnsemblTranscriptsProvider(SqlConnectionHolder connectionHolder)
    {
        this.connectionHolder = connectionHolder;
    }

    /** One row of the transcript query. */
    private static class TranscriptRow
    {
        final int transcriptId;
        final String name, chr;
        final int start, end, strand;

        TranscriptRow(ResultSet rs) throws SQLException
        {
            this( rs, 0 );
        }

        /** @param offset number of columns before the transcript query's ones */
        TranscriptRow(ResultSet rs, int offset) throws SQLException
        {
            transcriptId = rs.getInt( offset + 1 );
            name = rs.getString( offset + 2 );
            chr = rs.getString( offset + 3 );
            start = rs.getInt( offset + 4 );
            end = rs.getInt( offset + 5 );
            strand = rs.getInt( offset + 6 );
        }
    }

    @Override
    public List<Transcript> getTranscripts()
    {
        String transcriptQuery = TRANSCRIPT_QUERY;
        if(onlyProteinCoding)
            transcriptQuery += " and transcript.biotype='protein_coding'";
        Connection con = connectionHolder.getConnection();
        try
        {
            List<TranscriptRow> rows = new ArrayList<>();
            if( subset == null )
                try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery( transcriptQuery );)
                {
                    while( rs.next() )
                        rows.add( new TranscriptRow( rs ) );
                }
            else
                rows = findSubset( con, transcriptQuery );
            List<Transcript> result = new ArrayList<>( rows.size() );
            for( int from = 0; from < rows.size(); from += BATCH_SIZE )
                loadTranscripts( con, rows.subList( from, Math.min( from + BATCH_SIZE, rows.size() ) ), result );
            return result;
        }
        catch( SQLException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    /**
     * The transcript rows of the subset, in its order (an id that occurs twice gives two rows, an unknown id none),
     * as one query per id returned them.
     * <p>
     * The ids are looked up in batches: "stable_id in (?,...)" with FIELD(stable_id, ?,...) telling which id a row
     * matched. Both compare with the same collation as the per-id "stable_id=?", so each row goes to the id that
     * query would have returned it for. Ids two of which could match the same row (non-ASCII ids, ids equal
     * ignoring case or trailing spaces) are looked up one by one as before, as FIELD reports only the first of them.
     */
    private List<TranscriptRow> findSubset(Connection con, String transcriptQuery) throws SQLException
    {
        Map<String, Integer> spellings = new HashMap<>();
        for( String id : new LinkedHashSet<>( subset ) )
            spellings.merge( normalize( id ), 1, Integer::sum );
        List<String> ids = new ArrayList<>();
        for( String id : new LinkedHashSet<>( subset ) )
            if( isAscii( id ) && spellings.get( normalize( id ) ) == 1 )
                ids.add( id );

        Map<String, TranscriptRow> byId = new HashMap<>();
        Set<String> batched = new HashSet<>( ids );
        for( int from = 0; from < ids.size(); from += BATCH_SIZE )
        {
            List<String> chunk = ids.subList( from, Math.min( from + BATCH_SIZE, ids.size() ) );
            String params = placeholders( chunk.size() );
            String select = "SELECT FIELD(transcript_stable_id.stable_id," + params + ")," + transcriptQuery.substring( "SELECT ".length() )
                    + " and transcript_stable_id.stable_id in (" + params + ")";
            try (PreparedStatement ps = con.prepareStatement( select ))
            {
                for( int i = 0; i < chunk.size(); i++ )
                {
                    ps.setString( i + 1, chunk.get( i ) );
                    ps.setString( chunk.size() + i + 1, chunk.get( i ) );
                }
                try (ResultSet rs = ps.executeQuery())
                {
                    while( rs.next() )
                        byId.putIfAbsent( chunk.get( rs.getInt( 1 ) - 1 ), new TranscriptRow( rs, 1 ) ); // the per-id query took the first row too
                }
            }
        }

        List<TranscriptRow> rows = new ArrayList<>();
        try (PreparedStatement single = con.prepareStatement( transcriptQuery + " and transcript_stable_id.stable_id=?" ))
        {
            for( String id : subset )
            {
                if( batched.contains( id ) )
                {
                    TranscriptRow row = byId.get( id );
                    if( row != null )
                        rows.add( row );
                    continue;
                }
                single.setString( 1, id );
                try (ResultSet rs = single.executeQuery())
                {
                    if( rs.next() )
                        rows.add( new TranscriptRow( rs ) );
                }
            }
        }
        return rows;
    }

    /** Exons and translations of the transcripts, two queries for the whole batch. */
    private void loadTranscripts(Connection con, List<TranscriptRow> rows, List<Transcript> result) throws SQLException
    {
        List<Integer> ids = new ArrayList<>( new LinkedHashSet<>( rows.stream().map( r -> r.transcriptId ).toList() ) );
        String in = " in (" + placeholders( ids.size() ) + ")";

        Map<Integer, List<int[]>> exons = new HashMap<>(); // transcript -> {exonId, start, end} in rank order
        try (PreparedStatement ps = con.prepareStatement( "SELECT exon_transcript.transcript_id, exon_id, seq_region_start, seq_region_end FROM exon JOIN exon_transcript using(exon_id) WHERE exon_transcript.transcript_id"
                + in + " ORDER BY exon_transcript.transcript_id, exon_transcript.rank" ))
        {
            setInts( ps, ids );
            try (ResultSet rs = ps.executeQuery())
            {
                while( rs.next() )
                    exons.computeIfAbsent( rs.getInt( 1 ), k -> new ArrayList<>() ).add( new int[] {rs.getInt( 2 ), rs.getInt( 3 ), rs.getInt( 4 )} );
            }
        }

        Map<Integer, List<int[]>> translations = new HashMap<>(); // transcript -> {seqStart, startExonId, seqEnd, endExonId}
        if( loadCDS )
            try (PreparedStatement ps = con.prepareStatement( "SELECT transcript_id, seq_start, start_exon_id, seq_end, end_exon_id FROM translation WHERE transcript_id"
                    + in + " ORDER BY transcript_id, translation_id" ))
            {
                setInts( ps, ids );
                try (ResultSet rs = ps.executeQuery())
                {
                    while( rs.next() )
                        translations.computeIfAbsent( rs.getInt( 1 ), k -> new ArrayList<>() )
                                .add( new int[] {rs.getInt( 2 ), rs.getInt( 3 ), rs.getInt( 4 ), rs.getInt( 5 )} );
                }
            }

        for( TranscriptRow row : rows )
            result.add( buildTranscript( row, exons.getOrDefault( row.transcriptId, Collections.emptyList() ),
                    translations.getOrDefault( row.transcriptId, Collections.emptyList() ) ) );
    }

    private Transcript buildTranscript(TranscriptRow row, List<int[]> exonRows, List<int[]> translationRows)
    {
        List<Interval> exonLocations = new ArrayList<>();
        List<Integer> exonIds = new ArrayList<>();
        for( int[] exon : exonRows )
        {
            exonLocations.add( new Interval( exon[1] - 1, exon[2] - 1 ) );
            exonIds.add( exon[0] );
        }

        List<Interval> cdsLocations = new ArrayList<>();
        if( loadCDS )
        {
            for( int[] translation : translationRows )
            {
                int offsetInFirstExon = translation[0];
                int firstExonId = translation[1];
                int offsetInLastExon = translation[2];
                int lastExonId = translation[3];

                int cdsStart = -1, cdsEnd = -1;
                int length = 0;
                for( int i = 0; i < exonLocations.size(); i++ )
                {
                    int id = exonIds.get( i );
                    if( id == firstExonId )
                        cdsStart = length + offsetInFirstExon - 1;
                    if( id == lastExonId )
                        cdsEnd = length + offsetInLastExon - 1;
                    length += exonLocations.get( i ).getLength();
                }
                cdsLocations.add( new Interval( cdsStart, cdsEnd ) );
            }
        }

        Collections.sort( exonLocations );

        return new Transcript( row.name, row.chr, new Interval( row.start - 1, row.end - 1 ), row.strand == 1, exonLocations, cdsLocations );
    }

    private static String placeholders(int count)
    {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < count; i++ )
            sb.append( i == 0 ? "?" : ",?" );
        return sb.toString();
    }

    private static void setInts(PreparedStatement ps, List<Integer> values) throws SQLException
    {
        for( int i = 0; i < values.size(); i++ )
            ps.setInt( i + 1, values.get( i ) );
    }

    private static String normalize(String id)
    {
        int end = id.length();
        while( end > 0 && id.charAt( end - 1 ) == ' ' )
            end--;
        return id.substring( 0, end ).toLowerCase( Locale.ROOT );
    }

    private static boolean isAscii(String s)
    {
        for( int i = 0; i < s.length(); i++ )
            if( s.charAt( i ) > 127 )
                return false;
        return true;
    }
}
