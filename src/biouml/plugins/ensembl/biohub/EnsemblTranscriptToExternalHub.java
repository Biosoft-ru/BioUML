package biouml.plugins.ensembl.biohub;

import java.util.Properties;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import biouml.plugins.ensembl.tabletype.EnsemblTranscriptTableType;
import biouml.plugins.ensembl.tabletype.GeneSymbolTableType;
import biouml.plugins.ensembl.tabletype.RefSeqTranscriptTableType;


public class EnsemblTranscriptToExternalHub extends EnsemblToExternalHub
{
    private final TypeRecord[] supportedTypeRecords =
    {
            new TypeRecord("-75", GeneSymbolTableType.class, "HGNC,RGD,MGI", 0.6, true),
            new TypeRecord("-59", RefSeqTranscriptTableType.class, "RefSeq_dna,RefSeq_rna"),
    };
    
    private final ReferenceType outputType = ReferenceTypeRegistry.getReferenceType(EnsemblTranscriptTableType.class);
    
    public EnsemblTranscriptToExternalHub(Properties properties)
    {
        super(properties);
    }

    @Override
    protected ReferenceType getInputType()
    {
        return outputType;
    }
    
    @Override
    protected String getQueryTemplate(DataElementPath ensemblPath)
    {
        return "SELECT DISTINCT "+RESULT_COLUMN+" FROM xref x JOIN object_xref o USING(xref_id) JOIN transcript_stable_id t ON(t.transcript_id=o.ensembl_id) WHERE "+
        "ensembl_object_type='Transcript' AND t.stable_id=? AND ";
    }
    
    @Override
    protected TypeRecord[] getSupportedTypeRecords()
    {
        return supportedTypeRecords;
    }

    @Override
    protected String getObjectType()
    {
        return "Transcript";
    }
}
