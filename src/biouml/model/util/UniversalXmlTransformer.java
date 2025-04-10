package biouml.model.util;

import java.beans.BeanInfo;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.Properties;

import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.Entry;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.MutableDataElementSupport;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.access.core.DataElementReadException;
import biouml.standard.type.BaseSupport;

import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * Converts {@link FileDataElement} to the {@link Object} and back
 *  This transformer is used for Embl file format converting
 * @see ru.biosoft.access.core.TransformedDataCollection
 * @author DevelopmentOnTheEdge
 * @version 1.0
 */

public class UniversalXmlTransformer extends AbstractTransformer<Entry, BaseSupport>
{
    protected static final Logger log = Logger.getLogger(UniversalXmlTransformer.class.getName());

    protected String errorMessage;

    //protected XMLSerializer xmlSerializer = new XMLSerializer();

    public UniversalXmlTransformer()
    {
    }

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    protected Class<? extends BaseSupport> outputType;
    @Override
    public Class<? extends BaseSupport> getOutputType()
    {
        if( outputType == null )
        {
            Properties properties = getTransformedCollection().getInfo().getProperties();
            String name = properties.getProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
            if( name != null )
            {
                try
                {
                    String plugins = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
                    outputType = ClassLoading.loadSubClass( name, plugins, BaseSupport.class );
                }
                catch( LoggedClassNotFoundException e )
                {
                    throw new DataElementReadException(e, getTransformedCollection(), DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
                }
            }
            else
            {
                throw new DataElementReadException(getTransformedCollection(), DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
            }
        }

        return outputType;
    }

    @Override
    public boolean isOutputType(Class<?> type)
    {
        return getOutputType().isAssignableFrom(type);
    }

    @Override
    public BaseSupport transformInput(Entry entry) throws Exception
    {
        BaseSupport de = null;
        ClassLoader oldClassLoader = null;
        try (InputStream is = new ByteArrayInputStream( entry.getData().getBytes( StandardCharsets.UTF_8 ) );
                XMLDecoder decoder = new XMLDecoder( is );)
        {
            oldClassLoader = Thread.currentThread().getContextClassLoader();
            String pluginNames = getTransformedCollection().getInfo().getProperties().getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
            Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader( getOutputType().getName(), pluginNames ));
            decoder.setExceptionListener(e -> {
                errorMessage += "\n" + e.getMessage();
                e.printStackTrace();
            });
            BaseSupportWrapper bsw = (BaseSupportWrapper)decoder.readObject();
            bsw.restoreWrapped();
            de = bsw.getWrapped();
            de.setOrigin(getTransformedCollection());
        }
        finally
        {
            if( oldClassLoader != null )
                Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

        return de;
    }

    private static class PersistenceDelegate extends DefaultPersistenceDelegate
    {
        @Override
        protected Expression instantiate(Object oldInstance, Encoder out)
        {
            if( oldInstance instanceof MutableDataElementSupport )
            {
                String name = "";
                try
                {
                    name = ( (MutableDataElementSupport)oldInstance ).getName();
                }
                catch( Throwable t )
                {
                    t.printStackTrace();
                }

                return new Expression(oldInstance, oldInstance.getClass(), "new", new Object[] {null, name});
            }
            return super.instantiate(oldInstance, out);
        }
    }

    @Override
    public Entry transformOutput(BaseSupport de) throws Exception
    {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        ClassLoader oldClassLoader = null;
        try (OutputStream os = new BufferedOutputStream( bas ); XMLEncoder xmlEncoder = new XMLEncoder( os );)
        {
            oldClassLoader = Thread.currentThread().getContextClassLoader();
            String pluginNames = getTransformedCollection().getInfo().getProperties().getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
            Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader( getOutputType().getName(), pluginNames ));
            xmlEncoder.setOwner(getTransformedCollection());
            xmlEncoder.setPersistenceDelegate(de.getClass(), new PersistenceDelegate());

            BeanInfo beanInfo = Introspector.getBeanInfo(de.getClass());
            PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
            for( PropertyDescriptor pd : props )
            {
                Class<?> propertyType = pd.getPropertyType();
                if( pd.getReadMethod() != null && pd.getWriteMethod() != null )
                {
                    if( propertyType.isArray() )
                    {
                        propertyType = propertyType.getComponentType();
                    }

                    if( !propertyType.equals(DynamicPropertySet.class) && !propertyType.equals(String.class) && !propertyType.isPrimitive()
                            && xmlEncoder.getPersistenceDelegate(propertyType).getClass().equals(DefaultPersistenceDelegate.class) )
                    {
                        xmlEncoder.setPersistenceDelegate(propertyType, new PersistenceDelegate());
                    }
                }
            }

            errorMessage = "";
            xmlEncoder.setExceptionListener(e -> {
                errorMessage += "\n" + e.getMessage();
                e.printStackTrace();
            });

            BaseSupportWrapper bsw = new BaseSupportWrapper(de);
            xmlEncoder.writeObject(bsw);
            if( errorMessage.length() > 0 )
            {
                log.log(Level.SEVERE, errorMessage);
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "transform to XML error", t);
        }
        finally
        {
            if( oldClassLoader != null )
                Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

        return new Entry( getPrimaryCollection(), de.getName(), bas.toString( "UTF-8" ) );
    }
}
