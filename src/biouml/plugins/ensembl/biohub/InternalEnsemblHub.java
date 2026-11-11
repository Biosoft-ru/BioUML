package biouml.plugins.ensembl.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.exception.BioHubFetchException;
import ru.biosoft.access.sql.SqlBatchLookup;
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
                if( found != null && found.isEmpty() && !stripVersion( input ).equals( input ) )
                    retries.add( stripVersion( input ) );
            }
            Map<String, List<String>> retried = batchConvert( conn, query, retries, jobControl );
            if( retried == null )
                return null;
            ps = conn.prepareStatement(query);
            Map<String, String[]> result = new HashMap<>();
            for( int i = 0; i < inputList.length; i++ )
            {
                String input = inputList[i];
                List<String> curList = converted.get( input );
                if( curList == null )
                {
                    if( isTerminated( jobControl ) )
                        return null;
                    curList = new ArrayList<>();
                    convertId( input, ps, curList );
                }
                String stripped = stripVersion( input );
                if( curList.isEmpty() && !stripped.equals( input ) ) // retrying the same id would find nothing again
                {
                    curList = retried.get( stripped );
                    if( curList == null )
                    {
                        if( isTerminated( jobControl ) )
                            return null;
                        curList = new ArrayList<>();
                        convertId( stripped, ps, curList );
                    }
                }

                result.put(input, curList.toArray(new String[curList.size()]));
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

    private static final Pattern KEY_CONDITION = Pattern.compile( "(\\S+)=\\?$" );

    private static String stripVersion(String id)
    {
        return id.replaceFirst( "\\.\\d++", "" );
    }

    private static boolean isTerminated(FunctionJobControl jobControl)
    {
        return jobControl != null && jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST;
    }

    /**
     * The query for many ids at once (public for tests). The "...and key=?" condition of a QUERIES entry becomes
     * "key in (?,...)", and FIELD(key, ?,...) tells which id a row matched:
     * "select distinct FIELD(key,?,..), value ... and key in (?,..)". DISTINCT now applies to (id, value) pairs, which
     * is what "select distinct value ... key=?" gives for each id; the same value found for two ids is kept for both.
     */
    public static String batchQuery(String query, int count)
    {
        Matcher m = KEY_CONDITION.matcher( query );
        if( !m.find() || !query.startsWith( "select distinct " ) )
            throw new IllegalArgumentException( "Not a single-key query: " + query );
        String key = m.group( 1 );
        String params = SqlBatchLookup.placeholders( count );
        return "select distinct FIELD(" + key + "," + params + ")," + query.substring( "select distinct ".length(), m.start() ) + key
                + " in (" + params + ")";
    }

    /**
     * Converts the ids in batches of {@link SqlBatchLookup#BATCH_SIZE} instead of one query per id: in profiles of
     * FindRegulatoryRegions the one-query-per-id round trips to MySQL were a large share of the analysis.
     * <p>
     * Answers only the ids {@link SqlBatchLookup#batchableIds} accepts, and for each of them returns what the per-id
     * query returns: its rows, or an empty list. A chunk with a row FIELD cannot attribute is dropped entirely. Ids
     * not answered are left out; the caller converts those one by one.
     *
     * @return the outputs by id for the ids answered here, or null if the job was terminated
     */
    private static Map<String, List<String>> batchConvert(Connection conn, String query, Collection<String> inputs,
            FunctionJobControl jobControl) throws SQLException
    {
        Map<String, List<String>> result = new HashMap<>();
        for( List<String> chunk : SqlBatchLookup.chunks( SqlBatchLookup.batchableIds( inputs ) ) )
        {
            if( isTerminated( jobControl ) )
                return null;
            Map<String, List<String>> chunkResult = new HashMap<>();
            for( String id : chunk )
                chunkResult.put( id, new ArrayList<>() );
            boolean attributed = true;
            try( PreparedStatement ps = conn.prepareStatement( batchQuery( query, chunk.size() ) ) )
            {
                for( int i = 0; i < chunk.size(); i++ )
                {
                    ps.setString( i + 1, chunk.get( i ) );
                    ps.setString( chunk.size() + i + 1, chunk.get( i ) );
                }
                try( ResultSet rs = ps.executeQuery() )
                {
                    while( attributed && rs.next() )
                    {
                        int index = SqlBatchLookup.fieldIndex( rs.getInt( 1 ), chunk.size() );
                        if( index < 0 )
                            attributed = false;
                        else
                            chunkResult.get( chunk.get( index ) ).add( rs.getString( 2 ) );
                    }
                }
            }
            if( attributed )
                result.putAll( chunkResult );
        }
        return isTerminated( jobControl ) ? null : result;
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
