package ru.biosoft.util.serialization;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import one.util.streamex.StreamEx;
import ru.biosoft.util.serialization.xml.Constants;

public class Utils
{
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final Map<Class<?>, String> primitiveWrapperXmlElements = new HashMap<>();
    
    static
    {
        primitiveWrapperXmlElements.put(Integer.class, Constants.INTEGER_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Float.class, Constants.FLOAT_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Double.class, Constants.DOUBLE_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Long.class, Constants.LONG_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Short.class, Constants.SHORT_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Boolean.class, Constants.BOOLEAN_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Character.class, Constants.CHARACTER_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Byte.class, Constants.BYTE_WRAPPER_PRIMITIVE_ELEMENT);
    }
    
    private Utils()
    {
    }
    
    // To avoid BE dependencies
    public static boolean isEmpty(Object o)
    {
        return o == null || "".equals(o);
    }
    
    public static void setFieldValue(Object o, String fieldName, String fieldValue)
    {
        Method setter = getSetter(o.getClass(), fieldName);
        if( setter != null )
        {
            Class<?> paramType = setter.getParameterTypes()[0];
            try
            {
                if( paramType.equals(String.class) )
                {
                    setter.invoke(o, fieldValue);
                }
                else if( fieldValue == null )
                {
                    setter.invoke(o, (Object)null);
                }
                else if( paramType.equals(int.class) || paramType.equals(Integer.class) )
                {
                    setter.invoke(o, Integer.valueOf(fieldValue));
                }
                else if( paramType.equals(double.class) || paramType.equals(Double.class) )
                {
                    setter.invoke(o, Double.valueOf(fieldValue));
                }
                else if( paramType.equals(boolean.class) || paramType.equals(Boolean.class) )
                {
                    setter.invoke(o, Boolean.valueOf(fieldValue));
                }
                else if( paramType.equals(Date.class) )
                {
                    setDateValue(o, setter, fieldValue);
                }
                else if( paramType.equals(java.sql.Time.class) )
                {
                    setTimeValue(o, setter, fieldValue);
                }
                else if( paramType.equals(long.class) || paramType.equals(Long.class) )
                {
                    setter.invoke(o, Long.valueOf(fieldValue));
                }
                else if( paramType.equals(short.class) || paramType.equals(Short.class) )
                {
                    setter.invoke(o, Short.valueOf(fieldValue));
                }
                else if( paramType.equals(float.class) || paramType.equals(Float.class) )
                {
                    setter.invoke(o, Float.valueOf(fieldValue));
                }
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        }
    }
    
    public static void setFieldValue(Object o, String fieldName, Object value)
    {
        Method setter = getSetter(o.getClass(), fieldName);
        if( setter != null )
        {
            try
            {
                setter.invoke(o, value);
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        }
    }
    
    public static void setArrayValue(Object array, Class<?> clazz, Collection<?> c)
    {
        if( clazz.isPrimitive() )
        {
            int n = 0;
            if( int.class.equals(clazz) )
            {
                for( Object aC : c )
                {
                    Array.setInt(array, n++, Integer.parseInt((String)aC));
                }
            }
            else if( long.class.equals(clazz) )
            {
                for( Object aC : c )
                {
                    Array.setLong(array, n++, Long.parseLong((String)aC));
                }
            }
            else if( double.class.equals(clazz) )
            {
                for( Object aC : c )
                {
                    Array.setDouble(array, n++, Double.parseDouble((String)aC));
                }
            }
            else if( float.class.equals(clazz) )
            {
                for( Object aC : c )
                {
                    Array.setFloat(array, n++, Float.parseFloat((String)aC));
                }
            }
            else if( boolean.class.equals(clazz) )
            {
                for( Object aC : c )
                {
                    Array.set(array, n++, Boolean.valueOf((String)aC));
                }
            }
        }
        else
        {
            int n = 0;
            for( Object aC : c )
            {
                Array.set(array, n++, aC);
            }
        }
    }
    
    private static boolean areEqualInner(Set<?> visitedSet, Object o1, Object o2, Set<AnnotatedElement> excludedFields)
    {
        if( o1 == o2 )
        {
            return true;
        }
        if( o1 == null || o2 == null )
        {
            return false;
        }
        Class<?> type1 = o1.getClass();
        Class<?> type2 = o2.getClass();
        if( !type1.equals(type2) )
        {
            return false;
        }
        if( o1 instanceof Collection )
        {
            Collection<?> c1 = (Collection<?>)o1;
            Collection<?> c2 = (Collection<?>)o2;
            if( c1.size() != c2.size() )
            {
                return false;
            }
            Iterator<?> iter2 = c2.iterator();
            for( Object aC1 : c1 )
            {
                if( !isVisitedContains(visitedSet, aC1) )
                {
                    appendToVisited(visitedSet, aC1);
                    if( !areEqualInner(visitedSet, aC1, iter2.next(), excludedFields) )
                    {
                        return false;
                    }
                }
            }
        }
        if( o1 instanceof Map )
        {
            Map<?,?> m1 = (Map<?,?>)o1;
            Map<?,?> m2 = (Map<?,?>)o2;
            if( m1.size() != m2.size() )
            {
                return false;
            }
            for( Entry<?,?> e : m1.entrySet() )
            {
                if( !isVisitedContains(visitedSet, e.getValue()) )
                {
                    appendToVisited(visitedSet, e.getValue());
                    if( !areEqualInner(visitedSet, e.getValue(), m2.get(e.getKey()), excludedFields) )
                    {
                        return false;
                    }
                }
            }
        }
        else if( type1.isArray() )
        {
            if( Array.getLength(o1) != Array.getLength(o2) )
            {
                return false;
            }
            for( int i = 0; i < Array.getLength(o1); i++ )
            {
                if( !isVisitedContains(visitedSet, Array.get(o1, i)) )
                {
                    appendToVisited(visitedSet, Array.get(o1, i));
                    if( !areEqualInner(visitedSet, Array.get(o1, i), Array.get(o2, i), excludedFields) )
                    {
                        return false;
                    }
                }
            }
        }
        else if( o1 instanceof String || o1 instanceof Integer || o1 instanceof Float || o1 instanceof Double || o1 instanceof Long
                || o1 instanceof Character || o1 instanceof Byte || o1 instanceof Boolean )
        {
            return o1.equals(o2);
        }
        else
        {
            List<Method> getters1 = getGetters(type1);
            List<Method> getters2 = getGetters(type2);
            if( getters1.size() != getters2.size() )
            {
                return false;
            }
            for( Method getter : getters1 )
            {
                if( excludedFields.contains(getter) )
                {
                    continue;
                }
                // Skip static methods
                if( Modifier.isStatic(getter.getModifiers()) )
                {
                    continue;
                }
                try
                {
                    Object fO1 = getter.invoke(o1);
                    Object fO2 = getter.invoke(o2);
                    if( !isVisitedContains(visitedSet, fO1) )
                    {
                        appendToVisited(visitedSet, fO1);
                        if( !areEqualInner(visitedSet, fO1, fO2, excludedFields) )
                        {
                            System.out.println("Not equal.Method:" + getter.getName() + ";Val1=" + fO1 + ";Val2=" + fO2);
                            return false;
                        }
                    }
                }
                catch( Exception e )
                {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean isVisitedContains(Set<?> visitedSet, Object o)
    {
        if( o == null )
        {
            return false;
        }
        return visitedSet.contains(o);
    }
    
    @SuppressWarnings("unchecked")
    public static void appendToVisited(Set visitedSet, Object o)
    {
        if( o != null )
        {
            visitedSet.add(o);
        }
    }
    
    public static boolean areEqual(Object o1, Object o2)
    {
        return areEqual(o1, o2, new HashSet<AnnotatedElement>());
    }
    
    public static boolean areEqual(Object o1, Object o2, Set<AnnotatedElement> excludedFields)
    {
        Set<Object> visitedSet = new HashSet<>();
        return areEqualInner(visitedSet, o1, o2, excludedFields);
    }
    
    public static String escapeXMLAttribute(String s)
    {
        return s.replaceAll("\"", "&quot;").replaceAll("&", "&amp;");
    }
    
    public static Object instantiate(Class<?> clazz)
    {
        try
        {
            Constructor<?>[] constructors = clazz.getConstructors();
            Arrays.sort( constructors, Comparator.comparingInt( c -> c.getParameterTypes().length ) );
            for( Constructor<?> constructor : constructors )
            {
                Object[] params = new Object[constructor.getParameterTypes().length];
                for( int j = 0; j < constructor.getParameterTypes().length; j++ )
                {
                    params[j] = null;
                }
                return constructor.newInstance(params);
            }
        }
        catch( Exception e )
        {
            // If public constructors fail, try to find a no-arg constructor
            try
            {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            }
            catch( Exception ex )
            {
                // Fall back to trying all declared constructors
                try
                {
                    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
                    Arrays.sort( constructors, Comparator.comparingInt( c -> c.getParameterTypes().length ) );
                    for( Constructor<?> constructor : constructors )
                    {
                        Object[] params = new Object[constructor.getParameterTypes().length];
                        for( int j = 0; j < constructor.getParameterTypes().length; j++ )
                        {
                            params[j] = null;
                        }
                        constructor.setAccessible(true);
                        return constructor.newInstance(params);
                    }
                }
                catch( Exception finalEx )
                {
                    // All attempts failed
                }
            }
        }
        return null;
    }
    
    private static Method getSetter(Class<?> clazz, String fieldName)
    {
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method[] methods = clazz.getMethods();
        for( Method method : methods )
        {
            if( method.getName().equals(setterName) && method.getParameterCount() == 1 )
            {
                return method;
            }
        }
        return null;
    }
    
    private static Method getGetter(Class<?> clazz, String fieldName)
    {
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String booleanGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        
        Method[] methods = clazz.getMethods();
        for( Method method : methods )
        {
            if( (method.getName().equals(getterName) || method.getName().equals(booleanGetterName)) 
                && method.getParameterCount() == 0 && !method.getReturnType().equals(void.class) )
            {
                return method;
            }
        }
        return null;
    }
    
    private static Method getFieldGetter(Class<?> clazz, String name)
    {
        List<Method> methods = getClassGetters(clazz);
        String getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String booleanGetterName = "is" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        
        for( Method method : methods )
        {
            if( method.getName().equals(getterName) || method.getName().equals(booleanGetterName) )
            {
                return method;
            }
        }
        return null;
    }
    
    private static List<Method> getGetters(Class<?> clazz)
    {
        List<Method> getters = new ArrayList<>();
        Method[] methods = clazz.getMethods();
        for( Method method : methods )
        {
            if( isGetter(method) )
            {
                getters.add(method);
            }
        }
        return getters;
    }
    
    private static boolean isGetter(Method method)
    {
        if( method.getParameterCount() != 0 )
        {
            return false;
        }
        if( method.getReturnType().equals(void.class) )
        {
            return false;
        }
        if( Modifier.isStatic(method.getModifiers()) )
        {
            return false;
        }
        String name = method.getName();
        return (name.startsWith("get") && name.length() > 3) || 
               (name.startsWith("is") && name.length() > 2 && 
                (method.getReturnType().equals(boolean.class) || method.getReturnType().equals(Boolean.class)));
    }
    
    public static String getDateValue(Object o)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return Utils.escapeXMLAttribute(sdf.format((Date)o));
    }
    
    private static void setDateValue(Object o, Method setter, String value)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        try
        {
            setter.invoke(o, sdf.parse(value, new ParsePosition(0)));
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
    
    private static void setTimeValue(Object o, Method setter, String value)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        try
        {
            setter.invoke(o, new java.sql.Time(sdf.parse(value, new ParsePosition(0)).getTime()));
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
    
    public static Method getField(Class<?> clazz, String superClassName, String fieldName) throws SecurityException, NoSuchFieldException
    {
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String booleanGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        
        String clazzName = clazz.getName();
        if( clazzName.equals(superClassName) )
        {
            Method[] methods = clazz.getDeclaredMethods();
            for( Method method : methods )
            {
                if( (method.getName().equals(getterName) || method.getName().equals(booleanGetterName)) 
                    && method.getParameterCount() == 0 && !method.getReturnType().equals(void.class) )
                {
                    return method;
                }
            }
            throw new NoSuchFieldException("No getter found for field: " + fieldName);
        }
        
        Class<?> superClass = clazz;
        do
        {
            superClass = superClass.getSuperclass();
            if( superClass == null )
                throw new NoSuchFieldException("No getter found for field: " + fieldName);
            clazzName = superClass.getName();
        }
        while( !clazzName.equals(superClassName) );
        
        Method[] methods = superClass.getDeclaredMethods();
        for( Method method : methods )
        {
            if( (method.getName().equals(getterName) || method.getName().equals(booleanGetterName)) 
                && method.getParameterCount() == 0 && !method.getReturnType().equals(void.class) )
            {
                return method;
            }
        }
        throw new NoSuchFieldException("No getter found for field: " + fieldName);
    }
    
    public static Method[] getFields(Class<?> clazz)
    {
        List<Method> methods = getClassGetters(clazz);
        return methods.toArray(new Method[methods.size()]);
    }
    
    private static List<Method> getClassGetters(Class<?> clazz)
    {
        if( !clazz.isArray() && !clazz.isPrimitive() )
        {
            List<Method> methodsList = new ArrayList<>();
            Method[] methods = clazz.getDeclaredMethods();
            for( Method method : methods )
            {
                if( isGetter(method) )
                {
                    methodsList.add(method);
                }
            }
            Class<?> superClass = clazz.getSuperclass();
            if( superClass != null )
            {
                methodsList.addAll(getClassGetters(superClass));
            }
            return methodsList;
        }
        return new ArrayList<>();
    }
    
    public static String getPrimitiveWrapperElementName(Class<?> type)
    {
        return primitiveWrapperXmlElements.get(type);
    }
    
    public static boolean isPrimiteWrapperElementName(String name)
    {
        return primitiveWrapperXmlElements.containsValue(name);
    }
    
    public static Class<?> getPrimitiveWrapperType(String name)
    {
        return StreamEx.ofKeys(primitiveWrapperXmlElements, name::equals).findAny().orElse(null);
    }
    
    public static Object getPrimitiveWrapperValue(Class<?> type, String s)
    {
        // order is important for performance, the most probable value first
        if( Integer.class.equals(type) )
        {
            return Integer.valueOf(s);
        }
        else if( Double.class.equals(type) )
        {
            return Double.valueOf(s);
        }
        else if( Float.class.equals(type) )
        {
            return Float.valueOf(s);
        }
        else if( Long.class.equals(type) )
        {
            return Long.valueOf(s);
        }
        else if( Boolean.class.equals(type) )
        {
            return Boolean.valueOf(s);
        }
        else if( Short.class.equals(type) )
        {
            return Short.valueOf(s);
        }
        else if( Character.class.equals(type) )
        {
            return s.charAt(0);
        }
        else if( Byte.class.equals(type) )
        {
            return Byte.valueOf(s);
        }
        return null;
    }
}