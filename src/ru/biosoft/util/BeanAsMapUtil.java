package ru.biosoft.util;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.beans.editors.PropertyEditorEx;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;

import one.util.streamex.EntryStream;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.TextUtil2.ParseException;
import ru.biosoft.util.bean.BeanInfoEx2;

public class BeanAsMapUtil
{
    public static Map<String, Object> convertBeanToMap(Object bean)
    {
        return convertBeanToMap( bean, x -> true );
    }

    public static Map<String, Object> convertBeanToMap(Object bean, Predicate<Property> propertyFilter)
    {
        CompositeProperty model = ComponentFactory.getModel(bean, Policy.DEFAULT, true);
        return compositePropertyToMap( model, propertyFilter );
    }

    private static Map<String, Object> compositePropertyToMap(CompositeProperty model, Predicate<Property> propertyFilter)
    {
        if( model.getValue() == null )
            return null;
        Map<String, Object> result = new LinkedHashMap<>();
        for( int j = 0; j < model.getPropertyCount(); j++ )
        {
            Property property = model.getPropertyAt( j );
            if( !propertyFilter.test( property ) )
                continue;
            result.put( property.getName(), propertyToObject( property, propertyFilter ) );
        }
        return result;
    }

    private static List<Object> arrayPropertyToList(ArrayProperty model, Predicate<Property> propertyFilter)
    {
        if( model.getValue() == null )
            return null;
        List<Object> result = new ArrayList<>();
        for( int j = 0; j < model.getPropertyCount(); j++ )
        {
            Property property = model.getPropertyAt( j );
            if( !propertyFilter.test( property ) )
                continue;
            result.add( propertyToObject( property, propertyFilter ) );
        }
        return result;
    }

    private static String propertyToString(Property property)
    {
        Object value = property.getValue();
        if( value == null )
            return null;
        return TextUtil2.toString( value );
    }

    private static final Class<?>[] TYPES_AS_IS = new Class<?>[] {
        Integer.class, int.class,
        Long.class, long.class,
        Double.class, double.class,
        Float.class, float.class,
        Boolean.class, boolean.class,
        String.class};

    private static Object propertyToSimpleObject(Property property)
    {
        Class<?> type = property.getValueClass();
        for(Class<?> typeAsIs : TYPES_AS_IS)
            if(type.equals( typeAsIs ))
                return property.getValue();
        return propertyToString( property );
    }

    private static Object propertyToObject(Property property, Predicate<Property> propertyFilter)
    {
        if( isCompositeProperty(property) )
            return compositePropertyToMap( (CompositeProperty)property, propertyFilter );
        else if( isArrayProperty( property ) )
            return arrayPropertyToList( (ArrayProperty)property, propertyFilter );
        else
            return propertyToSimpleObject( property );
    }

    public static boolean isCompositeProperty(Property property)
    {
        return property instanceof CompositeProperty && !property.isHideChildren()
                && !property.getValueClass().getPackage().getName().startsWith( "java." );
    }

    public static boolean isArrayProperty(Property property)
    {
        return property instanceof ArrayProperty && !property.isHideChildren();
    }

    public static void readBeanFromHierarchicalMap(Map<String, Object> hMap, Object bean)
    {
        readBeanFromHierarchicalMap( hMap, bean, false );
    }

    /**
     * @param strict whether a value that cannot be converted to its property's type is an
     *        {@link IllegalArgumentException}: a value that converts to nothing (an unknown choice name), to another
     *        type, or to an array with a missing item. An explicit null, and an empty string, still clear the
     *        property. Lenient binding keeps the historical behavior: such a value is assigned as whatever the
     *        conversion gave (possibly null) or, when the property rejects it, skipped. Either way the bean is
     *        bound property by property, and an array of beans item by item, so a failure can leave the
     *        properties bound before it changed: a caller that needs all or nothing binds a fresh bean and
     *        discards it on error.
     */
    public static void readBeanFromHierarchicalMap(Map<String, Object> hMap, Object bean, boolean strict)
    {
        ComponentModel model = ComponentFactory.getModel( bean );
        setCompositePropertyFromMap( model, hMap, strict );
    }

    private static void setPropertyFromObject(Property property, Object value, boolean strict)
    {
        if( value == null )
        {
            setPropertyFromSimpleObject( property, null, strict );
        }
        else if( isCompositeProperty( property ) )
        {
            if( ! ( value instanceof Map ) )
                throw new IllegalArgumentException();
            setCompositePropertyFromMap( (CompositeProperty)property, (Map<?, ?>)value, strict );
        }
        else if( isArrayProperty( property ) )
        {
            if( ! ( value instanceof List ) )
                throw new IllegalArgumentException();
            setArrayPropertyFromList( (ArrayProperty)property, (List<?>)value, strict );
        }
        else
        {
            setPropertyFromSimpleObject( property, value, strict );
        }
    }

    public static void setPropertyFromSimpleObject(Property property, Object value)
    {
        setPropertyFromSimpleObject( property, value, false );
    }

    private static void setPropertyFromSimpleObject(Property property, Object value, boolean strict)
    {
        if( property.isReadOnly() )
        {
            // strict: a read-only property given another value than its own is an error; echoing it back is fine
            if( strict && !Objects.deepEquals( property.getValue(), value ) )
                throw new IllegalArgumentException( property.getName() + " is read-only" );
            return;
        }
        Object input = value;
        if( value != null && !property.getValueClass().isInstance( value ) )
        {
            try
            {
                value = TextUtil2.fromString( property.getValueClass(), toText( property.getValueClass(), value ), true );
            }
            catch(ParseException e)
            {
                if( value instanceof String )
                {
                    try
                    {
                        PropertyEditor editor = (PropertyEditor)property.getPropertyEditorClass().newInstance();
                        if( editor instanceof PropertyEditorEx )
                        {
                            PropertyEditorEx editorEx = (PropertyEditorEx)editor;
                            editorEx.setBean( property.getOwner() );
                            editorEx.setDescriptor( property.getDescriptor() );
                        }
                        editor.setAsText( (String)value );
                        value = editor.getValue();
                    }
                    catch( Exception e2 )
                    {
                    }
                }
            }
        }
        if( strict && input != null )
            checkConverted( property, input, value );
        try
        {
            if( strict )
                invokeSetter( property, input, value );
            else
                property.setValue( value );
            if(property.getBooleanAttribute( BeanInfoEx2.STRUCTURE_CHANGING_PROPERTY ))
            {
                ComponentFactory.recreateChildProperties( property.getParent() );
            }
        }
        catch( NoSuchMethodException e )
        {
        }
    }

    /**
     * Strict binding: call the property's setter so that what it throws is reported. {@link Property#setValue}
     * (beans 3.0.4) invokes the same write method but logs and swallows any Throwable, also the
     * IllegalArgumentException of a null for an {@code int}. The contract is that the setter accepted the value: it
     * may normalize it. Only the model's own PropertyChangeEvent, which setValue fires for a bean that does not fire
     * its events itself, is not fired; the bean's setter fires its own, and a structure-changing property is
     * recreated by the caller. A property without a plain write method is set through setValue as before.
     */
    private static void invokeSetter(Property property, Object input, Object value) throws NoSuchMethodException
    {
        FeatureDescriptor descriptor = property.getDescriptor();
        Method write = descriptor instanceof PropertyDescriptor ? ( (PropertyDescriptor)descriptor ).getWriteMethod() : null;
        if( write == null )
        {
            property.setValue( value );
            return;
        }
        String text = input == null ? "null" : toText( property.getValueClass(), input );
        try
        {
            write.invoke( property.getOwner(), value );
        }
        catch( InvocationTargetException e )
        {
            Throwable cause = e.getCause();
            throw new IllegalArgumentException( property.getName() + ": " + text + " was refused"
                    + ( cause.getMessage() == null ? " (" + cause.getClass().getSimpleName() + ")" : ": " + cause.getMessage() ), cause );
        }
        catch( IllegalAccessException | IllegalArgumentException e )
        {
            throw new IllegalArgumentException( property.getName() + ": cannot set " + text, e );
        }
    }

    /**
     * Strict binding: the value {@code input} converted to must be a value of the property's type, and an
     * array one without missing items. {@link TextUtil2#fromString} answers an unknown name of a
     * {@code createInstance} factory (SiteAggregator, Species) with null, also in its strict mode, which other
     * callers rely on; so it is checked here, where a null for a non-empty input is a conversion failure.
     */
    private static void checkConverted(Property property, Object input, Object value)
    {
        Class<?> type = property.getValueClass();
        String text = toText( type, input );
        if( value == null )
        {
            if( text.isEmpty() && !type.isPrimitive() )
                return;
            throw new IllegalArgumentException( "cannot convert " + text + " to " + type.getSimpleName() );
        }
        if( !wrapper( type ).isInstance( value ) )
            throw new IllegalArgumentException( "cannot convert " + text + " to " + type.getSimpleName() );
        checkItems( property.getName(), input, value );
    }

    /** Every item of an object array, and of the arrays nested in it, is present. */
    private static void checkItems(String name, Object input, Object value)
    {
        Class<?> type = value.getClass();
        if( !type.isArray() || type.getComponentType().isPrimitive() )
            return;
        for( int i = 0; i < Array.getLength( value ); i++ )
        {
            Object item = Array.get( value, i );
            if( item == null )
                throw new IllegalArgumentException( name + "[" + i + "]: cannot convert " + itemText( input, i ) + " to "
                        + type.getComponentType().getSimpleName() );
            checkItems( name + "[" + i + "]", itemOf( input, i ), item );
        }
    }

    /** The {@code i}-th item of a list, JSON array or JSON-array text input, or null if it has none. */
    private static Object itemOf(Object input, int i)
    {
        try
        {
            if( input instanceof List )
                return ( (List<?>)input ).get( i );
            if( input instanceof JsonValue )
                return ( (JsonValue)input ).asArray().get( i );
            if( input instanceof String )
                return Json.parse( (String)input ).asArray().get( i );
        }
        catch( RuntimeException e )
        {
            // not a list of that size
        }
        return null;
    }

    /** The {@code i}-th item of the input, as text for an error message; the whole input if it is no list. */
    private static String itemText(Object input, int i)
    {
        Object item = itemOf( input, i );
        if( item instanceof JsonValue )
            return item.toString();
        if( item != null )
            return "\"" + TextUtil2.toString( item ) + "\"";
        return input instanceof List || input instanceof JsonValue ? "null" : TextUtil2.toString( input );
    }

    private static Class<?> wrapper(Class<?> type)
    {
        if( !type.isPrimitive() )
            return type;
        if( type == int.class )
            return Integer.class;
        if( type == long.class )
            return Long.class;
        if( type == double.class )
            return Double.class;
        if( type == float.class )
            return Float.class;
        if( type == boolean.class )
            return Boolean.class;
        if( type == short.class )
            return Short.class;
        if( type == byte.class )
            return Byte.class;
        if( type == char.class )
            return Character.class;
        throw new IllegalArgumentException( "no value of type " + type );
    }

    /**
     * The text a simple property's value is parsed from. For an array property shown as one value (a
     * multi-select of SiteAggregator) a list becomes a JSON array, the form {@link TextUtil2#fromString} reads
     * arrays from: {@code List.toString()} gives {@code [Count]}, which is no JSON and was bound as null. The items
     * are JSON strings, as {@code fromString} reads each item with {@code asString()} and parses it by the
     * component type (so {@code [1, 2]} binds an {@code int[]}); a nested list becomes a nested array. Any other
     * value, and a list holding a map, is converted as before.
     */
    private static String toText(Class<?> type, Object value)
    {
        if( type.isArray() && value instanceof Collection )
        {
            JsonArray array = toJsonArray( (Collection<?>)value );
            if( array != null )
                return array.toString();
        }
        return TextUtil2.toString( value );
    }

    private static JsonArray toJsonArray(Collection<?> items)
    {
        JsonArray array = new JsonArray();
        for( Object item : items )
        {
            if( item == null )
                array.add( Json.NULL );
            else if( item instanceof Collection )
            {
                JsonArray nested = toJsonArray( (Collection<?>)item );
                if( nested == null )
                    return null;
                array.add( nested );
            }
            else if( item instanceof Map )
                return null;
            else
                array.add( Json.value( TextUtil2.toString( item ) ) );
        }
        return array;
    }

    private static void setArrayPropertyFromList(ArrayProperty model, List<?> list, boolean strict)
    {
        if(model.getValue() == null)
        {
            Object value = Array.newInstance( model.getItemClass(), 0 );
            try
            {
                model.setValue( value );
            }
            catch( NoSuchMethodException e )
            {
                throw new RuntimeException(e);
            }
        }
        for( int i = 0; i < list.size(); i++ )
        {
            if( i == model.getPropertyCount() )
                try
                {
                    model.insertItem( i, null );
                }
                catch( InstantiationException ignore1 )
                {
                }
                catch( IllegalAccessException ignore2 )
                {
                }
            setPropertyFromObject( model.getPropertyAt( i ), list.get( i ), strict );
        }
        while( model.getPropertyCount() > list.size() )
            model.removeItem( model.getPropertyCount() - 1 );
    }

    private static void setCompositePropertyFromMap(CompositeProperty model, Map<?, ?> hMap, boolean strict)
    {
        for(int i = 0; i < model.getPropertyCount(); i++)
        {
            Property property = model.getPropertyAt( i );
            if(!hMap.containsKey( property.getName() ))
                continue;
            setPropertyFromObject(property, hMap.get( property.getName() ), strict );
        }
    }

    public static Map<String, Object> flattenMap(Map<String, Object> hMap)
    {
        Map<String, Object> fMap = new LinkedHashMap<>();
        flattenMap(hMap, fMap, "");
        return fMap;
    }

    private static void flattenMap(Map<?, ?> hMap, Map<String, Object> result, String prefix)
    {
        for( Map.Entry<?, ?> e : hMap.entrySet() )
        {
            if( ! ( e.getKey() instanceof String ) )
                throw new IllegalArgumentException();
            String key = (String)e.getKey();
            String newPrefix = prefix.isEmpty() ? key : prefix + "/" + key;
            flattenObject( e.getValue(), result, newPrefix );
        }
    }

    private static void flattenList(List<?> list, Map<String, Object> result, String prefix)
    {
        for( int i = 0; i < list.size(); i++ )
        {
            String key = "[" + i + "]";
            String newPrefix = prefix.isEmpty() ? key : prefix + "/" + key;
            flattenObject( list.get( i ), result, newPrefix );
        }
    }

    private static void flattenObject(Object obj, Map<String, Object> result, String prefix)
    {
        if( obj instanceof Map )
        {
            Map<?, ?> map = (Map<?, ?>)obj;
            if( map.isEmpty() )
                result.put( prefix, new LinkedHashMap<>() );
            else
                flattenMap( map, result, prefix );
        }
        else if( obj instanceof List )
        {
            List<?> list = (List<?>)obj;
            if( list.isEmpty() )
                result.put( prefix, new ArrayList<>() );
            else
                flattenList( list, result, prefix );
        }
        else
            result.put( prefix, obj );
    }

    private static final Pattern SUBSCRIPT_PATTERN = Pattern.compile( "\\[(\\d+)\\]" );

    public static Map<String, Object> expandMap(Map<String, Object> fMap)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for(Map.Entry<String, Object> e : fMap.entrySet())
        {
            Object curResult = result;
            String[] parts = TextUtil2.split( e.getKey(), '/' );
            for(int i = 0; i < parts.length - 1; i++)
                curResult = putIntoContainer( curResult, parts[i], createContainerForKey( parts[i+1] ) );
            curResult = putIntoContainer( curResult, parts[parts.length - 1], e.getValue() );
            if(curResult != e.getValue())
                throw new IllegalArgumentException();
        }
        return result;
    }

    private static Object putIntoContainer(Object container, String key, Object value)
    {
        Matcher matcher = SUBSCRIPT_PATTERN.matcher( key );
        if(matcher.matches())
        {
            if(!(container instanceof List))
                throw new IllegalArgumentException();
            List<Object> list = (List<Object>)container;
            int index = Integer.parseInt( matcher.group( 1 ) );
            while(index >= list.size())
                list.add( null );
            Object oldValue = list.get( index );
            if(oldValue == null)
                list.set( index, value );
            return list.get( index );
        }else
        {
            if(!(container instanceof Map))
                throw new IllegalArgumentException();
            Map<String, Object> map = (Map<String, Object>)container;
            if(!map.containsKey( key ))
                map.put( key, value );
            return map.get( key );
        }
    }

    private static Object createContainerForKey(String key)
    {
        if( SUBSCRIPT_PATTERN.matcher( key ).matches() )
            return new ArrayList<>();
        else
            return new LinkedHashMap<>();

    }

    public static Map<String, Object> getNonDefault(Object bean, Object defaultBean)
    {
        ComponentModel model = ComponentFactory.getModel( bean, Policy.DEFAULT, true );
        ComponentModel defaultModel = ComponentFactory.getModel( defaultBean, Policy.DEFAULT, false );//Should be false for ComponentFactory.recreateChildProperties to work properly
        return getNonDefaultInCompositeProperty( model, defaultModel );
    }

    private static Map<String, Object> getNonDefaultInCompositeProperty(CompositeProperty property, CompositeProperty defaultProperty)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for( int i = 0; i < property.getPropertyCount(); i++ )
        {
            Property child = property.getPropertyAt( i );
            if( defaultProperty.getPropertyCount() <= i )
                incompatibleBeans();
            Property defaultChild = defaultProperty.getPropertyAt( i );
            List<Object> childValue = getNonDefaultInProperty( child, defaultChild );
            if( !childValue.isEmpty() )
                result.put( child.getName(), childValue.get(0) );
        }
        return result;
    }

    private static void incompatibleBeans() throws IllegalArgumentException
    {
        throw new IllegalArgumentException( "Incompatible beans" );
    }

    /**
     * @return empty if p matches dp or single element list with the difference.
     * I don't use Optional<?> as return type here because I need Optional.of(null) to indicate that new value is null.
     */
    private static List<Object> getNonDefaultInProperty(Property p, Property dp)
    {
        if( !p.getName().equals( dp.getName() ) )
            incompatibleBeans();
        if( p.isReadOnly() || !p.isVisible( Property.SHOW_EXPERT ) )
            return Collections.emptyList();
        if( isCompositeProperty( p ) )
        {
            if( !isCompositeProperty( dp ) )
                incompatibleBeans();
            Map<String, Object> res = getNonDefaultInCompositeProperty( (CompositeProperty)p, (CompositeProperty)dp );
            return res.isEmpty() ? Collections.emptyList() : Collections.singletonList( res );
        }
        else if( isArrayProperty( p ) )
        {
            if( !isArrayProperty( dp ) )
                incompatibleBeans();
            Optional<?> res = getNonDefaultInArrayProperty( (ArrayProperty)p, (ArrayProperty)dp );
            return res.isPresent() ? Collections.singletonList( res.get() ) : Collections.emptyList();
        }
        else
        {
            Object value = p.getValue();
            Object defaultValue = dp.getValue();
            List<Object> result = Collections.emptyList();
            if( !Objects.equals( value, defaultValue ) )
            {
                result = Collections.singletonList( propertyToSimpleObject( p ) );
                try
                {
                    dp.setValue( value );
                }
                catch( NoSuchMethodException e )
                {
                    throw ExceptionRegistry.translateException( e );
                }
            }
            return result;
        }
    }

    private static Optional<List<?>> getNonDefaultInArrayProperty(ArrayProperty p, ArrayProperty dp)
    {
        boolean sameSize = p.getPropertyCount() == dp.getPropertyCount();

        while( dp.getPropertyCount() < p.getPropertyCount() )
            try
            {
                dp.insertItem( dp.getPropertyCount(), null );
            }
            catch( InstantiationException | IllegalAccessException e )
            {
                throw ExceptionRegistry.translateException( e );
            }
        while( p.getPropertyCount() < dp.getPropertyCount() )
            dp.removeItem( dp.getPropertyCount() - 1 );

        List<Object> result = new ArrayList<>( p.getPropertyCount() );
        boolean anyDiff = false;
        for( int i = 0; i < p.getPropertyCount(); i++ )
        {
            Property item = p.getPropertyAt( i );
            Property itemDefault = dp.getPropertyAt( i );
            List<?> itemDiff = getNonDefaultInProperty( item, itemDefault );
            Object child;
            if(itemDiff.isEmpty())
            {
                if(isCompositeProperty( item ))
                    child = new LinkedHashMap<>();
                else
                    child = propertyToObject( item, x->true );
            }
            else
            {
                anyDiff = true;
                child = itemDiff.get( 0 );
            }
            result.add( child );
        }

        if( sameSize && !anyDiff )
            return Optional.empty();
        return Optional.of( result );
    }

    /**
     * Convert hierarchical map from display name keys to simple name keys.
     * Bean may be changed by this procedure.
     */
    public static Map<String, Object> convertFromDisplayNames(Map<String, Object> hMap, Object bean, boolean allowBeanChanges)
    {
        ComponentModel model = ComponentFactory.getModel( bean, Policy.DEFAULT, true );
        return convertMapFromDisplayNames( hMap, model, allowBeanChanges );
    }

    private static Map<String, Object> convertMapFromDisplayNames(Map<?, ?> hMap, Property model, boolean allowBeanChanges)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for( int i = 0; i < model.getPropertyCount(); i++ )
        {
            Property property = model.getPropertyAt( i );

            String name = property.getName();
            String displayName = property.getDisplayName();

            Object oldValue;
            if( hMap.containsKey( displayName ) )
                oldValue = hMap.get( displayName );
            else if( hMap.containsKey( name ) )
                oldValue = hMap.get( name );
            else
                continue;

            Object value = convertObjectFromDisplayName( oldValue, property, allowBeanChanges );
            result.put( name, value );

            if( allowBeanChanges && !(value instanceof Map) && !(value instanceof List) )
                BeanAsMapUtil.setPropertyFromSimpleObject( property, value );
        }
        return result;
    }

    private static List<Object> convertListFromDisplayNames(List<?> list, ArrayProperty model, boolean allowBeanChanges)
    {
        List<Object> result = new ArrayList<>( list.size() );
        for( int i = 0; i < list.size(); i++ )
        {
            if( i >= model.getPropertyCount() )
                try
                {
                    model.insertItem( i, null );
                }
                catch( InstantiationException ignore1 )
                {
                }
                catch( IllegalAccessException ignore2 )
                {
                }

            Object item = convertObjectFromDisplayName( list.get( i ), model.getPropertyAt( i ), allowBeanChanges );
            result.add( item );
        }
        if( allowBeanChanges )
            while( model.getPropertyCount() > list.size() )
                model.removeItem( model.getPropertyCount() - 1 );
        return result;
    }

    private static Object convertObjectFromDisplayName(Object obj, Property property, boolean allowBeanChanges)
    {
        if( obj instanceof Map )
            return convertMapFromDisplayNames( (Map<?, ?>)obj, property, allowBeanChanges );
        else if( obj instanceof List )
        {
            if( !(property instanceof ArrayProperty) )
                throw new IllegalArgumentException("Expecting array property in bean");
            return convertListFromDisplayNames( (List<?>)obj, (ArrayProperty)property, allowBeanChanges );
        }
        else
            return obj;
    }

    /**
     * Convert hierarchical map from simple name keys to display name keys.
     * Bean should be in the same state as beanMap.
     */
    public static Map<String, Object> convertToDisplayNames(Map<String, Object> hMap, Object bean)
    {
        ComponentModel model = ComponentFactory.getModel( bean, Policy.DEFAULT, true );
        return convertMapToDisplayNames( hMap, model );
    }

    private static Map<String, Object> convertMapToDisplayNames(Map<?, ?> beanMap, Property model)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for( Map.Entry<?, ?> e : beanMap.entrySet() )
        {
            if( ! ( e.getKey() instanceof String ) )
                throw new IllegalArgumentException();
            String key = (String)e.getKey();
            Property property = model.findProperty( key );
            Object value = convertObjectToDisplayName( e.getValue(), property );
            String displayName = property.getDisplayName();
            result.put( displayName, value );
        }
        return result;
    }

    private static List<Object> convertListToDisplayNames(List<?> list, Property model)
    {
        return EntryStream.of( list ).mapKeys( model::getPropertyAt )
                .mapKeyValue( (property, obj) -> convertObjectToDisplayName( obj, property ) ).toList();
    }

    private static Object convertObjectToDisplayName(Object obj, Property property)
    {
        if( obj instanceof Map )
            return convertMapToDisplayNames( (Map<?, ?>)obj, property );
        else if( obj instanceof List )
            return convertListToDisplayNames( (List<?>)obj, property );
        else
            return obj;
    }
    
    
    public static Map<String, Object> makeOneBased(Map<String, Object> fMap)
    {
        return EntryStream.of( fMap ).mapKeys( k -> {
            String[] parts = k.split( "/", -1 );
            for(int i = 0; i < parts.length; i++)
            {
                Matcher matcher = SUBSCRIPT_PATTERN.matcher( parts[i] );
                if(matcher.matches())
                {
                    int zeroBasedIdx = Integer.parseInt( matcher.group( 1 ) );
                    parts[i] = "[" + ( zeroBasedIdx + 1 ) + "]"; 
                }
            }
            return String.join( "/", parts );
        } ).toCustomMap( LinkedHashMap::new );
    }
}
