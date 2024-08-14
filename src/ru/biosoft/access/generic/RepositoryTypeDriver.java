package ru.biosoft.access.generic;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.CreateDataCollectionController;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.util.ExProperties;

/**
 * Driver which supports some items normally located in Repository
 */
public class RepositoryTypeDriver implements DataElementTypeDriver
{
    protected static final Logger log = Logger.getLogger(RepositoryTypeDriver.class.getName());
    
    public static final String FOLDER_NAME = "repository_collection.files";

    @Override
    public DataCollection createBaseCollection(GenericDataCollection gdc)
    {
        try
        {
            File folder = new File(gdc.getRootDirectory() + FOLDER_NAME);
            if( !folder.exists() )
            {
                folder.mkdirs();
            }
            File configFile = new File(folder, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
            Properties primary = null;
            if( configFile.exists() )
            {
                primary = new ExProperties(configFile);
            }
            else
            {
                primary = createCollectionProperties(gdc);
                ExProperties.store(primary, configFile);
            }
            primary.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, gdc.getRealParent());
            primary.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, folder.getPath());
            return CollectionFactory.createCollection(gdc.getOrigin(), primary);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not load data collection", e);
            return null;
        }
    }

    protected Properties createCollectionProperties(GenericDataCollection gdc)
    {
        Properties result = new ExProperties();
        result.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, InternalGenericRepository.class.getName());
        result.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, gdc.getName());
        return result;
    }

    @Override
    public DataElement doGet(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        DataCollection primaryDC = gdc.getTypeSpecificCollection(this);
        DataElement de = primaryDC.get(dei.getName());
        try
        {
            de = DataCollectionUtils.fetchPrimaryElement(de, Permission.WRITE);
        }
        catch(Exception e)
        {
        }
        return de;
    }

    @Override
    public void doPut(GenericDataCollection gdc, DataElement de, DataElementInfo dei) throws Exception
    {
        DataCollection primaryDC = gdc.getTypeSpecificCollection(this);
        primaryDC.put(de);
    }

    @Override
    public void doRemove(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        DataElement de = gdc.getFromCache(dei.getName());
        if(de instanceof DataCollection)
            ((DataCollection)de).close();
        gdc.getTypeSpecificCollection(this).remove(dei.getName());
    }

    @Override
    public boolean isLeafElement(GenericDataCollection gdc, DataElementInfo dei)
    {
        return false;
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> childClass)
    {
        return TransformedDataCollection.class.isAssignableFrom(childClass);
    }
    
    public static class InternalGenericRepository extends LocalRepository
    {
        public InternalGenericRepository(DataCollection parent, Properties properties) throws Exception
        {
            super(parent, properties);
            getInfo().getProperties().setProperty(LocalRepository.UNPROTECTED_PROPERTY, "true");
        }

        protected DataCollection getPrimaryCollection()
        {
            return (DataCollection)getInfo().getProperties().get(DataCollectionConfigConstants.PRIMARY_COLLECTION);
        }

        @Override
        public DataCollection createDataCollection(String name, Properties properties, String subDir, File[] files, CreateDataCollectionController controller) throws Exception
        {
            properties.put(PARENT_COLLECTION, getPrimaryCollection().getCompletePath().toString());
            return super.createDataCollection(name, properties, subDir, files, controller);
        }
    }

    @Override
    public long estimateSize(GenericDataCollection gdc, DataElementInfo dei, boolean recalc)
    {
        try
        {
            DataCollection<?> dc = (DataCollection<?>)doGet(gdc, dei);
            if(dc == null) return -1;
            long size = 0;
            for(File file: dc.getInfo().getUsedFiles())
            {
                size+=file.length();
            }
            return size;
        }
        catch( Exception e )
        {
        }
        return -1;
    }
}
