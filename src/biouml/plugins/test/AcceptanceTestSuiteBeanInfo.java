package biouml.plugins.test;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class AcceptanceTestSuiteBeanInfo extends BeanInfoEx
{
    public AcceptanceTestSuiteBeanInfo()
    {
        super(AcceptanceTestSuite.class, "biouml.plugins.test.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_ACCEPTANCE_TEST"));
        beanDescriptor.setShortDescription(getResourceString("CD_ACCEPTANCE_TEST"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptor pd = new PropertyDescriptorEx("name", beanClass, "getName", null);
        add(pd, getResourceString("PN_ACCEPTANCE_TEST_NAME"), getResourceString("PD_ACCEPTANCE_TEST_NAME"));

        pd = new PropertyDescriptorEx("stateName", beanClass, "getStateName", "setStateName");
        HtmlPropertyInspector.setDisplayName(pd, "ST");
        add(pd, getResourceString("PN_ACCEPTANCE_TEST_STATE"), getResourceString("PD_ACCEPTANCE_TEST_STATE"));
        
        pd = new PropertyDescriptorEx("timeLimit", beanClass, "getTimeLimit", "setTimeLimit");
        HtmlPropertyInspector.setDisplayName(pd, "TI");
        add(pd, getResourceString("PN_ACCEPTANCE_TEST_TIME"), getResourceString("PD_ACCEPTANCE_TEST_TIME"));
        
        add(new PropertyDescriptorEx("maxTime", beanClass), getResourceString("PN_COMPLETION_TIME"), getResourceString("PD_COMPLETION_TIME"));
    }
}
