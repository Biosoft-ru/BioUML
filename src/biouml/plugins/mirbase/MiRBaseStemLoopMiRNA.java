package biouml.plugins.mirbase;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

public class MiRBaseStemLoopMiRNA extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        //TODO: check imported data
        if( id.contains( "-mir-" ) || id.contains( "-let-" ) || id.contains( "-miR-" ) )
            return SCORE_MEDIUM_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "stem-loop-miRNA";
    }
    
    @Override
    public String getSource()
    {
        return "miRBase";
    }

}
