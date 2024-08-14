package ru.biosoft.util.serialization;

import java.io.Writer;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import ru.biosoft.util.serialization.common.data.factories.SqlDateCustomTypeFactory;
import ru.biosoft.util.serialization.common.data.factories.SqlTimestampCustomTypeFactory;


/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 04.05.2006
 * Time: 16:28:18
 * <p/>
 * TODO 1) implement cyclic references check
 * TODO 2) implement CustomTypeConverter providing own XML/JSON/whatever
 * TODO for input type
 */
public class Serializer
{
    protected ObjectSerializationHandler handler;

    protected HashSet<AnnotatedElement> excluded = new HashSet<>();
    protected Map<Class, CustomTypeFactory> stubs = new HashMap<>();
    
    protected int idSeed = -1;
    protected Map<Object, Serializer.ObjectDescriptor> object2Descr = new HashMap<>();
    
    private boolean skipNulls;

    protected Serializer( ObjectSerializationHandler handler )
    {
        this.handler = handler;
        //register ser factories for some base types
        registerCustomTypeFactory( java.sql.Timestamp.class, new SqlTimestampCustomTypeFactory() );
        registerCustomTypeFactory( java.sql.Date.class, new SqlDateCustomTypeFactory() );
    }

    public synchronized String serialize( Object o ) throws SerializationException
    {
        return serialize( o, o != null ? o.getClass().getName() : "" );
    }

    public synchronized String serialize( Object o, String name ) throws SerializationException
    {
        try
        {
            handler.reinit();
            exploreObject( o, name );
            handler.removeIdAttributesFromUnreferencedObjects(getIdsOfUnreferencedObjects());
            handler.endSerialization();
            return handler.getString();
        }
        catch( IllegalAccessException e )
        {
            throw new SerializationException( e );
        }
    }

    public synchronized void serialize( Object o, Writer writer ) throws SerializationException
    {
        serialize( o, o != null ? o.getClass().getName() : "", writer );
    }

    public synchronized void serialize( Object o, String name, Writer writer ) throws SerializationException
    {

        try
        {
            handler.reinit();
            handler.setWriter( writer );
            exploreObject( o, name );
            handler.endSerialization();
        }
        catch( IllegalAccessException e )
        {
            throw new SerializationException( e );
        }
    }

    public void alias( Class clazz, String name ) throws SerializationException
    {
        handler.alias( clazz, name );
    }

    public void exclude( Field field )
    {
        excluded.add( field );
    }

    public void exclude( Class<LinkedList> clazz )
    {
        excluded.add( clazz );
    }

    public void resetExcluded()
    {
        excluded.clear();
    }

    public void setSkipNulls( boolean skipNulls )
    {
        this.skipNulls = skipNulls;
    }

    public void registerCustomTypeFactory( Class clazz, CustomTypeFactory factory )
    {
        stubs.put( clazz, factory );
    }

    protected void exploreObject( Object o, String objectName ) throws IllegalAccessException, SerializationException
    {
        if( o == null )
        {
            if( !skipNulls )
            {
                handler.handleNull( objectName );
            }
            return;
        }

        Class t = o.getClass();
        if( excluded.contains( t ) )
        {
            return;
        }

        Object obj = o;
        Class type = t;

        // if there is custom factory for this type, use it
        CustomTypeFactory factory = stubs.get( t );
        if( factory != null )
        {
            // serialize custom factory instance instead of original object
            obj = factory.getFactoryInstance( o );
            type = obj.getClass();
        }

        if( obj instanceof Collection )
        {
            exploreCollection(( Collection )obj, type, objectName);
        }
        else if( obj instanceof Map )
        {
            Map d = ( Map )obj;
            exploreMap( d, objectName );
        }
        else if( type.isArray() )
        {
            exploreArray( obj, type.getComponentType(), objectName );
        }
        else if( type.isPrimitive() || type.equals( String.class ) )
        {
            handler.handlePrimitive( type, objectName, o );
        }
        else if( type.equals( Integer.class ) ||
                 type.equals( Float.class ) ||
                 type.equals( Double.class ) ||
                 type.equals( Long.class ) ||
                 type.equals( Boolean.class ) )
        {
            handler.handlePrimitiveWrapper( type, objectName, o );
        }
        else if( Date.class.isAssignableFrom( type ) )
        {
            handler.handleDate( obj, objectName );
        }
        else
        {
            exploreObjectFields( obj, objectName );
        }
    }
    
    private void exploreCollection(Collection c, Class clazz, String collectionName) throws SerializationException, IllegalAccessException
    {
        if (!this.object2Descr.keySet().contains(c))
        {
            ++idSeed;
            this.object2Descr.put(c, new ObjectDescriptor(this.idSeed));
            handler.beginCollection( clazz, collectionName, ""+idSeed );
            for( Object o1 : c )
            {
                exploreObject( o1, "" );
            }
            
            handler.endCollection();
        }
        else
        {
            putReference(c, collectionName);
        }
    }

    private void exploreObjectFields( Object o, String objectName ) throws IllegalAccessException, SerializationException
    {
        Class clazz = o.getClass();
        Field[] fields = Utils.getFields( clazz );

        if (!this.object2Descr.keySet().contains(o))
        {
            ++this.idSeed;
            this.object2Descr.put(o, new ObjectDescriptor(this.idSeed));
            handler.beginObject( clazz, objectName, ""+this.idSeed );

            for( Field field : fields )
            {
                if( !excluded.contains( field ) &&
                    !Modifier.isTransient( field.getModifiers() ) &&
                    !Modifier.isStatic( field.getModifiers() ) )
                {
                    field.setAccessible( true );
                    exploreObject( field.get( o ), field.getName() );
                }
            }
            
            handler.endObject();
        }
        else
        {
            putReference(o, objectName);
        }
    }

    private void exploreArray( Object array, Class clazz, String arrayName ) throws IllegalAccessException, SerializationException
    {
        if (!this.object2Descr.keySet().contains(array))
        {
            ++this.idSeed;
            this.object2Descr.put(array, new ObjectDescriptor(this.idSeed));
            handler.beginArray( clazz, arrayName, ""+this.idSeed );
            if( clazz.isPrimitive() )
            {
                for( int i = 0; i < Array.getLength( array ); i++ )
                {
                    handler.handlePrimitive( clazz, null, Array.get( array, i ) );
                }
            }
            else
            {
                for( Object object : ( Object[] )array )
                {
                    // TODO: make array element names if needed
                    exploreObject( object, null );
                }
            }
            
            handler.endArray();
        }
        else
        {
            putReference(array, arrayName);
        }
    }

    protected void exploreMap( Map d, String objectName ) throws SerializationException, IllegalAccessException
    {
        if (!this.object2Descr.keySet().contains(d))
        {
            ++this.idSeed;
            this.object2Descr.put(d, new ObjectDescriptor(this.idSeed));
            handler.beginMap( d.getClass(), objectName, ""+this.idSeed );
            for( Object o : d.entrySet() )
            {
                Map.Entry entry = ( Map.Entry )o;
                exploreObject( entry.getKey(), "" );
                exploreObject( entry.getValue(), "" );
            }
            handler.endMap();
        }
        else
        {
            putReference(d, objectName);
        }
    }
    
    protected void putReference(Object o, String objectName)
    {
        ObjectDescriptor od = this.object2Descr.get(o);
        od.addRef();
        handler.beginReference(""+od.id, objectName);
        handler.endReference();
    }
    
    protected Set getIdsOfUnreferencedObjects()
    {
        Set set = new HashSet();
        for(Entry<Object, ObjectDescriptor> entry : this.object2Descr.entrySet())
        {
            if (entry.getValue().referenceCount == 1)
            {
                set.add(entry.getValue().id);
            }
        }
        
        return set;
    }
    
    protected static class ObjectDescriptor
    {
        public int id;
        public int referenceCount = 1;
        
        public ObjectDescriptor(int idArg)
        {
            this.id = idArg;
        }
        
        public void addRef()
        {
            ++this.referenceCount;
        }
    }
}
