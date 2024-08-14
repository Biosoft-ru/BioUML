package biouml.plugins.ensembl.biohub;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import biouml.plugins.ensembl.tabletype.EnsemblProteinTableType;
import biouml.plugins.ensembl.tabletype.EntrezGeneTableType;
import biouml.plugins.ensembl.tabletype.GenBankGeneTableType;
import biouml.plugins.ensembl.tabletype.GenBankProteinType;
import biouml.plugins.ensembl.tabletype.IPIProteinTableType;
import biouml.plugins.ensembl.tabletype.RefSeqProteinTableType;
import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;


public class ExternalToEnsemblProteinHub extends ExternalToEnsemblHub
{
    private final TypeRecord[] supportedTypeRecords =
    {
            new TypeRecord("-59", EntrezGeneTableType.class, "EntrezGene", 0.9),
            new TypeRecord("*", GenBankGeneTableType.class, "EMBL", 0.9)
            {
                private final Pattern PATTERN = Pattern.compile("MM([A-Z]*)(\\d+)");
                @Override
                public String strip(String id)
                {
                    Matcher m = PATTERN.matcher(id);
                    if(m.matches())
                    {
                        String letters = m.group(1);
                        int digits = Integer.parseInt(m.group(2));
                        if(letters.equals("") || letters.equals("U"))
                            return String.format("U%05d", digits);
                        if(letters.equals("AF"))
                            return String.format("AF%06d", digits);
                        if(letters.equals("AB"))
                            return String.format("AB%06d", digits);
                    }
                    return id;
                }
            },
            new TypeRecord("*", GenBankProteinType.class, "protein_id", 0.9, false, "\\.\\d+$"),
            new TypeRecord("*", IPIProteinTableType.class, "IPI", 0.9, false, "\\.\\d+$"),
            new TypeRecord("*", RefSeqProteinTableType.class, "RefSeq_peptide,RefSeq_peptide_predicted", 0.9),
            new TypeRecord("*", UniprotProteinTableType.class, "Uniprot/SWISSPROT,Uniprot/SWISSPROT_predicted", 0.8, false, "(_[A-Z]+|\\-\\d+)$"),
    };
    
    private ReferenceType outputType = ReferenceTypeRegistry.getReferenceType(EnsemblProteinTableType.class);
    
    public ExternalToEnsemblProteinHub(Properties properties)
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
                + "JOIN translation_stable_id ts ON(ts.translation_id=o.ensembl_id) "
                + "JOIN translation tl ON(ts.translation_id=tl.translation_id) "
                + "JOIN transcript t ON (tl.transcript_id=t.transcript_id)"
                + "JOIN seq_region sr ON(t.seq_region_id=sr.seq_region_id) "
                + "WHERE ensembl_object_type='Translation' AND " + typeRecord.getRestrictionClause(species);
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
