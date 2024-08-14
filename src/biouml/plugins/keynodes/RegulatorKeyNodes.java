package biouml.plugins.keynodes;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

/**
 * @author anna
 *
 */
@ClassIcon("resources/regulator-target-search.gif")
public class RegulatorKeyNodes extends KeyNodeAnalysis
{
    public RegulatorKeyNodes(DataCollection<?> origin, String name)
    {
        super(origin, name);
        parameters = new RegulatorKeyNodesParameters();
    }
}
