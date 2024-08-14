package biouml.plugins.expasy;

import ru.biosoft.analysis.type.CategoryType;

public class EnzymeClassExpasyType extends CategoryType
{
    @Override
    public int getIdScore(String id)
    {
        String[] fields = id.split("\\.");
        if(fields.length > 3) return SCORE_NOT_THIS_TYPE;
        for(String field: fields)
        {
            if(field.length()>3 || !field.matches("\\d+")) return SCORE_NOT_THIS_TYPE;
        }
        return SCORE_LOW_SPECIFIC;
    }

    @Override
    public String getSource()
    {
        return "ExPASy";
    }

}
