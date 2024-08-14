package biouml.plugins.chebi;

import ru.biosoft.analysis.type.SubstanceType;

public class DrugBankSubstanceType extends SubstanceType
{
    @Override
    public int getIdScore(String id)
    {
        if(id.matches( "^DB\\d{5}$" ))
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "DrugBank";
    }
}
