package ru.biosoft.access.generic;

import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import one.util.streamex.EntryStream;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.FileCollection;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.ExProperties;

public class DataElementFileTypeDriver implements DataElementTypeDriver
{
    protected static final Logger log = Logger.getLogger(DataElementFileTypeDriver.class.getName());

    public static final String FOLDER_NAME = "file_collection.files";
    public static final String CONFIG_FILE_NAME = "file_collection.config";

    public static final String TYPE_DRIVER_KEY = "file_type_driver";
    public static final String TRANSFORMER_CLASS = "de_transformer";

    @Override
    public DataElement doGet(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        DataCollection<?> primaryDC = gdc.getTypeSpecificCollection(this);
        Class<? extends DataElement> deClass = ClassLoading.loadSubClass( dei.getStrictProperty(DataElementInfo.ELEMENT_CLASS), dei.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY), DataElement.class );
        List<Class<? extends Transformer>> tClasses = TransformerRegistry.getTransformerClass(FileDataElement.class, deClass);
        DataElement de = primaryDC.get(dei.getName());
        if( !tClasses.isEmpty() )
        {
            Transformer transformer = null;
            String transformerName = dei.getProperty( TRANSFORMER_CLASS );
            if(transformerName != null)
            {
                for(Class<? extends Transformer> tClass: tClasses)
                {
                    if(tClass.getName().equals(transformerName))
                    {
                        transformer = tClass.newInstance();
                        break;
                    }
                }
            }
            if(transformer == null)
            {
                for(Class<? extends Transformer> tClass: tClasses)
                {
                    transformer = tClass.newInstance();
                    if(!(transformer instanceof PriorityTransformer))
                    {
                        break;
                    }
                }
            }
            transformer.init(primaryDC, gdc);
            DataElement result = transformer.transformInput(de);
            if(result instanceof DataCollection)
            {
                DataCollection<?> dc = (DataCollection<?>)result;
                Enumeration<?> propertyNames = dei.getProperties().propertyNames();
                while(propertyNames.hasMoreElements())
                {
                    String name = propertyNames.nextElement().toString();
                    if(name.equals(DataCollectionConfigConstants.NAME_PROPERTY) || name.equals(DataElementInfo.DRIVER_CLASS))
                        continue;
                    try
                    {
                        dc.getInfo().getProperties().setProperty(name, dei.getProperties().getProperty(name));
                    }
                    catch( Exception e )
                    {
                    }
                }
            }
            return result;
        }
        return de;
    }

    @Override
    public void doPut(GenericDataCollection gdc, DataElement de, DataElementInfo dei) throws Exception
    {
        DataCollection primaryDC = gdc.getTypeSpecificCollection(this);
        DataElement fileDE = de;
        List<Class<? extends Transformer>> tClasses = TransformerRegistry.getTransformerClass(FileDataElement.class, de.getClass());
        if( !tClasses.isEmpty() )
        {
            ExProperties.addPlugin(dei.getProperties(), de.getClass());
            Transformer bestTransformer = null;
            int maxPriority = 0;
            for(Class<? extends Transformer> tClass: tClasses)
            {
                Transformer transformer = tClass.newInstance();
                int priority = 1;
                if(transformer instanceof PriorityTransformer)
                {
                    priority = ((PriorityTransformer)transformer).getInputPriority(FileDataElement.class, de);
                }
                if(priority>maxPriority)
                {
                    bestTransformer = transformer;
                    maxPriority = priority;
                }
            }
            bestTransformer.init(primaryDC, gdc);
            dei.getProperties().put( TRANSFORMER_CLASS, bestTransformer.getClass().getName() );
            fileDE = bestTransformer.transformOutput(de);
            if(bestTransformer instanceof TransformerWithProperties)
            {
                Properties properties = ((TransformerWithProperties)bestTransformer).getProperties(de);
                if(properties != null)
                {
                    EntryStream.of(properties).selectKeys(String.class).selectValues(String.class)
                        .forKeyValue(dei.getProperties()::setProperty);
                }
            }
        }
        primaryDC.put(fileDE);
    }

    @Override
    public void doRemove(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        DataElement de = gdc.getFromCache(dei.getName());
        if( de instanceof DataCollection )
            ( (DataCollection<?>)de ).close();
        gdc.getTypeSpecificCollection(this).remove(dei.getName());
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> childClass)
    {
        if( FileDataElement.class.isAssignableFrom(childClass) )
        {
            return true;
        }
        if( !TransformerRegistry.getTransformerClass(FileDataElement.class, childClass).isEmpty() )
        {
            return true;
        }
        return false;
    }

    @Override
    public FileCollection createBaseCollection(GenericDataCollection gdc)
    {
        try
        {
            File folder = new File(gdc.getRootDirectory() + FOLDER_NAME);
            if( !folder.exists() )
            {
                folder.mkdirs();
            }
            File configFile = new File(gdc.getRootDirectory(), CONFIG_FILE_NAME);
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
            //primary.setProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, gdc.getRootDirectory() + FOLDER_NAME);
            primary.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, gdc.getRealParent());
            primary.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, folder.getPath());
            return (FileCollection)CollectionFactory.createCollection(gdc.getOrigin(), primary);
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
        result.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, GenericFileCollection.class.getName());
        result.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, gdc.getName());
        result.setProperty(FileCollection.FILE_FILTER, "*");
        return result;
    }

    public static class GenericFileCollection extends FileCollection
    {
        public GenericFileCollection(DataCollection<?> parent, Properties properties)
        {
            super(parent, properties);
        }

        @Override
        protected FileDataElement createElement(String name, File file)
        {
            return new FileDataElement(name, getPrimaryCollection(), file);
        }

        protected DataCollection getPrimaryCollection()
        {
            return (DataCollection<?>)getInfo().getProperties().get(DataCollectionConfigConstants.PRIMARY_COLLECTION);
        }

        @Override
        protected void doPut(FileDataElement dataElement, boolean isNew)
        {
            if(dataElement.getOrigin() != getPrimaryCollection())
            {
                try
                {
                    dataElement = (FileDataElement)dataElement.clone(getPrimaryCollection(), dataElement.getName());
                }
                catch( CloneNotSupportedException e )
                {
                    throw new InternalException( e );
                }
            }
            super.doPut(dataElement, isNew);
        }
    }

    @Override
    public boolean isLeafElement(GenericDataCollection gdc, DataElementInfo dei)
    {
        return true;
    }

    @Override
    public long estimateSize(GenericDataCollection gdc, DataElementInfo dei, boolean recalc)
    {
        try
        {
            DataCollection<?> primaryDC = gdc.getTypeSpecificCollection(this);
            return ((FileDataElement)primaryDC.get(dei.getName())).getContentLength();
        }
        catch( Exception e )
        {
            return -1;
        }
    }
}
