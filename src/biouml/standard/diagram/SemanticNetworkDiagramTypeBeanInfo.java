package biouml.standard.diagram;

import com.developmentontheedge.beans.BeanInfoEx;

public class SemanticNetworkDiagramTypeBeanInfo extends BeanInfoEx
{
    public SemanticNetworkDiagramTypeBeanInfo()
    {
        super(SemanticNetworkDiagramType.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName     (getResourceString("CN_SEMANTIC_NETWORK_DIAGRAM"));
        beanDescriptor.setShortDescription(getResourceString("CD_SEMANTIC_NETWORK_DIAGRAM"));
    }
}
