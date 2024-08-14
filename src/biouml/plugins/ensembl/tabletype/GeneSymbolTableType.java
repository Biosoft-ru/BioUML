package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.GeneTableType;

@ClassIcon("resources/genes-genesymbol.gif")
public class GeneSymbolTableType extends GeneTableType
{
    @Override
    public String getSource()
    {
        return "Gene symbol";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("LOC\\d{3,}")) return SCORE_HIGH_SPECIFIC;
        if(id.matches("[A-Z]([A-Z][A-Z0-9]{2,6}|[a-z][a-z0-9]{2,6})")) return SCORE_MEDIUM_SPECIFIC;
        if(id.matches("\\w{2,10}") && !id.matches("\\d+")) return SCORE_LOW_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
