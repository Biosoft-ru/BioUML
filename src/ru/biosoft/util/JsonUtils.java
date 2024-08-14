package ru.biosoft.util;

import java.io.File;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

public class JsonUtils
{
    public static JSONObject toOrgJson(JsonObject obj) throws JSONException
    {
        JSONObject result = new JSONObject();
        for( Member m : obj )
        {
            result.put( m.getName(), toOrgJson( m.getValue() ) );
        }
        return result;
    }

    public static JSONArray toOrgJson(JsonArray arr) throws JSONException
    {
        JSONArray result = new JSONArray();
        for( JsonValue val : arr )
        {
            result.put( toOrgJson( val ) );
        }
        return result;
    }

    public static Object toOrgJson(JsonValue value) throws JSONException
    {
        if( value.isObject() )
            return toOrgJson( value.asObject() );
        if( value.isArray() )
            return toOrgJson( value.asArray() );
        if( value.isNumber() )
            return value.asDouble();
        if( value.isString() )
            return value.asString();
        if( value.isBoolean() )
            return value.asBoolean();
        if( value.isNull() )
            return null;
        throw new JSONException( "Invalid value: " + value );
    }

    private static JsonArray fromArray(ArrayProperty array)
    {
        JsonArray result = new JsonArray();
        for( Property property : BeanUtil.properties( array ) )
        {
            result.add( fromProperty( property ) );
        }
        return result;
    }

    private static JsonValue fromProperty(Property property)
    {
        if( property instanceof CompositeProperty && !property.isHideChildren()
                && !property.getValueClass().isEnum()
                && !property.getValueClass().getPackage().getName().startsWith( "java." ) )
        {
            return fromBean( (CompositeProperty)property );
        }
        else if( property instanceof ArrayProperty )
        {
            return fromArray( (ArrayProperty)property );
        }
        else
        {
            Object value = property.getValue();
            if( value == null )
                return Json.NULL;
            if( value instanceof Double )
                return Json.value( (double)value );
            if( value instanceof Float )
                return Json.value( (float)value );
            if( value instanceof Integer )
                return Json.value( (int)value );
            if( value instanceof Boolean )
                return Json.value( (boolean)value );
            if( value instanceof Long )
                return Json.value( (long)value );
            return Json.value( TextUtil.toString( value ) );
        }
    }

    private static JsonObject fromBean(CompositeProperty model)
    {
        JsonObject result = new JsonObject();
        for( Property property : BeanUtil.properties( model ) )
        {
            if( !property.isVisible( Property.SHOW_HIDDEN ) )
                continue;
            result.add( property.getName(), fromProperty( property ) );
        }
        return result;
    }

    public static JsonObject fromBean(Object bean)
    {
        return fromBean( ComponentFactory.getModel( bean, Policy.DEFAULT, true ) );
    }

    public static JsonObject fromFile(String fileName) throws Exception
    {
        String json = new String( Files.readAllBytes( new File( fileName ).toPath() ), "utf8" );
        return Json.parse( json ).asObject();
    }

    public static JsonObject toMinimalJson(JSONObject obj) throws JSONException
    {
        JsonObject result = new JsonObject();
        for( Iterator<String> it = obj.keys(); it.hasNext(); )
        {
            String key = it.next();
            result.add( key, toMinimalJson( obj.get( key ) ) );
        }
        return result;
    }

    public static JsonArray toMinimalJson(JSONArray arr) throws JSONException
    {
        JsonArray result = new JsonArray();
        for( int i = 0; i < arr.length(); i++ )
        {
            result.add( toMinimalJson( arr.get( i ) ) );
        }
        return result;
    }

    public static JsonValue toMinimalJson(Object obj) throws JSONException
    {
        if( obj == null )
            return Json.NULL;
        if( obj instanceof JSONObject )
            return toMinimalJson( (JSONObject)obj );
        if( obj instanceof JSONArray )
            return toMinimalJson( (JSONArray)obj );
        if( obj instanceof String )
            return Json.value( (String)obj );
        if( obj instanceof Boolean )
            return Json.value( (Boolean)obj );
        if( obj instanceof Integer )
            return Json.value( (Integer)obj );
        if( obj instanceof Long )
            return Json.value( (Long)obj );
        if( obj instanceof Float )
            return Json.value( (Float)obj );
        if( obj instanceof Double )
            return Json.value( (Double)obj );
        throw new IllegalArgumentException( String.valueOf( obj ) );
    }

    public static StreamEx<JsonValue> arrayStream(JsonValue value)
    {
        return StreamEx.of( value.asArray().values() );
    }

    public static EntryStream<String, JsonValue> objectStream(JsonValue value)
    {
        JsonObject obj = value.asObject();
        return EntryStream.of( StreamSupport.stream( Spliterators.spliterator( obj.iterator(), obj.size(), 0 ), false ).map(
                m -> new AbstractMap.SimpleImmutableEntry<>( m.getName(), m.getValue() ) ) );
    }

    public static StreamEx<String> arrayOfStrings(JsonValue value)
    {
        return arrayStream( value ).map( JsonValue::asString );
    }

    public static StreamEx<JsonObject> arrayOfObjects(JsonValue value)
    {
        return arrayStream( value ).map( JsonValue::asObject );
    }

    public static StreamEx<JsonArray> arrayOfArrays(JsonValue value)
    {
        return arrayStream( value ).map( JsonValue::asArray );
    }

    public static JsonObject fromMap(Map<String, String> map)
    {
        JsonObject obj = new JsonObject();
        EntryStream.of( map ).forKeyValue( obj::add );
        return obj;
    }

    public static JsonArray fromCollection(Collection<String> collection)
    {
        JsonArray arr = new JsonArray();
        StreamEx.of( collection ).forEach( arr::add );
        return arr;
    }

    public static <T extends Map<? super String, ? super String>> T toMap(JsonObject object, Supplier<T> supplier)
    {
        T map = supplier.get();
        for( Member m : object )
        {
            map.put( m.getName(), m.getValue().asString() );
        }
        return map;
    }

    public static Collector<Entry<String, ? extends JsonValue>, ?, JsonObject> toObject()
    {
        return new Collector<Entry<String, ? extends JsonValue>, JsonObject, JsonObject>()
        {

            @Override
            public Supplier<JsonObject> supplier()
            {
                return JsonObject::new;
            }

            @Override
            public BiConsumer<JsonObject, Entry<String, ? extends JsonValue>> accumulator()
            {
                return (obj, e) -> obj.add( e.getKey(), e.getValue() );
            }

            @Override
            public BinaryOperator<JsonObject> combiner()
            {
                return ( (a, b) -> {
                    throw new UnsupportedOperationException();
                } );
            }

            @Override
            public Function<JsonObject, JsonObject> finisher()
            {
                return Function.identity();
            }

            @Override
            public Set<java.util.stream.Collector.Characteristics> characteristics()
            {
                return EnumSet.of( Characteristics.IDENTITY_FINISH );
            }
        };
    }

    public static Collector<JsonValue, ?, JsonArray> toArray()
    {
        return new Collector<JsonValue, JsonArray, JsonArray>()
        {

            @Override
            public Supplier<JsonArray> supplier()
            {
                return JsonArray::new;
            }

            @Override
            public BiConsumer<JsonArray, JsonValue> accumulator()
            {
                return JsonArray::add;
            }

            @Override
            public BinaryOperator<JsonArray> combiner()
            {
                return ( (a, b) -> {
                    throw new UnsupportedOperationException();
                } );
            }

            @Override
            public Function<JsonArray, JsonArray> finisher()
            {
                return Function.identity();
            }

            @Override
            public Set<java.util.stream.Collector.Characteristics> characteristics()
            {
                return EnumSet.of( Characteristics.IDENTITY_FINISH );
            }
        };
    }
}
