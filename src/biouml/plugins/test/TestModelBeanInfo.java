package biouml.plugins.test;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class TestModelBeanInfo extends BeanInfoEx
{
    public TestModelBeanInfo()
    {
        super(TestModel.class, "biouml.plugins.test.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_TEST_MODEL"));
        beanDescriptor.setShortDescription(getResourceString("CD_TEST_MODEL"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptor pd = new PropertyDescriptorEx("name", beanClass, "getName", null);
        add(pd, getResourceString("PN_TEST_MODEL_NAME"), getResourceString("PD_TEST_MODEL_NAME"));

        pd = new PropertyDescriptorEx("modelPath", beanClass, "getModelPath", "setModelPath");
        HtmlPropertyInspector.setDisplayName(pd, "MP");
        add(pd, getResourceString("PN_TEST_MODEL_PATH"), getResourceString("PD_TEST_MODEL_PATH"));
        
        pd = new PropertyDescriptorEx("acceptanceTests", beanClass, "getAcceptanceTests", null);
        add(pd, getResourceString("PN_TEST_MODEL_ACCTEST"), getResourceString("PD_TEST_MODEL_ACCTEST"));
    }
}
