package biouml.plugins.ensembl.biohub;

import java.util.Properties;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import biouml.plugins.ensembl.tabletype.EnsemblTranscriptTableType;
import biouml.plugins.ensembl.tabletype.RefSeqTranscriptTableType;
import biouml.plugins.ensembl.tabletype.UniGeneTableType;


public class ExternalToEnsemblTranscriptHub extends ExternalToEnsemblHub
{
    private final TypeRecord[] supportedTypeRecords =
    {
            new TypeRecord("-59", UniGeneTableType.class, "UniGene"),
            //new TypeRecord("*", GeneSymbolTableType.class, "HGNC,RGD,MGI", 0.8, true, "^(LOC|loc)(\\d+)$/$2"),
            new TypeRecord("*", RefSeqTranscriptTableType.class, "RefSeq_dna,RefSeq_dna_predicted,RefSeq_rna,RefSeq_rna_predicted,RefSeq_mRNA"),
    };
    
    private ReferenceType outputType = ReferenceTypeRegistry.getReferenceType(EnsemblTranscriptTableType.class);
    
    public ExternalToEnsemblTranscriptHub(Properties properties)
    {
        super(properties);
    }

    @Override
    protected ReferenceType getOutputType()
    {
        return outputType;
    }
    
    @Override
    protected String getQueryTemplate(String species, DataElementPath ensemblPath, TypeRecord typeRecord)
    {
        return "SELECT DISTINCT ts.stable_id,sr.name FROM xref x "
                + "JOIN object_xref o USING(xref_id) "
                + "JOIN transcript_stable_id ts ON(ts.transcript_id=o.ensembl_id) "
                + "JOIN transcript t ON (ts.transcript_id=t.transcript_id)"
                + "JOIN seq_region sr ON(t.seq_region_id=sr.seq_region_id) "
                + "WHERE ensembl_object_type='Transcript' AND ts.stable_id LIKE 'ENS%' AND " + typeRecord.getRestrictionClause(species);
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
