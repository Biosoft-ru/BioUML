package ru.biosoft.tasks;

import java.util.logging.Logger;

import ru.biosoft.access.support.SetAttributesCommand;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

/**
 * Bean info for task element
 */
public class TaskInfoBeanInfo extends BeanInfoEx
{
    protected static final Logger log = Logger.getLogger(TaskInfoBeanInfo.class.getName());

    public TaskInfoBeanInfo()
    {
        this(TaskInfo.class, "TASK_INFO");
    }

    protected TaskInfoBeanInfo(Class beanClass, String key)
    {
        this(beanClass, key, MessageBundle.class.getName());
    }

    protected TaskInfoBeanInfo(Class beanClass, String key, String messageBundle)
    {
        super(beanClass, messageBundle);

        if( key != null && messageBundle != null )
        {
            beanDescriptor.setDisplayName(getResourceString("CN_" + key));
            beanDescriptor.setShortDescription(getResourceString("CD_" + key));
        }
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde;
        pde = new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null);
        HtmlPropertyInspector.setDisplayName(pde, "ID");
        add(pde, getResourceString("PN_IDENTIFIER"), getResourceString("PD_IDENTIFIER"));

        pde = new PropertyDescriptorEx("type", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TY");
        add(pde, getResourceString("PN_TYPE"), getResourceString("PD_TYPE"));

        pde = new PropertyDescriptorEx("source", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "SO");
        add(pde, getResourceString("PN_SOURCE"), getResourceString("PD_SOURCE"));
        
        pde = new PropertyDescriptorEx("data", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "DT");
        add(pde, getResourceString("PN_DATA"), getResourceString("PD_DATA"));
        
        pde = new PropertyDescriptorEx("attributes", beanClass.getMethod("getAttributes"), null);
        pde.setValue("commandClass", SetAttributesCommand.class);
        HtmlPropertyInspector.setDisplayName(pde, "AT");
        add(pde, getResourceString("PN_ATTRIBUTES"), getResourceString("PD_ATTRIBUTES"));
        
        pde = new PropertyDescriptorEx("startTime", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "ST");
        add(pde, getResourceString("PN_START"), getResourceString("PD_START"));

        pde = new PropertyDescriptorEx("endTime", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "EN");
        add(pde, getResourceString("PN_END"), getResourceString("PD_END"));
        
        pde = new PropertyDescriptorEx("logInfo", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "LG");
        add(pde, getResourceString("PN_LOG"), getResourceString("PD_LOG"));
     }
}
