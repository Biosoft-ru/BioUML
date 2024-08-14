package biouml.standard.filter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class BiopolimerDiagramFilterBeanInfo extends BeanInfoEx
{
    public BiopolimerDiagramFilterBeanInfo()
    {
        super(BiopolimerDiagramFilter.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName     (getResourceString("PN_DIAGRAM_FILTER"));
        beanDescriptor.setShortDescription(getResourceString("PD_DIAGRAM_FILTER"));

        setCompositeEditor("enabled", new java.awt.BorderLayout());
    }

    @Override
    protected void initProperties() throws Exception
    {
        addHidden(new PropertyDescriptorEx("enabled", beanClass));

        add(new PropertyDescriptorEx("speciesFilter", beanClass));
        add(new PropertyDescriptorEx("cellTypeFilter", beanClass));
        add(new PropertyDescriptorEx("inducerFilter", beanClass));
    }
}

