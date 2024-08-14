package biouml.plugins.gtrd;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

public class ReadsGTRDType extends ReferenceTypeSupport
{

    @Override
    public int getIdScore(String id)
    {
        if(id.startsWith("READS"))
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "ChIP-seq reads";
    }
    
    @Override
    public String getSource()
    {
        return "GTRD";
    }

}
