package biouml.plugins.sabiork;

import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

public class SabioDataCollectionBeanInfo extends VectorDataCollectionBeanInfo
{

    public SabioDataCollectionBeanInfo()
    {
        super(SabioDataCollection.class, MessageBundle.class.getName());

        initResources("biouml.plugins.sabiork.MessageBundle");

        beanDescriptor.setDisplayName(getResourceString("CN_SABIO_DC"));
        beanDescriptor.setShortDescription(getResourceString("CD_SABIO_DC"));
    }
}
