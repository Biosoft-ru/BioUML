package ru.biosoft.util.serialization.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import ru.biosoft.util.serialization.ObjectSerializationHandlerSupport;
import ru.biosoft.util.serialization.SerializationException;
import ru.biosoft.util.serialization.Utils;


/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 04.05.2006
 * Time: 16:53:51
 * <p/>
 */
public class XMLObjectSerializationHandler extends ObjectSerializationHandlerSupport
{
    Stack<TypedElement> contexts;

    private XMLDocument document;
    private XMLDocument.Element root;

    private static class TypedElement
    {
        public XMLDocument.Element element;
        public String elementType;

        public TypedElement( XMLDocument.Element element, String type )
        {
            this.element = element;
            this.elementType = type;
        }
    }

    XMLObjectSerializationHandler()
    {
        reinit();
    }

    @Override
    public String getString()
    {
        return writer.toString();
    }

    @Override
    public void beginObject( Class<?> clazz, String objectName, String id )
    {
        TypedElement typedElement = contexts.peek();

        // create <object name="" type="" alias=""> element
        XMLDocument.Element objectElement = document.createElement( Constants.OBJECT_ELEMENT );
        setTypeAttributes( objectElement, clazz );
        if( !Utils.isEmpty( objectName ) )
        {
            objectElement.setAttribute( Constants.NAME_ATTRIBUTE, objectName );
        }
        
        if( !Utils.isEmpty( id ) )
        {
            objectElement.setAttribute( Constants.OBJECT_ID_ATTRIBUTE, id );
        }

        typedElement.element.appendChild( objectElement );

        // create <value ... > element
        XMLDocument.Element valueElement = document.createElement( Constants.VALUE_ELEMENT );
        objectElement.appendChild( valueElement );
        contexts.push( new TypedElement( valueElement, Constants.VALUE_ELEMENT ) );
    }
    
    @Override
    public void beginReference(String id, String name)
    {
        TypedElement typedElement = contexts.peek();
        
        XMLDocument.Element objectReferenceElement = document.createElement( Constants.OBJECT_REFERENCE_ELEMENT );
        objectReferenceElement.setAttribute( Constants.OBJECT_ID_ATTRIBUTE, id );
        objectReferenceElement.setAttribute( Constants.NAME_ATTRIBUTE, name );
        typedElement.element.appendChild( objectReferenceElement );
    }

    @Override
    public void beginArray( Class<?> clazz, String arrayName, String id )
    {
        beginTuple( clazz, arrayName, Constants.ARRAY_ELEMENT, id );
    }

    @Override
    public void beginCollection( Class<?> clazz, String collectionName, String id )
    {
        beginTuple( clazz, collectionName, Constants.COLLECTION_ELEMENT, id );
    }

    @Override
    public void beginMap( Class<?> clazz, String dictionaryName, String id ) throws SerializationException
    {
        beginTuple( clazz, dictionaryName, Constants.DICTIONARY_ELEMENT, id );
    }

    private void beginTuple( Class<?> clazz, String name, String tupleType, String id )
    {
        TypedElement typedElement = contexts.peek();

        // create <collection name="" type="" alias=""> element
        XMLDocument.Element tupleElement = document.createElement( tupleType );
        setTypeAttributes( tupleElement, clazz );
        if( !Utils.isEmpty( name ) )
        {
            tupleElement.setAttribute( Constants.NAME_ATTRIBUTE, name );
        }
        
        if( !Utils.isEmpty( id ) )
        {
            tupleElement.setAttribute( Constants.OBJECT_ID_ATTRIBUTE, id );
        }

        typedElement.element.appendChild( tupleElement );
        contexts.push( new TypedElement( tupleElement, tupleType ) );
    }

    private void setTypeAttributes( XMLDocument.Element element, Class<?> type )
    {
        if( hasAlias( type ) )
        {
            element.setAttribute( Constants.TYPE_ALIAS_ATTRIBUTE, getObjectName( type.getName() ) );
        }
        else
        {
            element.setAttribute( Constants.TYPE_ATTRIBUTE, type.getName() );
        }
    }

    @Override
    public void endArray()
    {
        endElement();
    }

    @Override
    public void endCollection()
    {
        endElement();
    }

    @Override
    public void endMap()
    {
        endElement();
    }
    
    @Override
    public void endReference()
    {
        //endElement();
    }

    @Override
    public void endObject()
    {
        endElement();
    }

    private void endElement()
    {
        contexts.pop();
    }

    @Override
    public void handlePrimitive( Class<?> fieldType, String fieldName, Object fieldValue )
    {
        TypedElement typedElement = contexts.peek();
        if( Constants.VALUE_ELEMENT.equals( typedElement.elementType ) )
        {
            typedElement.element.setAttribute( fieldName, Utils.escapeXMLAttribute( fieldValue.toString() ) );
        }
        else
        {
            // it is inside of array or collection
            XMLDocument.Element primitiveElement = document.createElement( Constants.PRIMITIVE_ELEMENT );
            primitiveElement.setAttribute( Constants.VALUE_ATTRIBUTE, Utils.escapeXMLAttribute( fieldValue.toString() ) );
            typedElement.element.appendChild( primitiveElement );
        }
    }

    @Override
    public void handlePrimitiveWrapper( Class<?> type, String fieldName, Object fieldValue ) throws SerializationException
    {
        TypedElement typedElement = contexts.peek();
        if( Constants.VALUE_ELEMENT.equals( typedElement.elementType ) )
        {
            typedElement.element.setAttribute( fieldName, Utils.escapeXMLAttribute( fieldValue.toString() ) );
        }
        else
        {
            // use own element only when not in "value" conext
            String elementName = Utils.getPrimitiveWrapperElementName( type );
            XMLDocument.Element primitiveWrapperElement = document.createElement( elementName );
            primitiveWrapperElement.setAttribute( Constants.VALUE_ATTRIBUTE, Utils.escapeXMLAttribute( fieldValue.toString() ) );
            typedElement.element.appendChild( primitiveWrapperElement );
        }
    }


    @Override
    public void handleNull( String objectName )
    {
        TypedElement typedElement = contexts.peek();
        XMLDocument.Element nullElement = document.createElement( Constants.NULL_ELEMENT );

        if( !Utils.isEmpty( objectName ) )
        {
            nullElement.setAttribute( Constants.NAME_ATTRIBUTE, objectName );
        }

        typedElement.element.appendChild( nullElement );
    }

    @Override
    public void handleDate( Object o, String fieldName ) throws SerializationException
    {
        TypedElement typedElement = contexts.peek();
        String value = Utils.getDateValue( o );
        if( Constants.VALUE_ELEMENT.equals( typedElement.elementType ) )
        {
            typedElement.element.setAttribute( fieldName, value );
        }
        else
        {
            // it is inside of array or collection
            XMLDocument.Element primitiveElement = document.createElement( Constants.PRIMITIVE_ELEMENT );
            primitiveElement.setAttribute( Constants.VALUE_ATTRIBUTE, value );
            typedElement.element.appendChild( primitiveElement );
        }
    }

    @Override
    public void reinit()
    {
        writer = null;
        contexts = new Stack<>();
        document = new XMLDocument();
        root = document.createElement( Constants.ROOT_ELEMENT );
        document.appendChild( root );
        contexts.push( new TypedElement( root, Constants.ROOT_ELEMENT ) );
        createAliasesElement();
    }

    private void createAliasesElement()
    {
        if( !aliases.isEmpty() )
        {
            for( Entry<Object, String> entry : aliases.entrySet() )
            {
                XMLDocument.Element aliasElement = document.createElement( Constants.ALIAS_ELEMENT );
                aliasElement.setAttribute( Constants.TYPE_ALIAS_ATTRIBUTE, Utils.escapeXMLAttribute( entry.getValue() ) );
                aliasElement.setAttribute( Constants.TYPE_ATTRIBUTE, Utils.escapeXMLAttribute( entry.getKey().toString() ) );
                root.appendChild( aliasElement );
            }
        }
    }

    @Override
    public void endSerialization() throws SerializationException
    {
        try
        {
            if( writer == null )
            {
                writer = new StringWriter();
            }
            document.serialize( writer );
        }
        catch( IOException e )
        {
            throw new SerializationException( e );
        }
    }

    @Override
    public void removeIdAttributesFromUnreferencedObjects(Set<?> objectIds)
    {
        for(XMLDocument.Element el : document.children)
        {
            removeIdAttrs(objectIds, el);
        }
    }
    
    protected void removeIdAttrs(Set<?> objectIds, XMLDocument.Element e)
    {
        if (objectIds.size() == 0)
            return;
        
        for(XMLDocument.Element el : e.children)
        {
            if (el.attributes.containsKey(Constants.OBJECT_ID_ATTRIBUTE))
            {
                int id = Integer.parseInt(el.attributes.get(Constants.OBJECT_ID_ATTRIBUTE));
                if (objectIds.contains(id))
                {
                    el.attributes.remove(Constants.OBJECT_ID_ATTRIBUTE);
                }
            }
            
            removeIdAttrs(objectIds, el);
        }
    }
}
