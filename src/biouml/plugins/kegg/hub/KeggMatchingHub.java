package biouml.plugins.kegg.hub;

import java.util.Properties;

import ru.biosoft.access.biohub.SQLBasedHub;
import biouml.plugins.ensembl.tabletype.EntrezGeneTableType;
import biouml.plugins.ensembl.tabletype.NCBIProteinType;

public class KeggMatchingHub extends SQLBasedHub
{
    public KeggMatchingHub(Properties properties)
    {
        super(properties);
    }
    @Override
    protected Matching[] getMatchings()
    {
        return matchings;
    }

    private final Matching[] matchings = new Matching[] {new Matching(KeggEnzymeType.class, NCBIProteinType.class, true, 1.0),
            new Matching(NCBIProteinType.class, KeggEnzymeType.class, false, 1.0),
            new Matching(EntrezGeneTableType.class, NCBIProteinType.class, true, 0.9),
            new Matching(NCBIProteinType.class, EntrezGeneTableType.class, false, 0.9)};
}
