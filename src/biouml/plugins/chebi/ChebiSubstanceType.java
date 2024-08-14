package biouml.plugins.chebi;

import ru.biosoft.analysis.type.SubstanceType;

public class ChebiSubstanceType extends SubstanceType
{
    @Override
    public int getIdScore(String id)
    {
        if(id.matches( "CHEBI:\\d+" ))
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "ChEBI";
    }
}
