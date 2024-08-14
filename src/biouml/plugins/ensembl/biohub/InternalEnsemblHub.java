package biouml.plugins.ensembl.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
            ps = conn.prepareStatement(query);
            Map<String, String[]> result = new HashMap<>();
            List<String> curList = new ArrayList<>();
            for( int i = 0; i < inputList.length; i++ )
            {
                String input = inputList[i];
                convertId( input, ps, curList);
                if(curList.isEmpty())
                    convertId( input.replaceFirst("\\.\\d++",""), ps, curList);
                
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
