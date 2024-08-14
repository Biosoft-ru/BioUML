package biouml.plugins.illumina;

import java.util.Properties;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.SQLBasedHub;
import biouml.plugins.ensembl.tabletype.EntrezGeneTableType;
import biouml.plugins.ensembl.tabletype.IlluminaProbeTableType;

public class IlluminaHub extends SQLBasedHub
{
    public IlluminaHub(Properties properties)
    {
        super(properties);
    }
    @Override
    protected Matching[] getMatchings()
    {
        return matchings;
    }
    @Override
    protected String processInputId(String tfId, ReferenceType inputType)
    {
        if( inputType.getClass().equals(IlluminaGeneType.class) )
            return tfId.replaceFirst("\\-\\w$", "");
        return tfId;
    }

    private final Matching[] matchings = new Matching[] {new Matching(IlluminaGeneType.class, IlluminaProbeTableType.class, false, 0.99),
            new Matching(IlluminaProbeTableType.class, EntrezGeneTableType.class, true, 0.8),
            new Matching(IlluminaTranscriptType.class, IlluminaProbeTableType.class, false, 0.8)};
}
