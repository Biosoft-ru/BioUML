package biouml.model;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class CollectionDescriptionBeanInfo extends BeanInfoEx
{
    public CollectionDescriptionBeanInfo()
    {
        super(CollectionDescription.class, null);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        add(new PropertyDescriptorEx("moduleName", beanClass), "Module", "Module name");
        add(new PropertyDescriptorEx("sectionName", beanClass), "Section", "Section name");
        add(new PropertyDescriptorEx("typeName", beanClass), "Collection", "Collection name");
        add(new PropertyDescriptorEx("readOnly", beanClass), "Read Only", "Is read only");
    }
}
