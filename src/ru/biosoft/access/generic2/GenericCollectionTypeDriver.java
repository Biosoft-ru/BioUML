package ru.biosoft.access.generic2;

import java.io.File;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.ProtectedElement;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.security.NetworkDataCollection;
import ru.biosoft.access.security.PrivilegedAction;
import ru.biosoft.access.security.ProtectedDataCollection;
import ru.biosoft.access.security.SecurityManager;

public class GenericCollectionTypeDriver extends GenericElementTypeDriver
{
    @Override
    protected boolean isSupported(Class<? extends DataElement> clazz)
    {
        return FolderCollection.class.isAssignableFrom( clazz );
    }

    @Override
    protected void doRemove(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException
    {
        DataElement de = gdc.getFromCache( properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY ) );
        if(de == null)
            de = doGet( gdc, folder, properties );
        final ru.biosoft.access.core.DataElement finalDE = de;
        try
        {
            SecurityManager.runPrivileged( new PrivilegedAction()
            {
                @Override
                public Object run() throws Exception
                {
                    DataElement child = DataCollectionUtils.fetchPrimaryElementPrivileged( finalDE );
                    if(child instanceof GenericDataCollection2)
                        ( (GenericDataCollection2)child ).clear();
                    return null;
                }
            } );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    protected void doPut(GenericDataCollection2 gdc, File folder, DataElement de, Properties properties) throws LoggedException
    {
        // Nothing is necessary
    }

    @Override
    protected boolean isLeaf(GenericDataCollection2 gdc, Properties properties)
    {
        return false;
    }

    @Override
    protected long estimateSize(GenericDataCollection2 gdc, File folder, Properties properties, boolean recalc)
    {
        // TODO implement
        return super.estimateSize( gdc, folder, properties, recalc );
    }

    @Override
    protected DataElement doGet(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException
    {
        try
        {
            DataCollection result = getElementClass( properties ).getConstructor( ru.biosoft.access.core.DataCollection.class, Properties.class ).newInstance( gdc, properties ).cast( GenericDataCollection2.class );
            DataCollection outerParent = gdc.getCompletePath().optDataCollection();
            // Need to protect if outer is protected
            if(outerParent instanceof ProtectedElement)
            {
                Properties protectedProperties = new Properties();
                protectedProperties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY));
                protectedProperties.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, ProtectedDataCollection.class.getName());
                protectedProperties.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, result);
                result = new NetworkDataCollection(outerParent, protectedProperties);
            }
            return result;
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

}
