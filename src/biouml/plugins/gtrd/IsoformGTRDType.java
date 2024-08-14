package biouml.plugins.gtrd;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

public class IsoformGTRDType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "Isoforms";
    }

    @Override
    public String getSource()
    {
        return "GTRD";
    }
}
