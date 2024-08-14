package biouml.plugins.gtrd;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.type.CategoryType;

/**
 * @author lan
 *
 */
public class ClassGTRDType extends CategoryType
{
    @Override
    public int getIdScore(String id)
    {
        String[] fields = id.split("\\.");
        if(fields.length > 6) return SCORE_NOT_THIS_TYPE;
        for(String field: fields)
        {
            if(field.length()>3 || !field.matches("\\d+")) return SCORE_NOT_THIS_TYPE;
        }
        return SCORE_LOW_SPECIFIC;
    }

    @Override
    public String getSource()
    {
        return "GTRD";
    }

    @Override
    public String getURL(String id)
    {
        return "de:" + getPath(id).toString();
    }
    
    @Override
    public DataElementPath getPath(String id)
    {
        DataElementPath path = DataElementPath.create( "databases/GTRD/Dictionaries/classification" );
        id = id + ".";
        int idx = -1;
        while((idx = id.indexOf( '.', idx + 1 )) >= 0)
        {
            String prefix = id.substring( 0, idx );
            if(path.getChildPath( prefix ).exists())
            {
                path = path.getChildPath( prefix );
            }
        }
        return path;
    }

    @Override
    public String getDescriptionHTML()
    {
        return "Transcription factor class identifier by <a href=\"http://edgar-wingender.de/huTF_classification.html\">Wingender classification</a>.";
    }

    @Override
    public String getSampleID()
    {
        return "1.1.2.1";
    }
}
