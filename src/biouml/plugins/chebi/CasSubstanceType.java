package biouml.plugins.chebi;

import ru.biosoft.analysis.type.SubstanceType;

public class CasSubstanceType extends SubstanceType
{
    @Override
    public int getIdScore(String id)
    {
        if(id.matches( "^\\d{1,7}\\-\\d{2}\\-\\d$" ))
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "CAS";
    }
}
