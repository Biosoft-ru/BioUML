package biouml.plugins.sabiork;

import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

public class SabioDiagramDataCollectionBeanInfo extends VectorDataCollectionBeanInfo
{

    public SabioDiagramDataCollectionBeanInfo()
    {
        super(SabioDiagramDataCollection.class, MessageBundle.class.getName());

        initResources("biouml.plugins.sabiork.MessageBundle");

        beanDescriptor.setDisplayName(getResourceString("CN_SABIO_DIAGRAM_DC"));
        beanDescriptor.setShortDescription(getResourceString("CD_SABIO_DIAGRAM_DC"));
    }
}
