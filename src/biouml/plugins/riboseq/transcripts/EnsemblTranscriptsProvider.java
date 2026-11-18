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
import java.util.Map;
import java.util.Set;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.SqlBatchLookup;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.bsa.Interval;

/**
 * Transcripts from an Ensembl core database.
 * <p>
 * Exons, translations and (for a subset) the transcripts themselves are read in batches of
 * {@link SqlBatchLookup#BATCH_SIZE} transcripts rather than with one query per transcript: profiles of
 * FindRegulatoryRegions showed the per-transcript round trips to MySQL as most of that analysis' time.
 * <p>
 * Where SQL leaves an order or a choice open, it is fixed here:
 * <ul>
 * <li>a subset id with several transcript rows (a stable id that is not unique) gives the row with the lowest
 * transcript_id - the one the old per-id query got first from the stable_id index;</li>
 * <li>exons are in exon_transcript.rank order (CDS offsets depend on it), as before;</li>
 * <li>several translations of one transcript (Ensembl core has at most one) are in translation_id order - the order
 * the old per-transcript query read them from the transcript_id index.</li>
 * </ul>
 * Without a subset, transcripts come in the order of the transcript query, which (as before) has no ORDER BY.
 */
public class EnsemblTranscriptsProvider extends TranscriptsProvider
{
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
            List<Transcript> result = new ArrayList<>();
            if( subset == null )
            {
                // Each batch is completed as soon as it is read, so only one batch of rows is held besides the result
                List<TranscriptRow> batch = new ArrayList<>();
                try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery( transcriptQuery ))
                {
                    while( rs.next() )
                    {
                        batch.add( new TranscriptRow( rs, 0 ) );
                        if( batch.size() == SqlBatchLookup.BATCH_SIZE )
                        {
                            loadTranscripts( con, batch, result );
                            batch.clear();
                        }
                    }
                }
                loadTranscripts( con, batch, result );
            }
            else
            {
                for( List<TranscriptRow> batch : SqlBatchLookup.chunks( findSubset( con, transcriptQuery ) ) )
                    loadTranscripts( con, batch, result );
            }
            return result;
        }
        catch( SQLException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    /**
     * The transcript rows of the subset, in its order (an id that occurs twice gives two rows, an unknown id none),
     * as one query per id returns them.
     * <p>
     * The ids {@link SqlBatchLookup#batchableIds} accepts are looked up in batches: "stable_id in (?,...)" with
     * FIELD(stable_id, ?,...) telling which id a row matched. A chunk with a row FIELD cannot attribute is looked up
     * one by one, like the other ids.
     */
    private List<TranscriptRow> findSubset(Connection con, String transcriptQuery) throws SQLException
    {
        Map<String, TranscriptRow> byId = new HashMap<>();
        Set<String> batched = new HashSet<>();
        for( List<String> chunk : SqlBatchLookup.chunks( SqlBatchLookup.batchableIds( subset ) ) )
        {
            String params = SqlBatchLookup.placeholders( chunk.size() );
            String select = "SELECT FIELD(transcript_stable_id.stable_id," + params + ")," + transcriptQuery.substring( "SELECT ".length() )
                    + " and transcript_stable_id.stable_id in (" + params + ")";
            Map<String, TranscriptRow> chunkRows = new HashMap<>();
            boolean attributed = true;
            try (PreparedStatement ps = con.prepareStatement( select ))
            {
                for( int i = 0; i < chunk.size(); i++ )
                {
                    ps.setString( i + 1, chunk.get( i ) );
                    ps.setString( chunk.size() + i + 1, chunk.get( i ) );
                }
                try (ResultSet rs = ps.executeQuery())
                {
                    while( attributed && rs.next() )
                    {
                        int index = SqlBatchLookup.fieldIndex( rs.getInt( 1 ), chunk.size() );
                        if( index < 0 )
                        {
                            attributed = false;
                            break;
                        }
                        TranscriptRow row = new TranscriptRow( rs, 1 );
                        chunkRows.merge( chunk.get( index ), row, (a, b) -> a.transcriptId <= b.transcriptId ? a : b );
                    }
                }
            }
            if( attributed )
            {
                byId.putAll( chunkRows );
                batched.addAll( chunk );
            }
        }

        List<TranscriptRow> rows = new ArrayList<>();
        try (PreparedStatement single = con
                .prepareStatement( transcriptQuery + " and transcript_stable_id.stable_id=? ORDER BY transcript_id" ))
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
                        rows.add( new TranscriptRow( rs, 0 ) );
                }
            }
        }
        return rows;
    }

    /** Exons and translations of the transcripts (at most one batch), two queries for the whole batch. */
    private void loadTranscripts(Connection con, List<TranscriptRow> rows, List<Transcript> result) throws SQLException
    {
        if( rows.isEmpty() )
            return;
        List<Integer> ids = new ArrayList<>( new LinkedHashSet<>( rows.stream().map( r -> r.transcriptId ).toList() ) );
        String in = " in (" + SqlBatchLookup.placeholders( ids.size() ) + ")";

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

    private static void setInts(PreparedStatement ps, List<Integer> values) throws SQLException
    {
        for( int i = 0; i < values.size(); i++ )
            ps.setInt( i + 1, values.get( i ) );
    }
}
