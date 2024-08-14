package ru.biosoft.util.serialization.xml;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import ru.biosoft.util.serialization.CustomTypeFactory;
import ru.biosoft.util.serialization.Utils;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 06.05.2006
 * Time: 17:45:52
 */
public class ParserImpl extends DefaultHandler
{
    Stack<ContextBase> contexts;

    private Map<String, String> aliases;
    
    private Object rootValue;
    
    private Map<Integer,Object> id2Object;
    private List<ReferenceDescriptor> referenceDescriptors;

    public void reset()
    {
        contexts = new Stack<>();
        aliases = new HashMap<>();
        rootValue = new Object();
        id2Object = new HashMap<>();
        referenceDescriptors = new ArrayList<>();
    }

    public ParserImpl()
    {
        reset();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if( Constants.PRIMITIVE_ELEMENT.equals( qName ) )
        {
            parsePrimitive( attributes );
        }
        else if( Constants.VALUE_ELEMENT.equals( qName ) )
        {
            parseObjectValue( attributes );
        }
        else if( Constants.OBJECT_ELEMENT.equals( qName ) )
        {
            parseObject( attributes );
        }
        else if( Constants.OBJECT_REFERENCE_ELEMENT.equals( qName ) )
        {
            parseReference( attributes );
        }
        else if( Constants.ARRAY_ELEMENT.equals( qName ) )
        {
            parseArray( attributes );
        }
        else if( Constants.COLLECTION_ELEMENT.equals( qName ) )
        {
            parseCollection( attributes );
        }
        else if( Constants.DICTIONARY_ELEMENT.equals( qName ) )
        {
            parseMap( attributes );
        }
        else if( Constants.NULL_ELEMENT.equals( qName ) )
        {
            parseNull( attributes );
        }
        else if( Constants.ALIAS_ELEMENT.equals( qName ) )
        {
            parseAliases( attributes );
        }
        else if( Constants.ROOT_ELEMENT.equals( qName ) )
        {
            contexts.push( new RootContext() );
        }
        else if( Utils.isPrimiteWrapperElementName( qName ) )
        {
            parsePrimitiveWrapper( qName, attributes );
        }
    }

    private void parseNull( Attributes attributes )
    {
        ContextBase context = contexts.peek();
        String name = getName( attributes );

        context.setObject( name, null );
    }

    private void parsePrimitive( Attributes attributes )
    {
        ContextBase context = contexts.peek();
        assert context instanceof ArrayContext ||
               context instanceof MapContext;

        context.setObject( null, attributes.getValue( Constants.VALUE_ATTRIBUTE ) );
    }

    private void parsePrimitiveWrapper( String name, Attributes attributes )
    {
        ContextBase context = contexts.peek();

        assert context instanceof ArrayContext ||
               context instanceof MapContext ||
               context instanceof RootContext;

        context.setObject( name, getPrimitiveWrapperValue( attributes, name ) );
    }

    private void parseObjectValue( Attributes attributes )
    {
        Object context = contexts.peek();
        Object destination = null;
        if( context instanceof ArrayContext )
        {
            ArrayContext arrayContext = ( ArrayContext )context;
            destination = arrayContext.values.get( arrayContext.values.size() - 1 );
        }
        else if( context instanceof ObjectContext )
        {
            ObjectContext objectContext = ( ObjectContext )context;
            destination = objectContext.value;
        }
        if( destination != null )
        {
            for( int i = 0; i < attributes.getLength(); i++ )
            {
                Utils.setFieldValue( destination, attributes.getQName( i ), attributes.getValue( i ) );
            }
        }
    }

    private void parseAliases( Attributes attributes )
    {
        aliases.put( attributes.getValue( Constants.TYPE_ALIAS_ATTRIBUTE ), attributes.getValue( Constants.TYPE_ATTRIBUTE ) );
    }

    private void parseCollection( Attributes attributes )
    {
        contexts.push( new CollectionContext( getName( attributes ), getType( attributes ) ) );
    }

    private void parseMap( Attributes attributes ) throws SAXException
    {
        contexts.push( new MapContext( getName( attributes ), getType( attributes ) ) );
    }

    private void parseArray( Attributes attributes )
    {
        contexts.push( new ArrayContext( getName( attributes ), getType( attributes ) ) );
    }

    public void error( String message ) throws SAXException
    {
        error( message, null );
    }

    public void error( String message, Exception e ) throws SAXException
    {
        super.error( new SAXParseException( message, null, e ) );
    }

    private void parseObject( Attributes attributes ) throws SAXException
    {
        Object o = getObject( attributes );
        String name = getName( attributes );
        Integer id = getElementId(attributes);
        if (id != null)
        {
            id2Object.put(id, o);
        }
        contexts.push( new ObjectContext( name, o ) );
        if( o == null )
        {
            error( "Could not instantiate object " + name + " of type " + getType( attributes ) );
        }
    }
    
    private void parseReference(Attributes attributes) throws SAXException
    {
        try
        {
            Object holder = contexts.peek().getValue();
            ReferenceDescriptor descr = getReferenceDescriptor(holder, attributes);
            referenceDescriptors.add(descr);
        }
        catch (Exception e)
        {
             error( e.getMessage(), e );
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        try
        {
            if( Constants.OBJECT_ELEMENT.equals( qName ) )
            {
                createObject();
            }
            else if( Constants.VALUE_ELEMENT.equals( qName ) )
            {
                createObjectValue();
            }
            else if( Constants.ARRAY_ELEMENT.equals( qName ) )
            {
                createComplexFromContext( ArrayContext.class );
            }
            else if( Constants.COLLECTION_ELEMENT.equals( qName ) )
            {
                createComplexFromContext( CollectionContext.class );
            }
            else if( Constants.DICTIONARY_ELEMENT.equals( qName ) )
            {
                createComplexFromContext( MapContext.class );
            }
        }
        catch( Exception e )
        {
            error( e.getMessage(), e );
        }
    }

    private void createComplexFromContext( Class<?> clazz ) throws Exception
    {
        ContextBase context = contexts.pop();
        assert clazz.equals( context.getClass() );

        ContextBase parentContext = contexts.peek();
        assert ( parentContext != null );

        parentContext.setObject( context.getName(), context.getValue() );
    }

    private void createObjectValue()
    {

    }

    private void createObject() throws Exception
    {
        ContextBase context = contexts.pop();
        assert context instanceof ObjectContext;

        Object o = context.getValue();
        // workaround for non-default constructible objects
        if( o instanceof CustomTypeFactory )
        {
            o = ( ( CustomTypeFactory )o ).getOriginObject();
        }

        ContextBase parentContext = contexts.peek();
        assert parentContext != null;

        parentContext.setObject( context.getName(), o );
    }

    private Object getObject( Attributes attributes ) throws SAXException
    {
        String typeName = getType( attributes );
        if( typeName != null )
        {
            try
            {
                return Utils.instantiate( Class.forName( typeName ) );
            }
            catch( Exception e )
            {
                error( e.getMessage(), e );
            }
        }
        return null;
    }

    private String getType( Attributes attributes )
    {
        String name = attributes.getValue( Constants.TYPE_ATTRIBUTE );
        if( name == null )
        {
            String alias = attributes.getValue( Constants.TYPE_ALIAS_ATTRIBUTE );
            if( alias != null )
            {
                name = aliases.get( alias );
            }
        }
        return name;
    }

    private Object getPrimitiveWrapperValue( Attributes attributes, String name )
    {
        String s = attributes.getValue( Constants.VALUE_ATTRIBUTE );
        Class<?> type = Utils.getPrimitiveWrapperType( name );
        return Utils.getPrimitiveWrapperValue( type, s );
    }

    private String getName( Attributes attributes )
    {
        return attributes.getValue( Constants.NAME_ATTRIBUTE );
    }
    
    private ReferenceDescriptor getReferenceDescriptor(Object holder, Attributes attributes)
    {
        ReferenceDescriptor descr = new ReferenceDescriptor();
        descr.holder = holder;
        descr.referenceId = Integer.parseInt(attributes.getValue( Constants.OBJECT_ID_ATTRIBUTE ));
        descr.fieldName = attributes.getValue( Constants.NAME_ATTRIBUTE );
        
        return descr;
    }
    
    private Integer getElementId(Attributes attributes)
    {
        String val = attributes.getValue( Constants.OBJECT_ID_ATTRIBUTE );
        if (val != null)
        {
            return Integer.valueOf(val);
        }
        
        return null;
    }
    
    private void resolveReferences()
    {
        for(ReferenceDescriptor descr : referenceDescriptors)
        {
            Object referenced = this.id2Object.get(descr.referenceId);
            Utils.setFieldValue(descr.holder, descr.fieldName, referenced);
        }
    }

    public Object getValue()
    {
        resolveReferences();
        return rootValue;
    }

    private class MapContext extends ContextBase
    {
        Map<Object, Object> d;
        public String type;
        public String name;

        public MapContext( String name, String type ) throws SAXException
        {
            this.type = type;
            this.name = name;

            Class<?> objectType = null;
            try
            {
                objectType = Class.forName( type );
            }
            catch( ClassNotFoundException e )
            {
                error( e.getMessage() );
            }

            try
            {
                d = ( Map<Object, Object> )objectType.newInstance();
            }
            catch( Exception e )
            {
                error( e.getMessage(), e );
            }
        }

        private Object lastAddedKey = null;

        @Override
        public void setObject( String name, Object o )
        {
            if( lastAddedKey == null )
            {
                lastAddedKey = o;
            }
            else
            {
                d.put( lastAddedKey, o );
                lastAddedKey = null;
            }

        }

        @Override
        public Object getValue()
        {
            return d;
        }

        @Override
        public String getName()
        {
            return name;
        }

    }

    private class ArrayContext extends ContextBase
    {
        List<Object> values = new ArrayList<>();
        public String type;
        public String name;

        public ArrayContext( String name, String type )
        {
            this.type = type;
            this.name = name;
        }

        @Override
        public void setObject( String name, Object o )
        {
            values.add( o );
        }

        @Override
        public Object getValue() throws Exception
        {
            Class<?> objectType = null;
            try
            {
                if( double.class.getName().equals( type ) )
                {
                    objectType = double.class;
                }
                else if( float.class.getName().equals( type ) )
                {
                    objectType = float.class;
                }
                else if( int.class.getName().equals( type ) )
                {
                    objectType = int.class;
                }
                else if( boolean.class.getName().equals( type ) )
                {
                    objectType = boolean.class;
                }
                else
                {
                    objectType = Class.forName( type );
                }
            }
            catch( ClassNotFoundException e )
            {
                error( e.getMessage(), e );
            }

            Object array = null;
            if( objectType != null )
            {
                array = Array.newInstance( objectType, values.size() );
                Utils.setArrayValue( array, objectType, values );
            }
            return array;
        }

        @Override
        public String getName()
        {
            return name;
        }
    }

    private class ObjectContext extends ContextBase
    {
        public Object value;
        public String name;

        public ObjectContext( String name, Object value )
        {
            this.name = name;
            this.value = value;
        }

        @Override
        public void setObject( String name, Object o )
        {
            Utils.setFieldValue( value, name, o );
        }

        @Override
        public Object getValue()
        {
            return value;
        }

        @Override
        public String getName()
        {
            return name;
        }
    }

    private class CollectionContext extends ArrayContext
    {
        public CollectionContext( String name, String type )
        {
            super( name, type );
        }

        @Override
        public Object getValue() throws SAXException
        {
            Class<?> objectType = null;
            try
            {
                objectType = Class.forName( type );
            }
            catch( ClassNotFoundException e )
            {
                error( e.getMessage(), e );
                return null;
            }

            Object collection = null;
            try
            {
                collection = objectType.newInstance();
                ( ( Collection )collection ).addAll( values );
            }
            catch( Exception e )
            {
                error( e.getMessage(), e );
            }
            return collection;
        }
    }

    private class RootContext extends ContextBase
    {
        @Override
        public void setObject( String name, Object o )
        {
            rootValue = o;
        }

        @Override
        public Object getValue()
        {
            return rootValue;
        }

        @Override
        public String getName()
        {
            return null;
        }
    }

    private abstract class ContextBase
    {
        public abstract void setObject( String name, Object o );

        public abstract Object getValue() throws Exception;

        public abstract String getName();
    }

    private static class ReferenceDescriptor
    {
        Object holder;
        int referenceId;
        String fieldName;
    }
}
