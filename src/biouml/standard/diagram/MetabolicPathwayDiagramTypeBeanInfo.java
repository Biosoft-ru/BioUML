package biouml.standard.diagram;

import com.developmentontheedge.beans.BeanInfoEx;

public class MetabolicPathwayDiagramTypeBeanInfo extends BeanInfoEx
{
    public MetabolicPathwayDiagramTypeBeanInfo()
    {
        super(MetabolicPathwayDiagramType.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName     (getResourceString("CN_METABOLIC_PATHWAY_DIAGRAM"));
        beanDescriptor.setShortDescription(getResourceString("CD_METABOLIC_PATHWAY_DIAGRAM"));
    }
}
