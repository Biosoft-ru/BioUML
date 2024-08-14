package ru.biosoft.access.generic2;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.generic.TransformerRegistry;
import ru.biosoft.access.support.BeanInfoEntryTransformer;

public class GenericEntryTypeDriver extends GenericElementTypeDriver
{
    public static final String TYPE_DRIVER_KEY = "entry_type_driver";
    public static final String PROPERTY_CONFIG_FILENAME = "entry_collection.config";
    public static final String PROPERTY_DATA_FILENAME = "entry_collection.dat";

    public static final String ENTRY_TRANSFORMER_ATTR = "transformer";

    @Override
    public DataElement doGet(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException
    {
        try
        {
            Class<? extends DataElement> deClass = getElementClass( properties );
            Transformer<Entry, ? extends DataElement> transformer = getEntryTransformer(gdc, deClass);
            Entry entry = new Entry( gdc, properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY ), ApplicationUtils.readAsString( new File(
                    folder, DATA_FILE_NAME ) ) );
            return transformer.transformInput(entry);
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    public void doPut(GenericDataCollection2 gdc, File folder, DataElement de, Properties properties) throws LoggedException
    {
        try
        {
            Transformer transformer = getEntryTransformer(gdc, de.getClass());
            Entry entry = (Entry)transformer.transformOutput( de );
            if(entry.getData() == null)
                throw new UnsupportedOperationException( "Writing from streamed entry is not supported" );
            ApplicationUtils.writeString( new File(folder, DATA_FILE_NAME), entry.getData() );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> childClass)
    {
        return !TransformerRegistry.getTransformerClass( Entry.class, childClass ).isEmpty();
    }

    protected Transformer<Entry, ?> getEntryTransformer(GenericDataCollection2 gdc, final Class<? extends DataElement> outType) throws Exception
    {
        Transformer transformer = TransformerRegistry.getTransformer(gdc, gdc, Entry.class, outType);
        if(transformer != null) return transformer;
        transformer = new BeanInfoEntryTransformer()
        {
            @Override
            public Class getOutputType()
            {
                if( outputType == null )
                {
                    outputType = outType;
                }
                return outputType;
            }
        };
        transformer.init(gdc, gdc);
        return transformer;
    }

    @Override
    public boolean isLeaf(GenericDataCollection2 gdc, Properties properties)
    {
        return true;
    }
}
