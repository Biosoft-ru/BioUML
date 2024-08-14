package ru.biosoft.util;

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.Statement;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

import ru.biosoft.access.ClassLoading;

public class BeanXMLUtils
{
    public static String toXML(Object bean) throws Exception
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        toXML( bean, os );
        return os.toString();
    }

    public static void toXML(Object bean, OutputStream os) throws Exception
    {
        Thread curThread = Thread.currentThread();
        ClassLoader oldClassLoader = curThread.getContextClassLoader();
        Thread.currentThread().setContextClassLoader( ClassLoading.getClassLoader() );
        
        try(XMLEncoder encoder = new XMLEncoder( os ))
        {
            encoder.setPersistenceDelegate( DynamicPropertySetAsMap.class, new DPSPersistenceDelegate() );
            encoder.setPersistenceDelegate( DynamicPropertySetSupport.class, new DPSPersistenceDelegate() );
            encoder.setPersistenceDelegate( DynamicProperty.class, new DynamicPropertyPersistenceDelegate() );
            encoder.setExceptionListener( e -> {
                throw new AsRuntime( e );
            } );
            encoder.writeObject( bean );
        }
        catch(AsRuntime e)
        {
            throw e.getCause();
        }
        finally
        {
            curThread.setContextClassLoader( oldClassLoader );
        }
    }
    
    public static Object fromXML(String str) throws Exception
    {
        return fromXML( new ByteArrayInputStream( str.getBytes() ) );
    }
    
    public static Object fromXML(InputStream is) throws Exception
    {
        try (XMLDecoder decoder = new XMLDecoder( is, null, e -> { throw new AsRuntime( e ); }, ClassLoading.getClassLoader() ))
        {
            Object bean = decoder.readObject();
            return bean;
        }
        catch( AsRuntime e )
        {
            throw e.getCause();
        }
    }
    
    
    private static class AsRuntime extends RuntimeException
    {
        public AsRuntime(Exception e)
        {
            super( e );
        }
        
        @Override
        public Exception getCause()
        {
            return (Exception)super.getCause();
        }
    }
    
    public static class DynamicPropertyPersistenceDelegate extends DefaultPersistenceDelegate
    {
        @Override
        protected Expression instantiate(Object oldInstance, Encoder out)
        {
            DynamicProperty dp = (DynamicProperty)oldInstance;
            return new Expression(oldInstance, oldInstance.getClass(), "new", new Object[] {dp.getName(), dp.getType(), dp.getValue()});
        }
    }
    
    public static class DPSPersistenceDelegate extends DefaultPersistenceDelegate
    {
        @Override
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out)
        {
            try
            {
                DynamicPropertySet oldDPS = (DynamicPropertySet)oldInstance;
                DynamicPropertySet newDPS = (DynamicPropertySet)newInstance;
                // Remove the new elements.
                if( newDPS != null )
                {
                    for( DynamicProperty newDP : newDPS )
                    {
                        if( oldDPS.getProperty( newDP.getName() ) == null )
                        {
                            invokeStatement( oldInstance, "remove", new Object[] {newDP.getName()}, out );
                        }
                    }
                    // Add the new elements.
                    for( DynamicProperty oldDP : oldDPS )
                    {
                        Expression oldGetExp = new Expression( oldInstance, "getProperty", new Object[] {oldDP.getName()} );
                        DynamicProperty newDP = newDPS.getProperty( oldDP.getName() );
                        if( newDP == null || !oldDP.getType().equals( newDP.getType() )
                                || !Objects.equals( oldDP.getValue(), newDP.getValue() ) )
                        {
                            invokeStatement( oldInstance, "add", new Object[] {oldGetExp.getValue()}, out );
                        }
                    }
                }
            }
            catch( Exception e )
            {
                out.getExceptionListener().exceptionThrown( e );
            }

        }
        static void invokeStatement(Object instance, String methodName, Object[] args, Encoder out)
        {
            out.writeStatement( new Statement( instance, methodName, args ) );
        }
    }
}
