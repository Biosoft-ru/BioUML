package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.GeneTableType;

@ClassIcon("resources/genes-unigene.gif")
public class UniGeneTableType extends GeneTableType
{
    @Override
    public String getSource()
    {
        return "Unigene";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("(Hs|Mm|Rn)\\.\\d+")) return SCORE_HIGH_SPECIFIC;
        if(id.matches("[A-Z][a-z]{1,2}\\.\\d+")) return SCORE_MEDIUM_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
