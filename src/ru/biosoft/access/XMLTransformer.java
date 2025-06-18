package ru.biosoft.access;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.AbstractFileTransformer;


/**
 * Converts {@link FileDataElement} to the {@link Object} and back
 *  This transformer is used for Embl file format converting
 * @see ru.biosoft.access.core.TransformedDataCollection
 * @author DevelopmentOnTheEdge
 * @version 1.0
 */

public class XMLTransformer extends AbstractFileTransformer<DataElement>
{
    private static class XMLExceptionListener implements ExceptionListener
    {
        private final List<String> exceptions = new ArrayList<>();
        
        @Override
        public void exceptionThrown(Exception e)
        {
            exceptions.add(e.getMessage());
        }
        
        public void validate() throws Exception
        {
            if(!exceptions.isEmpty())
                throw new Exception(String.join("; ", exceptions));
        }
    }
    
    @Override
    public Class<DataElement> getOutputType()
    {
        return ru.biosoft.access.core.DataElement.class;
    }

    @Override
    public boolean isOutputType(Class type)
    {
        return getOutputType().isAssignableFrom(type);
    }

    public static Object getClone(Object bean) throws Exception
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (XMLEncoder d = new XMLEncoder( os ))
        {
            d.writeObject( bean );
        }
        try (ByteArrayInputStream is = new ByteArrayInputStream( os.toByteArray() ); XMLDecoder decoder = new XMLDecoder( is ))
        {
            bean = decoder.readObject();
        }

        return bean;
    }

    @Override
    public DataElement load(File input, String name, DataCollection<DataElement> origin) throws Exception
    {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = new BufferedInputStream( new FileInputStream( input ) ); XMLDecoder decoder = new XMLDecoder( is ))
        {
            XMLExceptionListener exceptionListener = new XMLExceptionListener();
            decoder.setExceptionListener(exceptionListener);
            Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader());
            Object o = decoder.readObject();
            exceptionListener.validate();
            return (DataElement)o;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public void save(File output, DataElement element) throws Exception
    {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try (OutputStream os = new BufferedOutputStream( new FileOutputStream( output ) ); XMLEncoder encoder = new XMLEncoder( os ))
        {
            XMLExceptionListener exceptionListener = new XMLExceptionListener();
            encoder.setExceptionListener(exceptionListener);

            Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader());
            encoder.writeObject(element);
            exceptionListener.validate();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
