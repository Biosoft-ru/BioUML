package biouml.plugins.chebi;

import ru.biosoft.analysis.type.SubstanceType;

public class KeggSubstanceType extends SubstanceType
{
    @Override
    public int getIdScore(String id)
    {
        if(id.matches( "C\\d{5}" ))
            return SCORE_ABOVE_MEDIUM_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "KEGG";
    }
}
