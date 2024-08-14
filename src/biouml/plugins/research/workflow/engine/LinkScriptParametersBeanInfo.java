package biouml.plugins.research.workflow.engine;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.plugins.javascript.JSElement;
import biouml.plugins.research.MessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;

public class LinkScriptParametersBeanInfo extends BeanInfoEx
{
    public LinkScriptParametersBeanInfo()
    {
        super(LinkScriptParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_SCRIPT_PROPERTIES"));
        beanDescriptor.setShortDescription(getResourceString("CD_SCRIPT_PROPERTIES"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("scriptPath", beanClass, JSElement.class), getResourceString("PN_SCRIPT_PATH"), getResourceString("PD_SCRIPT_PATH"));
    }
}