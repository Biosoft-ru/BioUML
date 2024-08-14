package ru.biosoft.util.serialization;

import java.io.Writer;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 04.05.2006
 * Time: 16:29:24
 */
public interface ObjectSerializationHandler
{
    String getString();

    void beginObject( Class<?> clazz, String objectName, String id ) throws SerializationException;
    
    void beginReference(String id, String name);

    void beginArray( Class<?> clazz, String collectionName, String id ) throws SerializationException;

    void beginCollection( Class<?> fieldType, String collectionName, String id ) throws SerializationException;

    void beginMap( Class<?> fieldType, String dictionaryName, String id ) throws SerializationException;

    void endArray();

    void endCollection();

    void endMap();
    
    void endReference();

    void endObject();

    void handlePrimitive( Class<?> fieldType, String fieldName, Object fieldValue ) throws SerializationException;

    void reinit();

    void alias( Class<?> clazz, String name ) throws SerializationException;

    void endSerialization() throws SerializationException;

    void setWriter( Writer writer );

    void handleNull( String objectName ) throws SerializationException;

    void handleDate( Object o, String objectName ) throws SerializationException;

    void handlePrimitiveWrapper( Class<?> type, String objectName, Object o ) throws SerializationException;
    
    void removeIdAttributesFromUnreferencedObjects(Set<?> objectIds);
}
