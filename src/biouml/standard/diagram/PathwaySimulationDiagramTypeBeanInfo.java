package biouml.standard.diagram;

import com.developmentontheedge.beans.BeanInfoEx;

public class PathwaySimulationDiagramTypeBeanInfo extends BeanInfoEx
{
    public PathwaySimulationDiagramTypeBeanInfo()
    {
        super(PathwaySimulationDiagramType.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName     (getResourceString("CN_PATHWAY_SIMULATION_DIAGRAM"));
        beanDescriptor.setShortDescription(getResourceString("CN_PATHWAY_SIMULATION_DIAGRAM"));
    }
}
