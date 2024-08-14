package ru.biosoft.access.generic2;

import java.io.File;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.InvalidElement;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.InternalException;
import ru.biosoft.access.core.DataElementGetException;

public class UnknownElementDriver extends GenericElementTypeDriver
{
    @Override
    protected boolean isSupported(Class<? extends DataElement> clazz)
    {
        return InvalidElement.class.isAssignableFrom( clazz );
    }

    @Override
    protected void doPut(GenericDataCollection2 gdc, File folder, DataElement de, Properties properties) throws LoggedException
    {
        throw new InternalException( "Cannot save invalid element" );
    }

    @Override
    protected DataElement doGet(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException
    {
        DataElementPath path = gdc.getCompletePath().getChildPath(properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY));
        return new InvalidElement(properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY), gdc, new DataElementGetException(new Exception(
                "Invalid driver: " + ( properties.getProperty(GenericDataCollection2.DRIVER_PROPERTY, "(none)") )), path));
    }

    @Override
    protected boolean isLeaf(GenericDataCollection2 gdc, Properties properties)
    {
        return true;
    }

    @Override
    protected Class<? extends DataElement> getElementClass(Properties properties)
    {
        return InvalidElement.class;
    }
}
