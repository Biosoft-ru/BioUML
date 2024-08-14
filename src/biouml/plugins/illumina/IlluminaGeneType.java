package biouml.plugins.illumina;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.GeneTableType;

@ClassIcon("resources/genes-illumina.gif")
public class IlluminaGeneType extends GeneTableType
{
    @Override
    public int getIdScore(String id)
    {
        if( id.matches("GI_\\d+-\\w") )
            return SCORE_HIGH_SPECIFIC;
        if( id.matches("\\d{5,}_\\d+(_rc|)-S"))
            return SCORE_MEDIUM_SPECIFIC;
        if( id.endsWith("-S"))
            return SCORE_LOW_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "Illumina";
    }
}
