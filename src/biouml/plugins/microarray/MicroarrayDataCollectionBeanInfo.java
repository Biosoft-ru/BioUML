package biouml.plugins.microarray;

import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

public class MicroarrayDataCollectionBeanInfo extends VectorDataCollectionBeanInfo
{
    public MicroarrayDataCollectionBeanInfo()
    {
        super(MicroarrayDataCollection.class, MessageBundle.class.getName());

        initResources("biouml.plugins.microarray.MessageBundle");

        beanDescriptor.setDisplayName(getResourceString("CN_MICROARRAY_DC"));
        beanDescriptor.setShortDescription(getResourceString("CD_MICROARRAY_DC"));
    }

    public MicroarrayDataCollectionBeanInfo(Class c, String messageBundle)
    {
        super(c, messageBundle);
    }
}
