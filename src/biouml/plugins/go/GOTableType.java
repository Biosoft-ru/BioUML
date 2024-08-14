package biouml.plugins.go;

import ru.biosoft.analysis.type.CategoryType;

public class GOTableType extends CategoryType
{
    @Override
    public String getSource()
    {
        return "Gene ontology";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("GO\\:\\d{7}")) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getDescriptionHTML()
    {
        return "Category of the <a href=\"http://www.geneontology.org/\">Gene Ontology</a>";
    }
}
