package ru.biosoft.access.generic2;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.util.ExProperties;

/**
 * Driver which supports some items normally located in Repository
 * TODO: support fully
 */
public class GenericRepositoryTypeDriver extends GenericElementTypeDriver
{
    @Override
    public DataElement doGet(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException
    {
        String path = folder.getAbsolutePath();
        properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, path );
        properties.setProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, path );
        return CollectionFactory.createCollection( gdc, properties );
    }

    @Override
    public void doPut(GenericDataCollection2 gdc, File folder, DataElement de, Properties properties) throws LoggedException
    {
        try
        {
            if(de instanceof TransformedDataCollection)
            {
                TransformedDataCollection dc = (TransformedDataCollection)de;
                Properties primaryProperties = (Properties)dc.getPrimaryCollection().getInfo().getProperties().clone();
                primaryProperties.remove(DataCollectionConfigConstants.CONFIG_FILE_PROPERTY);
                primaryProperties.remove(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
                primaryProperties.remove(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
                String primaryConfig = properties.getProperty(DataCollectionConfigConstants.NEXT_CONFIG);
                if(primaryConfig == null)
                {
                    primaryConfig = "default.primary.config";
                    properties.setProperty(DataCollectionConfigConstants.NEXT_CONFIG, primaryConfig);
                }
                ExProperties.store( primaryProperties, new File(folder, primaryConfig) );
            }
        }
        catch( IOException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    public boolean isLeaf(GenericDataCollection2 gdc, Properties properties)
    {
        return false;
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> childClass)
    {
        return ru.biosoft.access.core.DataCollection.class.isAssignableFrom(childClass);
    }
}
