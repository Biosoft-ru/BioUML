package biouml.plugins.test.tests;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class IntervalTestBeanInfo extends BeanInfoEx
{
    public IntervalTestBeanInfo()
    {
        super(IntervalTest.class, "biouml.plugins.test.tests.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_INTERVAL"));
        beanDescriptor.setShortDescription(getResourceString("CD_INTERVAL"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptor pd = new PropertyDescriptorEx("valueFrom", beanClass, "getValueFrom", "setValueFrom");
        add(pd, getResourceString("PN_INTERVAL_VALUE_FROM"), getResourceString("PD_INTERVAL_VALUE_FROM"));

        pd = new PropertyDescriptorEx("valueTo", beanClass, "getValueTo", "setValueTo");
        add(pd, getResourceString("PN_INTERVAL_VALUE_TO"), getResourceString("PD_INTERVAL_VALUE_TO"));
        
        pd = new PropertyDescriptorEx("from", beanClass, "getFrom", "setFrom");
        add(pd, getResourceString("PN_INTERVAL_FROM"), getResourceString("PD_INTERVAL_FROM"));

        pd = new PropertyDescriptorEx("to", beanClass, "getTo", "setTo");
        add(pd, getResourceString("PN_INTERVAL_TO"), getResourceString("PD_INTERVAL_TO"));
        
        pd = new PropertyDescriptorEx("variables", beanClass, "getVariables", "setVariables");
        add(pd, getResourceString("PN_INTERVAL_VARIABLES"), getResourceString("PD_INTERVAL_VARIABLES"));
    }
}
