package biouml.plugins.ensembl.biohub;

import java.util.Properties;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import biouml.plugins.ensembl.tabletype.EnsemblProteinTableType;
import biouml.plugins.ensembl.tabletype.EntrezGeneTableType;
import biouml.plugins.ensembl.tabletype.GenBankProteinType;
import biouml.plugins.ensembl.tabletype.RefSeqProteinTableType;
import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;


public class EnsemblProteinToExternalHub extends EnsemblToExternalHub
{
    private final TypeRecord[] supportedTypeRecords =
    {
            new TypeRecord("-59", EntrezGeneTableType.class, "EntrezGene", 0.9),
            new TypeRecord("*", RefSeqProteinTableType.class, "RefSeq_peptide", 0.9),
            new TypeRecord("*", UniprotProteinTableType.class, "Uniprot/SWISSPROT", 0.8),
            new TypeRecord("*", GenBankProteinType.class, "protein_id", 0.9),
    };
    
    private ReferenceType inputType = ReferenceTypeRegistry.getReferenceType(EnsemblProteinTableType.class);
    
    public EnsemblProteinToExternalHub(Properties properties)
    {
        super(properties);
    }

    @Override
    protected ReferenceType getInputType()
    {
        return inputType;
    }
    
    @Override
    protected String getQueryTemplate(DataElementPath ensemblPath)
    {
        return "SELECT DISTINCT "+RESULT_COLUMN+" FROM xref x JOIN object_xref o USING(xref_id) JOIN translation_stable_id t ON(t.translation_id=o.ensembl_id) WHERE "+
        "ensembl_object_type='Translation' AND t.stable_id=? AND ";
    }
    
    @Override
    protected TypeRecord[] getSupportedTypeRecords()
    {
        return supportedTypeRecords;
    }

    @Override
    protected String getObjectType()
    {
        return "Translation";
    }
}
