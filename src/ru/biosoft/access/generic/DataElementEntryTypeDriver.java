package ru.biosoft.access.generic;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.Entry;
import ru.biosoft.access.EntryCollection;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.util.ExProperties;

public class DataElementEntryTypeDriver implements DataElementTypeDriver
{
    protected static final Logger log = Logger.getLogger( DataElementEntryTypeDriver.class.getName() );

    public static final String TYPE_DRIVER_KEY = "entry_type_driver";
    public static final String PROPERTY_CONFIG_FILENAME = "entry_collection.config";
    public static final String PROPERTY_DATA_FILENAME = "entry_collection.dat";

    public static final String ENTRY_TRANSFORMER_ATTR = "transformer";

    @Override
    public DataElement doGet(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        DataCollection primaryDC = gdc.getTypeSpecificCollection(this);
        Class<? extends DataElement> deClass = ClassLoading.loadSubClass( dei.getStrictProperty(DataElementInfo.ELEMENT_CLASS), DataElement.class );
        Transformer transformer = getEntryTransformer(primaryDC, gdc, deClass);
        return transformer.transformInput(primaryDC.get(dei.getName()));
    }

    @Override
    public void doPut(GenericDataCollection gdc, DataElement de, DataElementInfo dei) throws Exception
    {
        DataCollection primaryDC = gdc.getTypeSpecificCollection(this);
        Transformer transformer = getEntryTransformer(primaryDC, gdc, de.getClass());
        primaryDC.put(transformer.transformOutput(de));
    }

    @Override
    public void doRemove(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        gdc.getTypeSpecificCollection(this).remove(dei.getName());
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> childClass)
    {
        if( !TransformerRegistry.getTransformerClass(Entry.class, childClass).isEmpty()
                /*|| ( MutableDataElementSupport.class.isAssignableFrom(childClass) && !ru.biosoft.access.core.DataCollection.class.isAssignableFrom(childClass) )*/ )
        {
            return true;
        }
        return false;
    }

    @Override
    public FileEntryCollection2 createBaseCollection(GenericDataCollection gdc)
    {
        FileEntryCollection2 result = null;
        //try to open collection
        try
        {
            File file = new File(gdc.getRootDirectory() + PROPERTY_CONFIG_FILENAME);
            if( file.exists() )
            {
                Properties primary = new ExProperties(file);
                result = (FileEntryCollection2)CollectionFactory.createCollection(gdc.getOrigin(), primary);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not load data collection", e);
        }
        if( result == null )
        {
            //create new entry collection
            try
            {
                Properties primary = createCollectionProperties(gdc);
                ExProperties.store(primary, new File(gdc.getRootDirectory(), PROPERTY_CONFIG_FILENAME));
                primary.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, gdc.getRealParent());
                primary.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, gdc.getRootDirectory());
                result = (FileEntryCollection2)CollectionFactory.createCollection(gdc.getOrigin(), primary);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not create data collection", e);
            }
        }
        return result;
    }

    protected Transformer getEntryTransformer(DataCollection primary, GenericDataCollection gdc, Class outType)
    {
        Transformer transformer = null;
        try
        {
            transformer = TransformerRegistry.getTransformer(primary, gdc, Entry.class, outType);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While obtaining transformer", e);
        }
        if(transformer != null) return transformer;
        final Class type = outType;
        transformer = new BeanInfoEntryTransformer()
        {
            @Override
            public Class getOutputType()
            {
                if( outputType == null )
                {
                    outputType = type;
                }
                return outputType;
            }
        };
        transformer.init(primary, gdc);
        return transformer;
    }

    protected Properties createCollectionProperties(GenericDataCollection gdc)
    {
        Properties result = new ExProperties();
        result.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName());
        result.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, PROPERTY_DATA_FILENAME);
        //result.setProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, gdc.getRootDirectory());
        //result.setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, gdc.getRootDirectory());
        result.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, gdc.getName());
        result.setProperty(EntryCollection.ENTRY_START_PROPERTY, "ID");
        result.setProperty(EntryCollection.ENTRY_ID_PROPERTY, "ID");
        result.setProperty(EntryCollection.ENTRY_END_PROPERTY, "//");
        result.setProperty(EntryCollection.ENTRY_DELIMITERS_PROPERTY, "\"; \"");
        return result;
    }

    public static class GenericFileEntryCollection2 extends FileEntryCollection2
    {
        private final GenericDataCollection primaryCollection;

        public GenericFileEntryCollection2(DataCollection parent, Properties properties) throws Exception
        {
            super(parent, properties);
            this.primaryCollection = (GenericDataCollection)properties.get(DataCollectionConfigConstants.PRIMARY_COLLECTION);
        }

        @Override
        protected Entry doCreateEntry(String name, String data)
        {
            return new Entry(primaryCollection, name, data);
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
            Entry entry = (Entry)primaryDC.get(dei.getName());
            return entry.isStoredAsStream()?entry.getSize():entry.getData().length();
        }
        catch( Exception e )
        {
            return -1;
        }
    }
}
