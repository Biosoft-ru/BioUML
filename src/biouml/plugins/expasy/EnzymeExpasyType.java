package biouml.plugins.expasy;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

public class EnzymeExpasyType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        String[] fields = id.split("\\.");
        if(fields.length != 4) return SCORE_NOT_THIS_TYPE;
        for(String field: fields)
        {
            if(field.length()>3 || !field.matches("\\d+")) return SCORE_NOT_THIS_TYPE;
        }
        return SCORE_HIGH_SPECIFIC;
    }

    @Override
    public String getObjectType()
    {
        return "Enzymes";
    }
    
    @Override
    public String getSource()
    {
        return "ExPASy";
    }

}
