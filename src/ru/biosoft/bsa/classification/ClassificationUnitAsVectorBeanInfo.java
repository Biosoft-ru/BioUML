package ru.biosoft.bsa.classification;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @pending refine
 * @pending provide HTML formatter.
 */
public class ClassificationUnitAsVectorBeanInfo extends BeanInfoEx
{
    public ClassificationUnitAsVectorBeanInfo()
    {
        super(ClassificationUnitAsVector.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASSIFICATION_UNIT"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASSIFICATION_UNIT"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("classNumber", beanClass, "getClassNumber", null),
                getResourceString("PN_CLASSIFICATION_UNIT_CLASS_NUMBER"),
                getResourceString("PN_CLASSIFICATION_UNIT_CLASS_NUMBER"));

        add(new PropertyDescriptorEx("className", beanClass, "getClassName", null),
                getResourceString("PN_CLASSIFICATION_UNIT_CLASS_NAME"),
                getResourceString("PN_CLASSIFICATION_UNIT_CLASS_NAME"));

        add(new PropertyDescriptorEx("description", beanClass, "getDescription", null),
                getResourceString("PN_CLASSIFICATION_UNIT_DESCRIPTION"),
                getResourceString("PN_CLASSIFICATION_UNIT_DESCRIPTION"));

        add(new PropertyDescriptorEx("size", beanClass, "getSize", null),
                getResourceString("PN_CLASSIFICATION_UNIT_SIZE"),
                getResourceString("PN_CLASSIFICATION_UNIT_SIZE"));
        
        add(new PropertyDescriptorEx("attributes", beanClass, "getAttributes", null),
                getResourceString("PN_CLASSIFICATION_UNIT_ATTRIBUTES"),
                getResourceString("PN_CLASSIFICATION_UNIT_ATTRIBUTES"));

//        add(new PropertyDescriptorEx("info", beanClass, "getInfo", null), "info", "info");
    }
}

