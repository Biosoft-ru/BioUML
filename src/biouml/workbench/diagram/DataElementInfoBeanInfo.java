package biouml.workbench.diagram;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class DataElementInfoBeanInfo extends BeanInfoEx
{
    public DataElementInfoBeanInfo()
    {
        super(DataElementInfo.class, biouml.workbench.resources.MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde;

        pde = new PropertyDescriptorEx("moduleName", beanClass.getMethod("getModuleName"), null);
        add(pde,
            getResourceString("PN_DIAGRAM_ELEMENT_INFO_DATABASE_NAME"),
            getResourceString("PD_DIAGRAM_ELEMENT_INFO_DATABASE_NAME"));

        pde = new PropertyDescriptorEx("kernelName", beanClass.getMethod("getKernelName"), null);
        add(pde,
            getResourceString("PN_DIAGRAM_ELEMENT_INFO_KERNEL_NAME"),
            getResourceString("PD_DIAGRAM_ELEMENT_INFO_KERNEL_NAME"));

        pde = new PropertyDescriptorEx("kernelTitle", beanClass.getMethod("getKernelTitle"), null);
        add(pde,
            getResourceString("PN_DIAGRAM_ELEMENT_INFO_KERNEL_TITLE"),
            getResourceString("PD_DIAGRAM_ELEMENT_INFO_KERNEL_TITLE"));

        pde = new PropertyDescriptorEx("kernelType", beanClass.getMethod("getKernelType"), null);
        add(pde,
            getResourceString("PN_DIAGRAM_ELEMENT_INFO_KERNEL_TYPE"),
            getResourceString("PD_DIAGRAM_ELEMENT_INFO_KERNEL_TYPE"));
    }
}
