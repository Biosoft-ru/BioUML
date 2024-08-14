package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.GeneTableType;

@ClassIcon("resources/genes-entrez.gif")
public class EntrezGeneTableType extends GeneTableType
{
    @Override
    public String getSource()
    {
        return "Entrez";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("\\d{2,6}")) return SCORE_MEDIUM_SPECIFIC;
        if(id.matches("\\d{1}") || id.matches("\\d{7,10}")) return SCORE_LOW_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
