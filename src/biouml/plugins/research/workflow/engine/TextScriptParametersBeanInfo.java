package biouml.plugins.research.workflow.engine;

import one.util.streamex.StreamEx;

import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.research.MessageBundle;
import biouml.plugins.research.workflow.items.TextScriptEditor;

public class TextScriptParametersBeanInfo extends BeanInfoEx2<TextScriptParameters>
{
    public TextScriptParametersBeanInfo()
    {
        super(TextScriptParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_SCRIPT_PROPERTIES"));
        beanDescriptor.setShortDescription(getResourceString("CD_SCRIPT_PROPERTIES"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "script" ).editor( TextScriptEditor.class ).title( "PN_SCRIPT_SOURCE" ).description( "PD_SCRIPT_SOURCE" ).add();
        property( "scriptType" ).tags( bean -> StreamEx.ofKeys( ScriptTypeRegistry.getScriptTypes() ) ).title( "PN_SCRIPT_TYPE" )
                .description( "PD_SCRIPT_TYPE" ).add();
    }
}