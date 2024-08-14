package biouml.model.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.exception.ExceptionRegistry;
import biouml.standard.type.BaseSupport;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSerializer;

public class BaseSupportWrapper
{
    private Map<BaseSupport, Map<String, String>> object2DpsProperty2Serialized = new HashMap<>();

    private BaseSupport wrapped;

    public BaseSupportWrapper(BaseSupport baseSupportArg)
    {
        explore(baseSupportArg);
    }

    public BaseSupportWrapper()
    {
    }

    private void explore(BaseSupport obj)
    {
        if( obj == null || object2DpsProperty2Serialized.containsKey(obj) )
        {
            return;
        }

        if( wrapped == null )
        {
            wrapped = obj;
        }

        Map<String, String> dpsProperty2Serialized = new HashMap<>();

        DynamicPropertySetSerializer dpsSerializer = new DynamicPropertySetSerializer();

        try
        {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
            for( PropertyDescriptor pd : props )
            {
                try (ByteArrayOutputStream bas = new ByteArrayOutputStream(); OutputStream os = new BufferedOutputStream( bas ))
                {
                    if( pd.getPropertyType().equals(DynamicPropertySet.class) && pd.getReadMethod() != null )
                    {
                        Method readMethod = pd.getReadMethod();
                        DynamicPropertySet dps = (DynamicPropertySet)readMethod.invoke(obj, new Object[0]);

                        dpsSerializer.save(os, dps);

                        String serialized = bas.toString();
                        dpsProperty2Serialized.put(pd.getName(), serialized);
                    }
                    else if( pd.getPropertyType().isArray() && pd.getReadMethod() != null && pd.getWriteMethod() != null )
                    {
                        if( !BaseSupport.class.isAssignableFrom(pd.getPropertyType().getComponentType()) )
                        {
                            continue;
                        }

                        Method readMethod = pd.getReadMethod();
                        Object arr = readMethod.invoke(obj, new Object[0]);
                        if( arr == null )
                            continue;
                        for( int i = 0; i < Array.getLength(arr); i++ )
                        {
                            explore((BaseSupport)Array.get(arr, i));
                        }
                    }
                    else if( BaseSupport.class.isAssignableFrom(pd.getPropertyType()) && pd.getReadMethod() != null
                            && pd.getWriteMethod() != null )
                    {
                        Method readMethod = pd.getReadMethod();
                        explore((BaseSupport)readMethod.invoke(obj, new Object[0]));
                    }
                }
                catch( IllegalArgumentException | IllegalAccessException | InvocationTargetException | IOException e )
                {
                    ExceptionRegistry.log(e);
                }
            }

            object2DpsProperty2Serialized.put(obj, dpsProperty2Serialized);
        }
        catch( IntrospectionException e )
        {
            ExceptionRegistry.log(e);
        }
    }

    public void restoreWrapped()
    {
        for( Map.Entry<BaseSupport, Map<String, String>> entry : this.object2DpsProperty2Serialized.entrySet() )
        {
            Map<String, String> dpsProperty2Serialized = entry.getValue();
            if( dpsProperty2Serialized.size() < 1 )
                continue;

            DynamicPropertySetSerializer dpsSerializer = new DynamicPropertySetSerializer();

            try
            {
                BeanInfo beanInfo = Introspector.getBeanInfo(entry.getKey().getClass());
                PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
                for( PropertyDescriptor pd : props )
                {
                    if( pd.getPropertyType().equals(DynamicPropertySet.class) && pd.getReadMethod() != null )
                    {
                        Method readMethod = pd.getReadMethod();
                        DynamicPropertySet dps = (DynamicPropertySet)readMethod.invoke(entry.getKey(), new Object[0]);

                        String serialized = dpsProperty2Serialized.get(pd.getName());
                        if( serialized.length() > 0 )
                        {
                            InputStream is = new ByteArrayInputStream(serialized.getBytes(StandardCharsets.UTF_8));
                            InputStream bis = new BufferedInputStream(is);
                            dpsSerializer.load(dps, bis);
                        }
                    }
                }
            }
            catch( IntrospectionException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e )
            {
                ExceptionRegistry.log(e);
            }
        }

        this.object2DpsProperty2Serialized.clear();
    }

    public Map<BaseSupport, Map<String, String>> getObject2DpsProperty2Serialized()
    {
        return object2DpsProperty2Serialized;
    }

    public void setObject2DpsProperty2Serialized(Map<BaseSupport, Map<String, String>> object2DpsProperty2Serialized)
    {
        this.object2DpsProperty2Serialized = object2DpsProperty2Serialized;
    }

    public BaseSupport getWrapped()
    {
        return wrapped;
    }

    public void setWrapped(BaseSupport wrapped)
    {
        this.wrapped = wrapped;
    }
}
