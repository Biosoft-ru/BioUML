package biouml.plugins.ensembl.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.exception.BioHubFetchException;
import ru.biosoft.access.sql.SqlUtil;
import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.plugins.ensembl.tabletype.EnsemblProteinTableType;
import biouml.plugins.ensembl.tabletype.EnsemblTranscriptTableType;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public class InternalEnsemblHub extends AbstractEnsemblHub
{
    @SuppressWarnings ( "unchecked" )
    private final ReferenceType[] types =
        ReferenceTypeRegistry.getReferenceTypes(EnsemblGeneTableType.class, EnsemblTranscriptTableType.class, EnsemblProteinTableType.class);
    
    private static final String[] QUERIES = {
        // Transcript->Genes
        "select distinct gs.stable_id from gene_stable_id gs,transcript_stable_id ts,transcript t where gs.gene_id=t.gene_id and " +
        "ts.transcript_id=t.transcript_id and ts.stable_id=?",
        // Translation->Genes
        "select distinct gs.stable_id from gene_stable_id gs,translation_stable_id ts,transcript t,translation tr where gs.gene_id=t.gene_id and " +
        "tr.transcript_id=t.transcript_id and tr.translation_id=ts.translation_id and ts.stable_id=?",
        // Translation->Transcript
        "select distinct trs.stable_id from transcript_stable_id trs,translation_stable_id ts,translation tr where trs.transcript_id=tr.transcript_id and " +
        "tr.translation_id=ts.translation_id and ts.stable_id=?",
        // Genes->Transcripts
        "select distinct ts.stable_id from gene_stable_id gs,transcript_stable_id ts,transcript t where gs.gene_id=t.gene_id and " +
        "ts.transcript_id=t.transcript_id and gs.stable_id=?",
        // Genes->Translation
        "select distinct ts.stable_id from gene_stable_id gs,translation_stable_id ts,transcript t,translation tr where gs.gene_id=t.gene_id and " +
        "tr.transcript_id=t.transcript_id and tr.translation_id=ts.translation_id and gs.stable_id=?",
        // Transcript->Translation
        "select distinct ts.stable_id from transcript_stable_id trs,translation_stable_id ts,translation tr where trs.transcript_id=tr.transcript_id and " +
        "tr.translation_id=ts.translation_id and trs.stable_id=?"
    };
    
    private String getQuery(ReferenceType inputType, ReferenceType outputType)
    {
        if(inputType.getClass().equals(EnsemblGeneTableType.class))
        {
            if(outputType.getClass().equals(EnsemblTranscriptTableType.class))
                return QUERIES[3];
            if(outputType.getClass().equals(EnsemblProteinTableType.class))
                return QUERIES[4];
        }
        if(inputType.getClass().equals(EnsemblTranscriptTableType.class))
        {
            if(outputType.getClass().equals(EnsemblGeneTableType.class))
                return QUERIES[0];
            if(outputType.getClass().equals(EnsemblProteinTableType.class))
                return QUERIES[5];
        }
        if(inputType.getClass().equals(EnsemblProteinTableType.class))
        {
            if(outputType.getClass().equals(EnsemblGeneTableType.class))
                return QUERIES[1];
            if(outputType.getClass().equals(EnsemblTranscriptTableType.class))
                return QUERIES[2];
        }
        return null;
    }

    public InternalEnsemblHub(Properties properties)
    {
        super(properties);
    }

    @Override
    public double getMatchingQuality(ReferenceType inputType, ReferenceType outputType)
    {
        if(inputType.equals(outputType)) return 0;
        if(inputType.getClass().equals(EnsemblGeneTableType.class)) return 1;
        if(inputType.getClass().equals(EnsemblTranscriptTableType.class) && outputType.getClass().equals(EnsemblProteinTableType.class)) return 1;
        return 0.7;
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, ReferenceType inputType, ReferenceType outputType,
            Properties properties, FunctionJobControl jobControl)
    {
        PreparedStatement ps = null;
        try
        {
            DataElementPath ensemblPath = getEnsemblPath(properties);
            Connection conn = getConnection(ensemblPath);
            String query = getQuery(inputType, outputType);
            if(query == null)
                throw new Exception("Unsupported input/output type combination");
            // The ids, then the empty ones again without their version, are converted in batches; ids a batch cannot
            // answer exactly (see batchConvert) are converted one by one as before.
            Map<String, List<String>> converted = batchConvert( conn, query, Arrays.asList( inputList ), jobControl );
            if( converted == null )
                return null;
            List<String> retries = new ArrayList<>();
            for( String input : inputList )
            {
                List<String> found = converted.get( input );
                if( found != null && found.isEmpty() )
                    retries.add( stripVersion( input ) );
            }
            Map<String, List<String>> retried = batchConvert( conn, query, retries, jobControl );
            if( retried == null )
                return null;
            ps = conn.prepareStatement(query);
            Map<String, String[]> result = new HashMap<>();
            List<String> curList = new ArrayList<>();
            for( int i = 0; i < inputList.length; i++ )
            {
                String input = inputList[i];
                List<String> found = converted.get( input );
                if( found == null )
                    convertId( input, ps, curList );
                else
                    curList = found;
                if( curList.isEmpty() )
                {
                    String stripped = stripVersion( input );
                    found = retried.get( stripped );
                    if( found == null )
                    {
                        curList = new ArrayList<>(); // never refill a list held by the batch results
                        convertId( stripped, ps, curList );
                    }
                    else
                        curList = found;
                }

                result.put(input, curList.toArray(new String[curList.size()]));
                curList = new ArrayList<>();
                if( jobControl != null )
                {
                    jobControl.setPreparedness(i * 100 / inputList.length);
                    if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                        return null;
                }
            }
            return result;
        }
        catch( Exception e )
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError(e);
            throw new BioHubFetchException( e, this );
        }
        finally
        {
            SqlUtil.close( ps, null );
        }
    }

    /** Number of ids looked up by one batched query. */
    static final int BATCH_SIZE = 500;

    private static final Pattern KEY_CONDITION = Pattern.compile( "(\\S+)=\\?$" );

    private static String stripVersion(String id)
    {
        return id.replaceFirst( "\\.\\d++", "" );
    }

    /**
     * The query for many ids at once. The "...and key=?" condition of a QUERIES entry becomes "key in (?,...)", and
     * FIELD(key, ?,...) tells which id a row matched: "select distinct FIELD(key,?,..), value ... and key in (?,..)".
     * FIELD and IN compare with the same collation as "key=?", so each row is matched to the id the per-id query
     * would have returned it for.
     */
    static String batchQuery(String query, int count)
    {
        Matcher m = KEY_CONDITION.matcher( query );
        if( !m.find() || !query.startsWith( "select distinct " ) )
            throw new IllegalArgumentException( "Not a single-key query: " + query );
        String key = m.group( 1 );
        String params = String.join( ",", Collections.nCopies( count, "?" ) );
        return "select distinct FIELD(" + key + "," + params + ")," + query.substring( "select distinct ".length(), m.start() ) + key
                + " in (" + params + ")";
    }

    /**
     * Converts the ids in batches of {@link #BATCH_SIZE} instead of one query per id: in profiles of
     * FindRegulatoryRegions the one-query-per-id round trips to MySQL were a large share of the analysis.
     * <p>
     * The result is the per-id query's for every id answered: the same rows (FIELD matches each row to its id with
     * the column's collation) and an empty list for an id without rows. Ids two of which could match the same row
     * (non-ASCII ids, ids equal ignoring case or trailing spaces) are left out, as FIELD reports only the first of
     * them; the caller converts those one by one.
     *
     * @return the outputs by id for the ids answered here, or null if the job was terminated
     */
    private static Map<String, List<String>> batchConvert(Connection conn, String query, Collection<String> inputs,
            FunctionJobControl jobControl) throws SQLException
    {
        Map<String, Integer> spellings = new HashMap<>();
        for( String id : new LinkedHashSet<>( inputs ) )
            spellings.merge( normalize( id ), 1, Integer::sum );
        List<String> ids = new ArrayList<>();
        for( String id : new LinkedHashSet<>( inputs ) )
            if( isAscii( id ) && spellings.get( normalize( id ) ) == 1 )
                ids.add( id );

        Map<String, List<String>> result = new HashMap<>();
        for( int from = 0; from < ids.size(); from += BATCH_SIZE )
        {
            List<String> chunk = ids.subList( from, Math.min( from + BATCH_SIZE, ids.size() ) );
            for( String id : chunk )
                result.put( id, new ArrayList<>() );
            try( PreparedStatement ps = conn.prepareStatement( batchQuery( query, chunk.size() ) ) )
            {
                for( int i = 0; i < chunk.size(); i++ )
                {
                    ps.setString( i + 1, chunk.get( i ) );
                    ps.setString( chunk.size() + i + 1, chunk.get( i ) );
                }
                try( ResultSet rs = ps.executeQuery() )
                {
                    while( rs.next() )
                        result.get( chunk.get( rs.getInt( 1 ) - 1 ) ).add( rs.getString( 2 ) );
                }
            }
            if( jobControl != null && jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                return null;
        }
        return result;
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

    private void convertId(String id, PreparedStatement ps, List<String> result) throws SQLException
    {
        result.clear();
        ps.setString(1, id);
        try( ResultSet rs = ps.executeQuery() )
        {
            while( rs.next() )
            {
                result.add(rs.getString(1));
            }
        }
    }

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        return types.clone();
    }

    @Override
    public ReferenceType[] getSupportedMatching(ReferenceType inputType)
    {
        for(ReferenceType type: types)
        {
            if(inputType.equals(type)) return types.clone();
        }
        return null;
    }
}
