package ru.biosoft.access.generic;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * Type registry for GenericDataCollection
 */
public class DataElementTypeRegistry
{
    private static final ObjectExtensionRegistry<DataElementTypeDriver> drivers = new ObjectExtensionRegistry<>(
            "ru.biosoft.access.typeDriver", DataElementTypeDriver.class);

    public static final String DRIVER_CLASS = "class";

    public static DataElementTypeDriver getDriver(String className)
    {
        return drivers.getExtension(className);
    }

    public static DataElement doGet(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        String driverClassName = dei.getProperty(DataElementInfo.DRIVER_CLASS);
        DataElementTypeDriver driver = drivers.getExtension(driverClassName);
        if(driver == null)
        {
            Class<? extends DataElement> elementClass = ClassLoading.loadSubClass( dei.getStrictProperty(DataElementInfo.ELEMENT_CLASS), dei.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY), DataElement.class );
            driver = lookForDriver(elementClass);
        }
        return driver.doGet(gdc, dei);
    }

    public static void doRemove(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        String driverClassName = dei.getProperty(DataElementInfo.DRIVER_CLASS);
        DataElementTypeDriver driver = drivers.getExtension(driverClassName);
        if(driver == null)
        {
            Class<? extends DataElement> elementClass = ClassLoading.loadSubClass( dei.getStrictProperty(DataElementInfo.ELEMENT_CLASS), dei.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY), DataElement.class );
            driver = lookForDriver(elementClass);
        }
        driver.doRemove(gdc, dei);
    }

    protected static DataElementTypeDriver lookForDriver(Class<? extends DataElement> childClass) throws Exception
    {
        for( DataElementTypeDriver driver : drivers )
        {
            try
            {
                if( driver.isSupported(childClass) )
                {
                    return driver;
                }
            }
            catch( Exception e )
            {
            }
        }
        if(DataCollection.class.isAssignableFrom(childClass))
        {
            DataElementTypeDriver extension = drivers.getExtension(RepositoryTypeDriver.class.getName());
            if( extension == null )
                throw new InternalException( "Unable to find driver for " + RepositoryTypeDriver.class.getName()
                        + ". Check whether extension registry is properly initialized!" );
            return extension;
        }
        throw new Exception("Can not find DataElementTypeDriver for element of type " + childClass.getName());
    }
}
