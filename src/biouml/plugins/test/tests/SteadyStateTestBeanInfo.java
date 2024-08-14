package biouml.plugins.test.tests;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SteadyStateTestBeanInfo extends BeanInfoEx
{
    public SteadyStateTestBeanInfo()
    {
        super(SteadyStateTest.class, "biouml.plugins.test.tests.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_STEADY_STATE"));
        beanDescriptor.setShortDescription(getResourceString("CD_STEADY_STATE"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pd = new PropertyDescriptorEx("from", beanClass, "getFrom", "setFrom");
        add(pd, getResourceString("PN_STEADY_STATE_FROM"), getResourceString("PD_STEADY_STATE_FROM"));

        pd = new PropertyDescriptorEx("to", beanClass, "getTo", "setTo");
        add(pd, getResourceString("PN_STEADY_STATE_TO"), getResourceString("PD_STEADY_STATE_TO"));

        pd = new PropertyDescriptorEx("rTol", beanClass, "getRTol", "setRTol");
        pd.setNumberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE);
        add(pd, getResourceString("PN_STEADY_STATE_RTOL"), getResourceString("PD_STEADY_STATE_RTOL"));

        pd = new PropertyDescriptorEx("variables", beanClass, "getVariables", "setVariables");
        add(pd, getResourceString("PN_STEADY_STATE_VARIABLES"), getResourceString("PD_STEADY_STATE_VARIABLES"));
    }
}
