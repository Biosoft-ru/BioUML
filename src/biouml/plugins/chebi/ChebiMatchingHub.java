package biouml.plugins.chebi;

import java.util.Properties;

import ru.biosoft.access.biohub.SQLBasedHub;

public class ChebiMatchingHub extends SQLBasedHub
{
    private final Matching[] matchings = {
            new Matching(ChebiSubstanceType.class, KeggSubstanceType.class, true, 0.9),
            new Matching(KeggSubstanceType.class, ChebiSubstanceType.class, false, 0.9),
            new Matching(ChebiSubstanceType.class, CasSubstanceType.class, true, 0.9),
            new Matching(CasSubstanceType.class, ChebiSubstanceType.class, false, 0.9),
            new Matching(ChebiSubstanceType.class, DrugBankSubstanceType.class, true, 0.9),
            new Matching(DrugBankSubstanceType.class, ChebiSubstanceType.class, false, 0.9),
    };

    public ChebiMatchingHub(Properties properties)
    {
        super(properties);
    }

    @Override
    protected Matching[] getMatchings()
    {
        return matchings;
    }

    @Override
    protected boolean controlSpecies()
    {
        return false;
    }
}
