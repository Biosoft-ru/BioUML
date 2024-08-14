package ru.biosoft.templates;

import java.lang.reflect.Method;
import java.util.Collection;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.util.BeanUtil;

/**
 * Filter for {@link TemplateInfo} by Object
 */
public class TemplateFilter
{
    /** The template is not suitable. */
    public static final int NOT_SUITABLE = 0;

    /** Object satisfies to template class (as subclass). */
    public static final int SUBCLASS = 1;

    /** Object satisfies to template class (exactly). */
    public static final int CLASS = 2;

    /** Object satisfies to template class and property filters. */
    public static final int PROPERTY = 4;

    // properties to restrict template applicability
    protected Class<?> clazz;
    protected boolean subclasses;
    protected PropertyFilter[] properties;
    protected String completeMethodName;

    public TemplateFilter(String className, Boolean subclasses, Collection<PropertyFilter> properties)
    {
        this.clazz = ClassLoading.loadClass( className );
        this.subclasses = subclasses;
        this.properties = properties.toArray( new PropertyFilter[properties.size()] );
    }

    public TemplateFilter(String className, Boolean subclasses, Collection<PropertyFilter> properties, String methodName)
    {
        this( className, subclasses, properties );
        this.completeMethodName = methodName;
    }

    /** Returns suitability status. */
    public int isSuitable(Object obj)
    {
        if( obj != null )
        {
            Class<?> objClass = obj.getClass();
            if( subclasses )
            {
                if( clazz.isAssignableFrom(objClass) )
                {
                    if( checkProperties( obj ) && checkMethod( obj ) )
                    {
                        return SUBCLASS;
                    }
                }
            }
            else
            {
                if( clazz == objClass )
                {
                    if( checkProperties( obj ) && checkMethod( obj ) )
                    {
                        return CLASS;
                    }
                }
            }
        }
        return NOT_SUITABLE;
    }

    protected boolean checkProperties(Object bean)
    {
        boolean result = true;
        if( properties != null )
        {
            for( PropertyFilter pFilter : properties )
            {
                try
                {
                    Object value = BeanUtil.getBeanPropertyValue(bean, pFilter.getPropertyName());
                    if( pFilter.getValue() != null )
                    {
                        if( value == null )
                        {
                            result = false;
                            break;
                        }
                        if( value instanceof String )
                        {
                            if( ! ( (String)value ).equals(pFilter.getValue()) )
                            {
                                result = false;
                                break;
                            }
                        }
                    }
                    if( pFilter.getClassName() != null )
                    {
                        if(value == null)
                        {
                            result = false;
                            break;
                        }
                        Class<?> targetClass = ClassLoading.loadClass( pFilter.getClassName() );
                        if( !targetClass.isInstance(value) )
                        {
                            result = false;
                            break;
                        }
                    }
                }
                catch( Exception e )
                {
                }
            }
        }
        return result;
    }

    protected boolean checkMethod(Object bean)
    {
        boolean result = true;
        if( completeMethodName != null )
        {
            int pos = completeMethodName.lastIndexOf( '.' );
            if( pos != -1 )
            {
                String className = completeMethodName.substring( 0, pos );
                String methodName = completeMethodName.substring( pos + 1 );
                try
                {
                    Class<?> c = ClassLoading.loadClass( className );
                    Method method = c.getMethod( methodName, bean.getClass() );
                    Object res = method.invoke( null, bean );
                    if( res instanceof Boolean )
                        return (boolean)res;
                    else
                        return false;
                }
                catch( Exception e )
                {
                    return false;
                }
            }
        }
        return result;
    }
}
