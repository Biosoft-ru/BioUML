package biouml.workbench.graphsearch;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SearchElementBeanInfo extends BeanInfoEx
{
    public SearchElementBeanInfo()
    {
        super(SearchElement.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        
        initResources(MessageBundle.class.getName());
        
        PropertyDescriptor pde = new PropertyDescriptorEx("add", beanClass, "isAdd", "setAdd");
        add(pde, getResourceString("PN_SEARCH_ELEMENT_ADD"), getResourceString("PD_SEARCH_ELEMENT_ADD"));
        
        pde = new PropertyDescriptorEx("use", beanClass, "isUse", "setUse");
        add(pde, getResourceString("PN_SEARCH_ELEMENT_USE"), getResourceString("PD_SEARCH_ELEMENT_USE"));

        pde = new PropertyDescriptorEx("baseName", beanClass.getMethod("getBaseName"), null);
        add(pde, getResourceString("PN_SEARCH_ELEMENT_BASE_NAME"),
                getResourceString("PD_SEARCH_ELEMENT_BASE_NAME"));

        pde = new PropertyDescriptorEx("baseTitle", beanClass.getMethod("getBaseTitle"), null);
        add(pde, getResourceString("PN_SEARCH_ELEMENT_BASE_TITLE"),
                getResourceString("PD_SEARCH_ELEMENT_BASE_TITLE"));

        pde = new PropertyDescriptorEx("baseType", beanClass.getMethod("getBaseType"), null);
        add(pde, getResourceString("PN_SEARCH_ELEMENT_BASE_TYPE"),
                getResourceString("PD_SEARCH_ELEMENT_BASE_TYPE"));
    }
}
