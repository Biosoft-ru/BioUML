package ru.biosoft.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSerializer;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;

/**
 * DPS utils independent from analysis package. Function moved from AnalysisDPSUtils
 */
public class DPSUtils
{
    protected static final Logger log = Logger.getLogger(DPSUtils.class.getName());

    public static final String PARAMETER_ANALYSIS_PARAMETER = "parameter";

    public static void writeBeanToDPS(Object bean, DynamicPropertySet attributes, String prefix)
    {
        if(bean == null)
            return;
        BeanUtil.properties( bean )
                .map( property -> new DynamicProperty( prefix + property.getName(), property.getValueClass(), property.getValue() ) )
                .forEach( attributes::add );
    }

    public static void readBeanFromDPS(Object bean, DynamicPropertySet attributes, String prefix)
    {
        ComponentModel model = ComponentFactory.getModel( bean, Policy.DEFAULT, true );
        for(int i=0; i<model.getPropertyCount(); i++)
        {
            try
            {
                Property property = model.getPropertyAt(i);
                DynamicProperty dynProperty = attributes.getProperty(prefix+property.getName());

                if( dynProperty != null )
                {
                    if( DynamicPropertySet.class.isAssignableFrom(dynProperty.getType() ))
                    {
                        readBeanFromDPS(property.getValue(), (DynamicPropertySet)dynProperty.getValue(), prefix);
                    }
                    else
                    {
                        property.setValue(dynProperty.getValue());
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "While saving properties: ", e);
            }
        }
    }

    /**
     * DynamicProperty PropertyDescriptor flag indicating that property will be marked as transient (not serializable)
     */
    private static final String TRANSIENT_PROPERTY = "transientProperty";

    public static boolean isTransient(DynamicProperty dp)
    {
        return dp.getBooleanAttribute(TRANSIENT_PROPERTY);
    }

    public static void makeTransient(DynamicProperty dp)
    {
        dp.getDescriptor().setValue(TRANSIENT_PROPERTY, true);
    }

    public static void saveDPSArray(DynamicPropertySet[] dpsArray, OutputStream out) throws IOException
    {
        ObjectOutputStream oos = new ObjectOutputStream(out);
        DynamicPropertySetSerializer serializer = new DynamicPropertySetSerializer();

        for( DynamicPropertySet dps: dpsArray )
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            serializer.save(os, dps);
            oos.writeObject(os.toString("UTF-8"));
        }
        oos.flush();
    }

    public static DynamicPropertySet[] loadDPSArray(InputStream in)
    {
        List<DynamicPropertySet> list = new ArrayList<>();
        try
        {
            ObjectInputStream ois = new ObjectInputStream(in);
            DynamicPropertySetSerializer serializer = new DynamicPropertySetSerializer();

            while( true )
            {
                Object p = ois.readObject();
                DynamicPropertySet dps = new DynamicPropertySetSupport();
                serializer.load( dps, new ByteArrayInputStream( p.toString().getBytes( StandardCharsets.UTF_8 ) ),
                        ClassLoading.getClassLoader() );
                list.add(dps);
            }
        }
        catch( EOFException e )
        {
            //nothing to do, just stop cycle
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not load dynamic property set", t);
        }
        return list.toArray(new DynamicPropertySet[list.size()]);
    }

    public static DynamicProperty createTransient(String name, Class<?> clazz, Object value)
    {
        DynamicProperty dp = new DynamicProperty(name, clazz, value);
        makeTransient(dp);
        return dp;
    }

    public static DynamicProperty createReadOnly(String name, Class<?> clazz, Object value)
    {
        DynamicProperty dp = new DynamicProperty(name, clazz, value);
        dp.setReadOnly(true);
        return dp;
    }

    public static DynamicProperty createHiddenReadOnly(String name, Class<?> clazz, Object value)
    {
        DynamicProperty dp = createReadOnly(name, clazz, value);
        dp.setHidden(true);
        return dp;
    }

    public static DynamicProperty createHiddenReadOnlyTransient(String name, Class<?> clazz, Object value)
    {
        DynamicProperty dp = createHiddenReadOnly(name, clazz, value);
        makeTransient(dp);
        return dp;
    }
}
