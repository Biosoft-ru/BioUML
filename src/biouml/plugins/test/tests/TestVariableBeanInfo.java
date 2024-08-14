package biouml.plugins.test.tests;

import java.beans.PropertyDescriptor;

import biouml.plugins.test.editors.TestVariableDiagramEditor;
import biouml.plugins.test.editors.TestVariableNameEditor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class TestVariableBeanInfo extends BeanInfoEx
{
    public TestVariableBeanInfo()
    {
        super(TestVariable.class, "biouml.plugins.test.tests.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_VARIABLE"));
        beanDescriptor.setShortDescription(getResourceString("CD_VARIABLE"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptor pd = new PropertyDescriptorEx("subDiagram", beanClass, "getSubDiagramName", "setSubDiagramName");
        pd.setPropertyEditorClass(TestVariableDiagramEditor.class);
        add(pd, getResourceString("PN_SUBDIAGRAM_NAME"), getResourceString("PD_SUBDIAGRAM_NAME"));

        pd = new PropertyDescriptorEx("name", beanClass, "getName", "setName");
        pd.setPropertyEditorClass(TestVariableNameEditor.class);
        add(pd, getResourceString("PN_VARIABLE_NAME"), getResourceString("PD_VARIABLE_NAME"));
    }
}
