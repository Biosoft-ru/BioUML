package ru.biosoft.analysiscore;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class AnalysisMethodInfoBeanInfo extends BeanInfoEx
{
    protected AnalysisMethodInfoBeanInfo(Class<?> beanClass)
    {
        super(beanClass, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    public AnalysisMethodInfoBeanInfo()
    {
        this(AnalysisMethodInfo.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("name", beanClass, "getName", null), getResourceString("PN_METHOD_NAME"),
                getResourceString("PD_METHOD_NAME"));
        add(new PropertyDescriptorEx("shortDescription", beanClass, "getShortDescription", null), getResourceString("PN_METHOD_SHORT_DESCRIPTION"),
                getResourceString("PD_METHOD_SHORT_DESCRIPTION"));
        add(new PropertyDescriptorEx("description", beanClass, "getDescription", null), getResourceString("PN_METHOD_DESCRIPTION"),
                getResourceString("PD_METHOD_DESCRIPTION"));
        addHidden(new PropertyDescriptorEx("descriptionHTML", beanClass, "getDescriptionHTML", null));
        addHidden(new PropertyDescriptorEx("baseId", beanClass, "getBaseId", null));
    }
}