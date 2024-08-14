package ru.biosoft.util.serialization;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
        Field field = getField(o.getClass(), fieldName);
        if( field != null )
        {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            try
            {
                if( fieldType.equals(String.class) )
                {
                    field.set(o, fieldValue);
                }
                else if( fieldValue == null )
                {
                    field.set(o, null);
                }
                else if( fieldType.equals(int.class) || fieldType.equals(Integer.class) )
                {
                    field.set(o, Integer.valueOf(fieldValue));
                }
                else if( fieldType.equals(double.class) || fieldType.equals(Double.class) )
                {
                    field.set(o, Double.valueOf(fieldValue));
                }
                else if( fieldType.equals(boolean.class) || fieldType.equals(Boolean.class) )
                {
                    field.set(o, Boolean.valueOf(fieldValue));
                }
                else if( fieldType.equals(Date.class) )
                {
                    setDateValue(o, field, fieldValue);
                }
                else if( fieldType.equals(java.sql.Time.class) )
                {
                    setTimeValue(o, field, fieldValue);
                }
                else if( fieldType.equals(long.class) || fieldType.equals(Long.class) )
                {
                    field.set(o, Long.valueOf(fieldValue));
                }
                else if( fieldType.equals(short.class) || fieldType.equals(Short.class) )
                {
                    field.set(o, Short.valueOf(fieldValue));
                }
                else if( fieldType.equals(float.class) || fieldType.equals(Float.class) )
                {
                    field.set(o, Float.valueOf(fieldValue));
                }
            }
            catch( IllegalAccessException e )
            {
                e.printStackTrace();
            }
        }
    }

    public static void setFieldValue(Object o, String fieldName, Object value)
    {
        Field field = getField(o.getClass(), fieldName);
        if( field != null )
        {
            field.setAccessible(true);
            try
            {
                field.set(o, value);
            }
            catch( IllegalAccessException e )
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
            Field[] fields1 = getFields(type1);
            Field[] fields2 = getFields(type2);
            if( fields1 == fields2 )
            {
                return false;
            }

            for( Field field : fields1 )
            {
                if( excludedFields.contains(field) )
                {
                    continue;
                }

                // do not take into account transient fields
                if( Modifier.isTransient(field.getModifiers()) )
                {
                    continue;
                }

                // do not take into account static fields
                if( Modifier.isStatic(field.getModifiers()) )
                {
                    continue;
                }

                field.setAccessible(true);
                try
                {
                    Object fO1 = field.get(o1);
                    Object fO2 = field.get(o2);

                    if( !isVisitedContains(visitedSet, fO1) )
                    {
                        appendToVisited(visitedSet, fO1);

                        if( !areEqualInner(visitedSet, fO1, fO2, excludedFields) )
                        {
                            System.out.println("Not equal.Type:" + field + ";Name:" + field.getName() + ";Val1=" + fO1 + ";Val2=" + fO2);
                            return false;
                        }
                    }
                }
                catch( IllegalAccessException e )
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

                constructor.setAccessible(true);
                return constructor.newInstance(params);
            }

            // force instantiation
            //            Constructor<?> constructor = clazz.getDeclaredConstructor( new Class[0] );
            //            constructor.setAccessible( true );
            //            return constructor.newInstance( new Object[0] );
        }
        catch( Exception e )
        {
        }
        return null;
    }

    public static Field getField(Class<?> clazz, String superClassName, String fieldName) throws SecurityException, NoSuchFieldException
    {
        String clazzName = clazz.getName();

        if( clazzName.equals(superClassName) )
            return clazz.getDeclaredField(fieldName);

        Class<?> superClass = clazz;

        do
        {
            superClass = superClass.getSuperclass();
            if( superClass == null )
                throw new NoSuchFieldException();

            clazzName = superClass.getName();
        }
        while( !clazzName.equals(superClassName) );

        return superClass.getDeclaredField(fieldName);
    }

    public static Field[] getFields(Class<?> clazz)
    {
        List<Field> fields = getClassFields(clazz);
        return fields.toArray(new Field[fields.size()]);
    }

    private static List<Field> getClassFields(Class<?> clazz)
    {
        if( !clazz.isArray() && !clazz.isPrimitive() )
        {
            List<Field> fieldsList = new ArrayList<>();
            Field[] fields = clazz.getDeclaredFields();
            for( Field field : fields )
            {
                fieldsList.add(field);
            }

            Class<?> superClass = clazz.getSuperclass();
            if( superClass != null )
            {
                fieldsList.addAll(getClassFields(superClass));
            }
            return fieldsList;
        }
        return new ArrayList<>();
    }

    private static Field getField(Class<?> clazz, String name)
    {
        List<Field> fields = getClassFields(clazz);
        for( Field field : fields )
        {
            if( name.equals(field.getName()) )
            {
                return field;
            }
        }
        return null;
    }

    public static String getDateValue(Object o)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return Utils.escapeXMLAttribute(sdf.format((Date)o));
    }

    private static void setDateValue(Object o, Field field, String value)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        try
        {
            field.set(o, sdf.parse(value, new ParsePosition(0)));
        }
        catch( IllegalAccessException | IllegalArgumentException e )
        {
        }
    }

    private static void setTimeValue(Object o, Field field, String value)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        try
        {
            field.set(o, new java.sql.Time(sdf.parse(value, new ParsePosition(0)).getTime()));
        }
        catch( IllegalAccessException e )
        {
        }
        catch( IllegalArgumentException e )
        {
        }
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
        // order is improtant for perfomance, the most propable value first
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
