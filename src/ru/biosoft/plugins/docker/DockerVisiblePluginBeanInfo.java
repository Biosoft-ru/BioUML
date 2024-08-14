package ru.biosoft.plugins.docker;

import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

/**
 * @pending Document
 */
public class DockerVisiblePluginBeanInfo extends VectorDataCollectionBeanInfo
{
    public DockerVisiblePluginBeanInfo()
    {
        super(DockerVisiblePlugin.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName     ( getResourceString("CN_DOCKER_PLUGIN") );
        beanDescriptor.setShortDescription( getResourceString("CD_DOCKER_PLUGIN") );
    }
}
