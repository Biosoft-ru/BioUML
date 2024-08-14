package biouml.plugins.riboseq.transcripts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.bsa.Interval;

public class EnsemblTranscriptsProvider extends TranscriptsProvider
{
    private SqlConnectionHolder connectionHolder;

    public EnsemblTranscriptsProvider(SqlConnectionHolder connectionHolder)
    {
        this.connectionHolder = connectionHolder;
    }

    @Override
    public List<Transcript> getTranscripts()
    {
        List<Transcript> result = new ArrayList<>();
        String transcriptQuery = "SELECT transcript_id, transcript_stable_id.stable_id, seq_region.name, seq_region_start, seq_region_end, seq_region_strand FROM transcript JOIN transcript_stable_id USING(transcript_id) JOIN seq_region USING(seq_region_id) JOIN coord_system USING(coord_system_id) WHERE coord_system.rank=1";
        if(onlyProteinCoding)
            transcriptQuery += " and transcript.biotype='protein_coding'";
        String exonQuery = "SELECT exon_id, seq_region_start, seq_region_end  FROM exon JOIN exon_transcript using(exon_id) WHERE transcript_id=? ORDER BY exon_transcript.rank";
        String translationQuery = "SELECT seq_start, start_exon_id, seq_end, end_exon_id FROM translation WHERE transcript_id=?";
        Connection con = connectionHolder.getConnection();
        try(
            PreparedStatement exonStatement = con.prepareStatement( exonQuery );
            PreparedStatement translationStatement = con.prepareStatement( translationQuery ))
        {
            if( subset == null )
                try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery( transcriptQuery );)
                {
                    while( rs.next() )
                    {
                        Transcript transcript = loadTranscript( rs, exonStatement, translationStatement );
                        result.add( transcript );
                    }
                }
            else
                try (PreparedStatement transcriptStatement = con
                        .prepareStatement( transcriptQuery + " and transcript_stable_id.stable_id=?" ))
                {
                    for(String id : subset)
                    {
                        transcriptStatement.setString( 1, id );
                        try(ResultSet rs = transcriptStatement.executeQuery())
                        {
                            if( rs.next() )
                            {
                                Transcript transcript = loadTranscript( rs, exonStatement, translationStatement );
                                result.add( transcript );
                            }
                        }
                    }
                }
        }
        catch( SQLException e )
        {
            throw ExceptionRegistry.translateException( e );
        }

        return result ;
    }

    private Transcript loadTranscript(ResultSet rs, PreparedStatement exonStatement, PreparedStatement translationStatement)
            throws SQLException
    {
        int transcriptId = rs.getInt( 1 );
        String transcriptName = rs.getString( 2 );
        String chr = rs.getString( 3 );
        int start = rs.getInt( 4 );
        int end = rs.getInt( 5 );
        int strand = rs.getInt( 6 );

        List<Interval> exonLocations = new ArrayList<>();
        List<Integer> exonIds = new ArrayList<>();
        exonStatement.setInt( 1, transcriptId );
        try(ResultSet exonRS = exonStatement.executeQuery())
        {
            while(exonRS.next())
            {
               int exonId = exonRS.getInt( 1 );
               int exonStart = exonRS.getInt( 2 );
               int exonEnd = exonRS.getInt( 3 );
               exonLocations.add( new Interval( exonStart - 1, exonEnd - 1 ) );
               exonIds.add( exonId );
            }
        }

        List<Interval> cdsLocations = new ArrayList<>();
        if( loadCDS )
        {
            translationStatement.setInt( 1, transcriptId );
            try (ResultSet translationRS = translationStatement.executeQuery())
            {
                while( translationRS.next() )
                {
                    int offsetInFirstExon = translationRS.getInt( 1 );
                    int firstExonId = translationRS.getInt( 2 );
                    int offsetInLastExon = translationRS.getInt( 3 );
                    int lastExonId = translationRS.getInt( 4 );

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
        }

        Collections.sort( exonLocations );

        Transcript transcript = new Transcript( transcriptName, chr, new Interval( start - 1, end - 1 ), strand == 1, exonLocations, cdsLocations );
        return transcript;
    }
}
