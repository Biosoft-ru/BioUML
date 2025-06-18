package ru.biosoft.access.generic2;

import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import one.util.streamex.EntryStream;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.core.PriorityTransformer;
import ru.biosoft.access.generic.TransformerRegistry;
import ru.biosoft.access.generic.TransformerWithProperties;

public class GenericFileTypeDriver extends GenericElementTypeDriver
{
    public static final String TYPE_DRIVER_KEY = "file_type_driver";
    public static final String TRANSFORMER_CLASS = "de_transformer";

    @Override
    public DataElement doGet(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException
    {
        try
        {
            Class<? extends DataElement> deClass = ClassLoading
                    .loadSubClass( properties.getProperty( DataCollectionConfigConstants.CLASS_PROPERTY ), DataElement.class );
            List<Class<? extends Transformer>> tClasses = TransformerRegistry.getTransformerClass(FileDataElement.class, deClass);
            File dataFile = new File(folder, DATA_FILE_NAME);
            FileDataElement de = new FileDataElement( properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY ), gdc, dataFile );
            if( !tClasses.isEmpty() )
            {
                Transformer transformer = null;
                String transformerName = properties.getProperty( TRANSFORMER_CLASS );
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
                transformer.init(gdc, gdc);
                DataElement result = transformer.transformInput( de );
                if( result instanceof DataCollection )
                {
                    DataCollection<?> dc = (DataCollection<?>)result;
                    Enumeration<?> propertyNames = properties.propertyNames();
                    while(propertyNames.hasMoreElements())
                    {
                        String name = propertyNames.nextElement().toString();
                        if(name.equals(DataCollectionConfigConstants.NAME_PROPERTY) || name.equals(DataCollectionConfigConstants.CLASS_PROPERTY))
                            continue;
                        try
                        {
                            dc.getInfo().getProperties().setProperty(name, properties.getProperty(name));
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
            List<Class<? extends Transformer>> tClasses = TransformerRegistry.getTransformerClass(FileDataElement.class, de.getClass());
            if( !tClasses.isEmpty() )
            {
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
                bestTransformer.init(gdc, gdc);
                properties.put( TRANSFORMER_CLASS, bestTransformer.getClass().getName() );
                bestTransformer.transformOutput(de);
                if(bestTransformer instanceof TransformerWithProperties)
                {
                    Properties transformerProperties = ((TransformerWithProperties)bestTransformer).getProperties(de);
                    if(transformerProperties != null)
                    {
                        EntryStream.of(transformerProperties).selectKeys(String.class).selectValues(String.class)
                                .forKeyValue(properties::setProperty);
                    }
                }
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> clazz)
    {
        if( FileDataElement.class.isAssignableFrom( clazz ) )
        {
            return true;
        }
        if( !TransformerRegistry.getTransformerClass(FileDataElement.class, clazz).isEmpty() )
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean isLeaf(GenericDataCollection2 gdc, Properties properties)
    {
        return true;
    }
}
