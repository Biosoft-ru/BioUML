package ru.biosoft.plugins.javascript;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class FunctionInfoBeanInfo extends BeanInfoEx
{
    public FunctionInfoBeanInfo()
    {
        super(FunctionInfo.class, "ru.biosoft.plugins.javascript.MessageBundle");
        beanDescriptor.setDisplayName     (getResourceString("CN_FUNCTION_INFO"));
        beanDescriptor.setShortDescription(getResourceString("CD_FUNCTION_INFO"));
    }

    @Override
    public void initProperties() throws Exception
    {
        HtmlPropertyInspector.setHtmlGeneratorMethod(beanDescriptor, beanClass.getMethod("toString"));

        add(new PropertyDescriptorEx("name", beanClass, "getName", null),
                getResourceString("PN_FUNCTION_NAME"),
                getResourceString("PD_FUNCTION_NAME"));

        PropertyDescriptorEx pde = new PropertyDescriptorEx("description", beanClass);
        pde.setReadOnly(true);
        add(pde,
            getResourceString("PN_FUNCTION_DESCRIPTION"),
            getResourceString("PD_FUNCTION_DESCRIPTION"));

        pde = new PropertyDescriptorEx("arguments", beanClass);
        pde.setReadOnly(true);
        add(pde,
            getResourceString("PN_FUNCTION_ARGUMENTS"),
            getResourceString("PD_FUNCTION_ARGUMENTS"));

        pde = new PropertyDescriptorEx("returnedValue", beanClass);
        pde.setReadOnly(true);
        add(pde,
            getResourceString("PN_FUNCTION_RETURNED_VALUE"),
            getResourceString("PD_FUNCTION_RETURNED_VALUE"));

        pde = new PropertyDescriptorEx("exceptions", beanClass);
        pde.setReadOnly(true);
        pde.setHidden(beanClass.getMethod("isExceptionsHidden"));
        add(pde,
            getResourceString("PN_FUNCTION_EXCEPTIONS"),
            getResourceString("PD_FUNCTION_EXCEPTIONS"));

        pde = new PropertyDescriptorEx("examples", beanClass);
        pde.setReadOnly(true);
        pde.setHidden(beanClass.getMethod("isExamplesHidden"));
        add(pde,
            getResourceString("PN_FUNCTION_EXAMPLES"),
            getResourceString("PD_FUNCTION_EXAMPLES"));
    }
}

