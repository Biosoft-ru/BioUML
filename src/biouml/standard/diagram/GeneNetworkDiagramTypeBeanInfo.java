package biouml.standard.diagram;

import com.developmentontheedge.beans.BeanInfoEx;

public class GeneNetworkDiagramTypeBeanInfo extends BeanInfoEx
{
    public GeneNetworkDiagramTypeBeanInfo()
    {
        super(GeneNetworkDiagramType.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName     (getResourceString("CN_GENE_NETWORK_DIAGRAM"));
        beanDescriptor.setShortDescription(getResourceString("CD_GENE_NETWORK_DIAGRAM"));
    }
}
