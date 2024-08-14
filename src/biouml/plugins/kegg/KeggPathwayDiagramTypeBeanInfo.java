package biouml.plugins.kegg;

import com.developmentontheedge.beans.BeanInfoEx;

public class KeggPathwayDiagramTypeBeanInfo extends BeanInfoEx
{
    public KeggPathwayDiagramTypeBeanInfo()
    {
        super(KeggPathwayDiagramType.class, "biouml.plugins.kegg.MessageBundle" );
        beanDescriptor.setDisplayName     (getResourceString("CN_PATHWAY_DIAGRAM"));
        beanDescriptor.setShortDescription(getResourceString("CD_PATHWAY_DIAGRAM"));
    }
}
