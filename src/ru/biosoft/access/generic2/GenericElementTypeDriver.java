package ru.biosoft.access.generic2;

import java.io.File;
import java.util.Properties;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.exception.ExceptionRegistry;

/**
 * Abstract class for generic object access
 */
public abstract class GenericElementTypeDriver
{
    public static final String DATA_FILE_NAME = "element.dat";

    /**
     * Checks whether specified element is supported by this driver
     * @param clazz TODO
     * @return true if this driver supports specified element, false otherwise
     */
    abstract protected boolean isSupported(Class<? extends DataElement> clazz);

    /**
     * Saves ru.biosoft.access.core.DataElement into GenericDataCollection2
     * @param gdc GenericDataCollection2 to store element to
     * @param folder element folder (it's already created and properties are written into default.config upon this call)
     * @param de ru.biosoft.access.core.DataElement to store. It's guaranteed that isSupported previously returned true for the type of this ru.biosoft.access.core.DataElement
     * @param properties Element properties
     * @throws LoggedException if some error occurred during the operation
     */
    abstract protected void doPut(GenericDataCollection2 gdc, File folder, DataElement de, Properties properties) throws LoggedException;

    /**
     * Fetches specified ru.biosoft.access.core.DataElement from GenericDataCollection2
     * @param gdc GenericDataCollection2 to fetch element from
     * @param folder element folder
     * @param properties Properties describing element to fetch
     * properties.getProperty(DataCollectionConfigConstants.CLASS_PROPERTY) may be of interest
     * @return fetched ru.biosoft.access.core.DataElement
     * @throws LoggedException if some error occurred during the fetching
     */
    abstract protected DataElement doGet(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException;

    /**
     * Removes specified ru.biosoft.access.core.DataElement from GenericDataCollection2
     * @param gdc GenericDataCollection to remove element from
     * @param folder element folder (it will be removed automatically after element removal finishes)
     * @param properties DataElementInfo describing element to remove
     * @throws LoggedException if error occurred during the element deletion
     * note that element will be removed (i.e. folder removed) even if exception is thrown
     */
    protected void doRemove(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException
    {
        try
        {
            DataElement de = gdc.getFromCache(properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY ));
            if( de instanceof DataCollection )
                ( (DataCollection)de ).close();
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    /**
     * Checks whether specified ru.biosoft.access.core.DataElement is a leaf (should not have subelements in the tree)
     * @param gdc GenericDataCollection2 in which element resides
     * @param folder element folder
     * @param properties Properties describing element
     * @return true if element is a leaf, false otherwise
     */
    protected boolean isLeaf(GenericDataCollection2 gdc, Properties properties)
    {
        try
        {
            return !ru.biosoft.access.core.DataCollection.class.isAssignableFrom( getElementClass( properties ) );
        }
        catch( LoggedClassNotFoundException e )
        {
            return true;
        }
    }

    /**
     * Estimate the drivespace size taken by the data element in bytes
     * @param gdc parent collection
     * @param folder element folder
     * @param properties Properties describing element
     * @param recalc whether to recalculate the value or use cached one (if applicable)
     * @return size in bytes.
     * Default implementation returns the total length of all files
     * Size of default.config is not counted
     */
    protected long estimateSize(GenericDataCollection2 gdc, File folder, Properties properties, boolean recalc)
    {
        long size = 0;
        for(File file : folder.listFiles())
        {
            if( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE.equals( file.getName() ) || file.isDirectory() )
                continue;
            size += file.length();
        }
        return size;
    }

    protected Class<? extends DataElement> getElementClass(Properties properties)
    {
        return ClassLoading.loadSubClass( properties.getProperty( DataCollectionConfigConstants.CLASS_PROPERTY ), DataElement.class );
    }
}
