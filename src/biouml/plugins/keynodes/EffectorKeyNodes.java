package biouml.plugins.keynodes;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

/**
 * @author anna
 *
 */
@ClassIcon("resources/effector-target-search.gif")
public class EffectorKeyNodes extends KeyNodeAnalysis
{
    public EffectorKeyNodes(DataCollection<?> origin, String name)
    {
        super(origin, name);
        parameters = new EffectorKeyNodesParameters();
    }
}
