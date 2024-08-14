package biouml.model.xml;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class XmlDiagramTypeBeanInfo extends BeanInfoEx
{
    public XmlDiagramTypeBeanInfo()
    {
        super(XmlDiagramType.class, "biouml.model.xml.MessageBundle" );
        beanDescriptor.setDisplayName     (getResourceString("CN_XML_DIAGRAM_TYPE"));
        beanDescriptor.setShortDescription(getResourceString("CD_XML_DIAGRAM_TYPE"));
    }

    @Override
    public void initProperties ( ) throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx ("name", beanClass, "getName", null);
        add(pde, getResourceString("PN_XML_DIAGRAM_TYPE_NAME"),
                 getResourceString ("PD_XML_DIAGRAM_TYPE_NAME") );
    }
}
