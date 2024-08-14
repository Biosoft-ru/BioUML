package ru.biosoft.tasks;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * Bean info for task info wrapper
 */
public class TaskInfoWrapperBeanInfo extends BeanInfoEx
{
    public TaskInfoWrapperBeanInfo()
    {
        super(TaskInfoWrapper.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde;

        pde = new PropertyDescriptorEx("type", beanClass.getMethod("getType"), null);
        pde.setReadOnly(true);
        add(pde, getResourceString("PN_TYPE"), getResourceString("PD_TYPE"));

        pde = new PropertyDescriptorEx("source", beanClass.getMethod("getSource"), null);
        pde.setReadOnly(true);
        add(pde, getResourceString("PN_SOURCE"), getResourceString("PD_SOURCE"));

        pde = new PropertyDescriptorEx("startTime", beanClass.getMethod("getStartTimeStr"), null);
        pde.setReadOnly(true);
        add(pde, getResourceString("PN_START"), getResourceString("PD_START"));

        pde = new PropertyDescriptorEx("endTime", beanClass.getMethod("getEndTimeStr"), null);
        pde.setReadOnly(true);
        add(pde, getResourceString("PN_END"), getResourceString("PD_END"));

        pde = new PropertyDescriptorEx("status", beanClass.getMethod("getStatus"), null);
        pde.setReadOnly(true);
        add(pde, getResourceString("PN_STATUS"), getResourceString("PD_STATUS"));

        pde = new PropertyDescriptorEx("logInfo", beanClass.getMethod("getLogInfo"), null);
        pde.setReadOnly(true);
        add(pde, getResourceString("PN_LOG"), getResourceString("PD_LOG"));
    }
}