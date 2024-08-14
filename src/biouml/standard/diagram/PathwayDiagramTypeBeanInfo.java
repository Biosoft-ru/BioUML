package biouml.standard.diagram;

import com.developmentontheedge.beans.BeanInfoEx;

public class PathwayDiagramTypeBeanInfo extends BeanInfoEx
{
    public PathwayDiagramTypeBeanInfo()
    {
        super(PathwayDiagramType.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName     (getResourceString("CN_PATHWAY_DIAGRAM"));
        beanDescriptor.setShortDescription(getResourceString("CD_PATHWAY_DIAGRAM"));
    }
}
