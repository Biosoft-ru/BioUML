package ru.biosoft.access.generic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.ProtectedElement;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.BiosoftFileNotFoundException;
import ru.biosoft.access.security.NetworkDataCollection;
import ru.biosoft.access.security.PrivilegedAction;
import ru.biosoft.access.security.ProtectedDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.ExProperties;

public class DataElementGenericCollectionTypeDriver implements DataElementTypeDriver
{
    protected static final Logger log = Logger.getLogger(DataElementGenericCollectionTypeDriver.class.getName());

    public static final String TYPE_DRIVER_KEY = "generic_type_driver";

    @Override
    public DataElement doGet(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        DataCollection primaryDC = gdc.getTypeSpecificCollection(this);
        if( primaryDC.contains(dei.getName()) )
        {
            return primaryDC.get(dei.getName());
        }
        File configPath = new File(gdc.getRootDirectory(), dei.getName());
        String fileName = DataCollectionConfigConstants.DEFAULT_CONFIG_FILE;
        File file = new File(configPath, fileName);
        if(! file.exists())
            throw new BiosoftFileNotFoundException(file.getAbsolutePath());
        Properties properties = new ExProperties(file);
        DataCollection result = CollectionFactory.createCollection(gdc.getRealParent(), properties);
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
        primaryDC.put(result);
        return result;
    }
    @Override
    public void doPut(GenericDataCollection gdc, DataElement de, DataElementInfo dei) throws Exception
    {
        DataCollection primaryDC = gdc.getTypeSpecificCollection(this);
        primaryDC.put(de);
        GenericDataCollection child = (GenericDataCollection)de;
        child.storeConfigs();
    }
    
    @Override
    public void doRemove(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        DataCollection primaryDC = gdc.getTypeSpecificCollection(this);
        primaryDC.remove(dei.getName());
        DataElement de = gdc.get(dei.getName());
        if(!DataCollectionUtils.checkPrimaryElementType(de, GenericDataCollection.class)) return;
        DataCollection<?> curDC = (DataCollection<?>)de;
        curDC.setNotificationEnabled(false);
        curDC.setPropagationEnabled(false);
        for(Object name: curDC.getNameList().toArray())
        {
            curDC.remove(name.toString());
        }
        curDC.close();
        gdc.release(dei.getName());
        
        File folder = new File(gdc.getRootDirectory(), dei.getName());
        if( folder.exists() )
        {
            ApplicationUtils.removeDir(folder);
        }
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> childClass)
    {
        if( GenericDataCollection.class.isAssignableFrom(childClass) )
        {
            return true;
        }
        return false;
    }

    @Override
    public DataCollection createBaseCollection(GenericDataCollection gdc)
    {
        try
        {
            return new WeakBaseCollection(gdc.getName(), null, new Properties());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not create data collection", e);
            return null;
        }
    }

    @Override
    public boolean isLeafElement(GenericDataCollection gdc, DataElementInfo dei)
    {
        return false;
    }
    
    private static class WeakBaseCollection extends AbstractDataCollection<DataElement>
    {
        protected WeakBaseCollection(String name, DataCollection parent, Properties properties)
        {
            super(name, parent, properties);
        }

        @Override
        public @Nonnull List<String> getNameList()
        {
            return new ArrayList<>(v_cache.keySet());
        }

        @Override
        protected DataElement doGet(String name) throws Exception
        {
            return null;
        }

        @Override
        protected void doPut(DataElement dataElement, boolean isNew) throws Exception
        {
        }

        @Override
        protected void doRemove(String name) throws Exception
        {
        }
    }

    @Override
    public long estimateSize(GenericDataCollection gdc, DataElementInfo dei, boolean recalc)
    {
        try
        {
            final DataCollection<?> collection = (DataCollection<?>)doGet(gdc, dei);
            if(collection == null || collection.isEmpty()) return 0;
            if(recalc)
                return ( (GenericDataCollection)DataCollectionUtils.fetchPrimaryCollectionPrivileged(collection) ).recalculateSize();
            return (Long)SecurityManager.runPrivileged(new PrivilegedAction()
            {
                @Override
                public Object run() throws Exception
                {
                    return ( (GenericDataCollection)DataCollectionUtils.fetchPrimaryCollectionPrivileged(collection) ).getDiskSize();
                }
            });
        }
        catch( Exception e )
        {
        }
        return -1;
    }
}
