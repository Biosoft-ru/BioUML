package biouml.model.xml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Base;

/**
 * Wrapper for DiagramElement properties
 * 
 * @author tolstyh
 * 
 */
public class DiagramElementJScriptWrapper
{
    protected DiagramElement diagramElement;

    public DiagramElementJScriptWrapper(DiagramElement diagramElement)
    {
        this.diagramElement = diagramElement;
    }

    public DiagramElement getDiagramElement()
    {
        return diagramElement;
    }

    public void setDiagramElement(DiagramElement diagramElement)
    {
        this.diagramElement = diagramElement;
    }
    
    public boolean isNode()
    {
        return (diagramElement instanceof Node);
    }
    
    public boolean isEdge()
    {
        return (diagramElement instanceof Edge);
    }

    public Object getValue(String name, Object defaultValue)
    {
        Object obj = getCoreValue(name, defaultValue);
        if(obj instanceof DiagramElement)
        {
            return new DiagramElementJScriptWrapper((DiagramElement)obj);
        }
        return obj;
    }
    
    private static Map<Class<?>, Map<String, Method>> methods = new ConcurrentHashMap<>();
    
    private static Object fetchValue(Object bean, String property)
    {
        Method m = methods.computeIfAbsent( bean.getClass(), clazz ->
                StreamEx.of(clazz.getMethods())
                    .filter( method -> method.getParameterTypes().length == 0 )
                    .mapToEntry( Method::getName, Function.identity() )
                    .filterKeys( name -> name.startsWith( "get" ) )
                    .mapKeys( name -> name.substring( "get".length() ).toLowerCase() )
                    .toMap() ).get( property.toLowerCase() );
        if(m == null)
            return null;
        try
        {
            return m.invoke( bean );
        }
        catch( IllegalAccessException | IllegalArgumentException | InvocationTargetException e )
        {
            return null;
        }
    }
    
    private Object getCoreValue(String name, Object defaultValue)
    {
        Object result = fetchValue( diagramElement, name );
        if(result != null)
            return result;
        if( diagramElement.getAttributes() != null )
        {
            result = diagramElement.getAttributes().getValue( name );
            if(result != null)
                return result;
        }
        Base kernel = diagramElement.getKernel();
        if( kernel != null )
        {
            result = fetchValue(kernel, name);
            if(result != null)
                return result;
            if( kernel.getAttributes() != null )
            {
                result = kernel.getAttributes().getValue( name );
                if(result != null)
                    return result;
            }
        }
        return defaultValue;
    }

    public void setValue(String name, Object value)
    {
        Method methods[] = diagramElement.getClass().getMethods();
        for( Method method : methods )
        {
            if( method.getName().toLowerCase().equals( ( "set" + name ).toLowerCase()) )
            {
                try
                {
                    method.invoke(diagramElement, new Object[] {value});
                    return;
                }
                catch( Exception e )
                {
                }
            }
        }
        if( diagramElement.getAttributes() != null )
        {
            DynamicProperty dp = diagramElement.getAttributes().getProperty(name);
            if( dp != null )
            {
                dp.setValue(value);
                return;
            }
        }
        Base kernel = diagramElement.getKernel();
        if( kernel != null )
        {
            Method kernelMethods[] = kernel.getClass().getMethods();
            for( Method kernelMethod : kernelMethods )
            {
                if( kernelMethod.getName().toLowerCase().equals( ( "set" + name ).toLowerCase()) )
                {
                    try
                    {
                        kernelMethod.invoke(kernel, new Object[] {value});
                        return;
                    }
                    catch( Exception e )
                    {
                    }
                }
            }
            if( kernel.getAttributes() != null )
            {
                DynamicProperty kernelDp = kernel.getAttributes().getProperty(name);
                if( kernelDp != null && kernelDp.getValue() != null )
                {
                    kernelDp.setValue(value);
                    return;
                }
            }
        }
    }

    public boolean hasValue(String name)
    {
        Method methods[] = diagramElement.getClass().getMethods();
        for( Method method : methods )
        {
            if( method.getName().toLowerCase().equals( ( "get" + name ).toLowerCase()) )
            {
                return true;
            }
        }
        if( diagramElement.getAttributes() != null )
        {
            DynamicProperty dp = diagramElement.getAttributes().getProperty(name);
            if( dp != null )
            {
                return true;
            }
        }
        Base kernel = diagramElement.getKernel();
        if( kernel != null )
        {
            Method kernelMethods[] = kernel.getClass().getMethods();
            for( Method kernelMethod : kernelMethods )
            {
                if( kernelMethod.getName().toLowerCase().equals( ( "get" + name ).toLowerCase()) )
                {
                    return true;
                }
            }
            if( kernel.getAttributes() != null )
            {
                DynamicProperty kernelDp = kernel.getAttributes().getProperty(name);
                if( kernelDp != null )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void addAttribute(String name, Class<?> type, Object value)
    {
        try
        {
            diagramElement.getAttributes().add(new DynamicProperty(name, type, value));
        }
        catch( Throwable t )
        {
        }
    }
}
