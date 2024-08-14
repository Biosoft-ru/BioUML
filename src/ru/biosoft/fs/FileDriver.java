package ru.biosoft.fs;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.access.generic.TransformerRegistry;
import ru.biosoft.access.generic.TransformerRegistry.TransformerInfo;
import ru.biosoft.access.generic.TransformerWithProperties;
import ru.biosoft.util.Clazz;

public class FileDriver implements FileSystemElementDriver
{
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
    public boolean isLeaf(ElementInfo info)
    {
        return true;
    }

    @SuppressWarnings ( {"rawtypes", "unchecked"} )
    @Override
    public DataElement create(FileSystemCollection parent, ElementInfo info, File file) throws Exception
    {
        List<Class<? extends Transformer>> tClasses = TransformerRegistry.getTransformerClass(FileDataElement.class, info.getClazz());
        FileDataElement de = new FileDataElement( info.getName(), parent, file );
        Transformer transformer = null;
        String transformerName = info.getProperty(DataCollectionConfigConstants.TRANSFORMER_CLASS);
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
            transformer = StreamEx.of(tClasses).map( Clazz.of( Transformer.class )::create )
                    .max( Comparator.comparingInt( Clazz.of( PriorityTransformer.class ).toInt( t -> t.getOutputPriority( de.getName() ), 1 ) ) )
                    .orElse( null );
        }
        if(transformer != null)
        {
            transformer.init(new StubFileCollection( file ), parent);
            DataElement result = transformer.transformInput(de);
            if(result instanceof DataCollection)
            {
                info.initCollectionProperties( (DataCollection<?>)result );
            }
            return result;
        }
        return de;
    }

    @SuppressWarnings ( {"rawtypes", "unchecked"} )
    @Override
    public ElementInfo save(DataElement de, File data) throws Exception
    {
        Map<String, String> properties = new HashMap<>();
        List<Class<? extends Transformer>> tClasses = TransformerRegistry.getTransformerClass(FileDataElement.class, de.getClass());
        if(tClasses.isEmpty())
            return null;
        Transformer bestTransformer = StreamEx.of(tClasses).map( Clazz.of( Transformer.class )::create )
                .max( Comparator.comparingInt( Clazz.of( PriorityTransformer.class ).toInt( t -> t.getInputPriority(FileDataElement.class, de), 1 ) ) )
                .orElseThrow( InternalException::new );
        bestTransformer.init(new StubFileCollection( data ), de.getOrigin());
        properties.put(DataCollectionConfigConstants.TRANSFORMER_CLASS, bestTransformer.getClass().getName());
        bestTransformer.transformOutput(de);
        if(bestTransformer instanceof TransformerWithProperties)
        {
            Properties transformerProperties = ((TransformerWithProperties)bestTransformer).getProperties(de);
            if(transformerProperties != null)
            {
                EntryStream.of( transformerProperties ).selectKeys( String.class ).selectValues( String.class )
                    .forKeyValue( properties::put );
            }
        }
        return new ElementInfo( de, properties );
    }

    private static class StubFileCollection extends AbstractDataCollection<FileDataElement> implements FileBasedCollection<FileDataElement>
    {
        private final File file;

        public StubFileCollection(File file)
        {
            super( "", null, null );
            this.file = file;
        }

        @Override
        @Nonnull
        public List<String> getNameList()
        {
            return Collections.emptyList();
        }

        @Override
        public boolean isFileAccepted(File file)
        {
            return true;
        }

        @Override
        public File getChildFile(String name)
        {
            return file;
        }

    }

    @Override
    public StreamEx<String> getAvailableTypes(ElementInfo info)
    {
        return TransformerRegistry.getSupportedTransformers( FileDataElement.class );
    }

    @Override
    public ElementInfo updateInfoForType(ElementInfo elementInfo, String type)
    {
        Map<String, String> properties = elementInfo.getProperties();
        TransformerInfo transformerInfo = TransformerRegistry.getTransformerInfo( type );
        if(transformerInfo == null || transformerInfo.getInputClass() != FileDataElement.class)
            throw new ParameterNotAcceptableException( "type", type );
        properties.put(DataCollectionConfigConstants.TRANSFORMER_CLASS, transformerInfo.getTransformerClass().getName());
        return new ElementInfo( elementInfo.getName(), transformerInfo.getOutputClass(), properties );
    }

    @Override
    public String getCurrentType(ElementInfo info)
    {
        return detectTransformer( info ).getName();
    }

    private TransformerInfo detectTransformer(ElementInfo info)
    {
        String transformerClassName = info.getProperty( DataCollectionConfigConstants.TRANSFORMER_CLASS );
        @SuppressWarnings ( "rawtypes" )
        Class<? extends Transformer> transformerClass = null;
        TransformerInfo transformerInfo = null;
        if(transformerClassName != null)
        {
            try
            {
                transformerClass = ClassLoading.loadSubClass( transformerClassName, Transformer.class );
            }
            catch( LoggedClassNotFoundException | LoggedClassCastException e )
            {
                // Ignore
            }
            if(transformerClass != null)
            {
                transformerInfo = TransformerRegistry.getTransformerInfo( transformerClass );
            }
        }
        if(transformerInfo == null)
            transformerInfo = TransformerRegistry.detectFileTransformer( info.getName() );
        return transformerInfo;
    }

    @Override
    public Class<? extends DataElement> detectClass(ElementInfo elementInfo)
    {
        return detectTransformer( elementInfo ).getOutputClass();
    }
}
