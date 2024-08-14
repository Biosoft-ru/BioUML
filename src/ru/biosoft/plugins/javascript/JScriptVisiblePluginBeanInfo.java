package ru.biosoft.plugins.javascript;

import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

/**
 * @pending Document
 */
public class JScriptVisiblePluginBeanInfo extends VectorDataCollectionBeanInfo
{
    public JScriptVisiblePluginBeanInfo()
    {
        super(JScriptVisiblePlugin.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName     ( getResourceString("CN_JSCRIPT_PLUGIN") );
        beanDescriptor.setShortDescription( getResourceString("CD_JSCRIPT_PLUGIN") );
    }
}
