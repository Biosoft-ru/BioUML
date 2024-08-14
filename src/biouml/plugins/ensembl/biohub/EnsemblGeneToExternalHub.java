package biouml.plugins.ensembl.biohub;

import java.util.Properties;

import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.plugins.ensembl.tabletype.EntrezGeneTableType;
import biouml.plugins.ensembl.tabletype.GeneSymbolTableType;
import biouml.plugins.ensembl.tabletype.UniGeneTableType;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;

/**
 * @author lan
 *
 */
public class EnsemblGeneToExternalHub extends EnsemblToExternalHub
{
    private final TypeRecord[] supportedTypeRecords =
    {
            new TypeRecord("76-", GeneSymbolTableType.class, ExternalToEnsemblGeneHub.GENE_SYMBOL_RESTRICTIONS, 0.6, true, null),
            new TypeRecord("60-", EntrezGeneTableType.class, "EntrezGene", 0.9),
            new TypeRecord("60-", UniGeneTableType.class, "UniGene", 0.5),
    };

    private final ReferenceType inputType = ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class);
    
    public EnsemblGeneToExternalHub(Properties properties)
    {
        super(properties);
    }

    @Override
    protected TypeRecord[] getSupportedTypeRecords()
    {
        return supportedTypeRecords;
    }

    @Override
    protected String getQueryTemplate(DataElementPath ensemblPath)
    {
        return "SELECT DISTINCT "+RESULT_COLUMN+" FROM xref x JOIN object_xref o USING(xref_id) JOIN gene_stable_id g ON(g.gene_id=o.ensembl_id) WHERE "+
        "ensembl_object_type='Gene' AND g.stable_id=? AND ";
    }

    @Override
    protected ReferenceType getInputType()
    {
        return inputType;
    }

    @Override
    protected String getObjectType()
    {
        return "Gene";
    }

}
