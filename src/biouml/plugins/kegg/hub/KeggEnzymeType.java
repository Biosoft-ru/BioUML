package biouml.plugins.kegg.hub;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

/**
 * @author lan
 *
 */
public class KeggEnzymeType extends ReferenceTypeSupport
{

    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "Enzymes";
    }

    @Override
    public String getSource()
    {
        return "KEGG";
    }
}
