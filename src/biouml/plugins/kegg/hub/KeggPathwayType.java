package biouml.plugins.kegg.hub;

import biouml.model.Module;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.type.PathwayType;

/**
 * @author lan
 */
public class KeggPathwayType extends PathwayType
{
    @Override
    public int getIdScore(String id)
    {
        return id.startsWith("map") && id.endsWith(".xml") ? SCORE_HIGH_SPECIFIC : SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "KEGG";
    }

    @Override
    public DataElementPath getPath(String id)
    {
        return DataElementPath.create("databases", "KEGG", Module.DIAGRAM, id);
    }
}
