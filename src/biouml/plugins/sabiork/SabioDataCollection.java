package biouml.plugins.sabiork;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.util.ExProperties;

/**
 * Data collection based on SABIO-RK service
 */
public class SabioDataCollection<T extends DataElement> extends AbstractDataCollection<T>
{
    public static final String CACHE_CONFIG = "cacheConfig";

    protected ServiceProvider service;
    protected DataCollection<T> cacheDataCollection;

    public SabioDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        String className = properties.getProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
        if( className != null )
        {
            service = SabiorkUtility.getServiceProvider(className);
        }

        String cacheFile = properties.getProperty(CACHE_CONFIG);
        if( cacheFile != null )
        {
            String configPath = properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
            if( configPath != null )
                configPath = configPath.trim();
            String filePath = properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
            if( filePath != null )
                filePath = filePath.trim();
            if( filePath == null || filePath.equals("") )
                filePath = configPath;

            cacheFile = cacheFile.trim();
            try
            {
                Properties cacheProperties = new ExProperties(new File(configPath, cacheFile));
                if( cacheProperties.get(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY) == null )
                {
                    cacheProperties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, configPath);
                }
                if( cacheProperties.get(DataCollectionConfigConstants.FILE_PATH_PROPERTY) == null )
                {
                    cacheProperties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, filePath);
                }
                cacheDataCollection = CollectionFactory.createCollection(parent, cacheProperties);

                cacheDataCollection.getInfo().addUsedFile(new File(configPath + "/" + cacheFile));
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not load cache properties", e);
            }
        }
    }

    protected List<String> nameList = null;
    protected void initNameList()
    {
        if( nameList == null )
        {
            try
            {
                nameList = service.getNameList();
            }
            catch( Exception e )
            {
                nameList = new ArrayList<>();
                log.log(Level.SEVERE, "Can not init name list for '" + getName() + "'", e);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // redefine DataCollection methods due to Module instance lazy initialisation
    //

    @Override
    public boolean isMutable()
    {
        return true;
    }

    @Override
    public int getSize()
    {
        initNameList();
        return nameList.size();
    }

    @Override
    public boolean contains(String name)
    {
        initNameList();
        return nameList.contains(name);
    }

    @Override
    protected T doGet(String name) throws Exception
    {
        T de = null;
        if( cacheDataCollection != null )
        {
            ClassLoader oldThreadclassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            de = cacheDataCollection.get(name);
            Thread.currentThread().setContextClassLoader(oldThreadclassLoader);
            if( de != null )
            {
                return de;
            }
        }
        de = (T)service.getDataElement(this, name);
        if( ( de != null ) && ( cacheDataCollection != null ) )
        {
            cacheDataCollection.put(de);
        }
        return de;
    }
    
    @Override
    protected void doPut(T dataElement, boolean isNew) throws Exception
    {
        //Put is not available
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        //Can not remove. This is read only collection
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        initNameList();
        return Collections.unmodifiableList(nameList);
    }

    @Override
    public void close() throws Exception
    {
        if( cacheDataCollection != null )
        {
            cacheDataCollection.close();
        }
        super.close();
    }
}
