package biouml.plugins.gtrd;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.type.ProteinTableType;

public class ProteinGTRDType extends ProteinTableType
{
    @Override
    public int getIdScore(String id)
    {
        if(id.split("\\.").length == 5)
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "GTRD";
    }

    @Override
    public DataElementPath getPath(String id)
    {
        String url = id;
        while(id.contains("."))
        {
            id = id.substring(0, id.lastIndexOf('.'));
            if(!id.endsWith(".0"))
            {
                url = id+"/"+url;
            }
        }
        return DataElementPath.create( "databases/GTRD/Dictionaries/classification/"+url);
    }
}
