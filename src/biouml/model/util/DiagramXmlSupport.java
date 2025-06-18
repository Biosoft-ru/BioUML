package biouml.model.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.SubDiagram.PortOrientation;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.XmlUtil;

public class DiagramXmlSupport extends DiagramXmlConstants
{
    protected static final Logger log = Logger.getLogger(DiagramXmlSupport.class.getName());

    protected static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected static void error(String messageBundleKey, String[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, (Object[])params);
        log.log(Level.SEVERE, message);
    }

    protected static void error(String messageBundleKey, String[] params, Throwable t)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, (Object[])params);
        log.log(Level.SEVERE, message);
        ExceptionRegistry.log(t);
    }

    protected static void warn(String messageBundleKey, String[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, (Object[])params);
        log.warning(message);
    }

    protected Diagram diagram = null;

    public DiagramXmlSupport()
    {
    }

    public Diagram getDiagram()
    {
        return diagram;
    }

    protected DiagramElement getDiagramElement(String diagramElement)
    {
        return getDiagramElement( diagramElement, DiagramElement.class );
    }

    protected <T extends DiagramElement> T getDiagramElement(String diagramElement, Class<T> clazz)
    {
        try
        {
            return CollectionFactory.getDataElement( diagramElement, diagram, clazz );
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, ExceptionRegistry.log( t ) );
            return null;
        }
    }

    public static Element getElement(Element element, String childName)
    {
        String elementName = element.getAttribute(NAME_ATTR);
        if( elementName.isEmpty() )
            elementName = element.getTagName();

        try
        {
            NodeList list = element.getChildNodes();
            Element result = null;
            for(Element node : XmlUtil.elements(list))
            {
                if( node.getNodeName().equals(childName) )
                {
                    if( result == null )
                        result = node;
                    else
                        warn("WARN_MULTIPLE_DECLARATION", new String[] {result.getTagName(), elementName, childName});
                }
            }

            return result;
        }
        catch( Throwable t )
        {
            error("ERROR_ELEMENT_PROCESSING", new String[] {elementName, elementName, childName, t.getMessage()});
        }

        return null;
    }

    protected static Class<?> getPropertyType(String type)
    {
        Class<?> clazz = typeNameToType.get(type);
        if(clazz != null)
            return clazz;
        try
        {
            return ClassLoading.loadClass( type );
        }
        catch( LoggedClassNotFoundException e )
        {
            e.log();
        }
        return null;
    }

    protected static String getPropertyType(Class<?> _class)
    {
        return StreamEx.ofKeys(typeNameToType, _class::equals).findAny().orElse(_class.getName());
    }

    protected static String getRequiredAttribute(Element element, String attrName, String diagramName)
    {
        if(!element.hasAttribute(attrName))
        {
            error("ERROR_REQUIRED_ATTRIBUTE_MISSING", new String[] {diagramName, attrName, element.getTagName()});
            return null;
        }
        return element.getAttribute(attrName);
    }

    protected static void readComment(DiagramElement diagramElement, Element element)
    {
        if( element.hasAttribute( COMMENT_ATTR ) )
            diagramElement.setComment( element.getAttribute( COMMENT_ATTR ) );
    }

    // stub for complex type
    protected static class Composite
    {
    }

    protected static class Array
    {
    }

    private static Map<String, Class<?>> typeNameToType = new HashMap<>();
    static
    {
        typeNameToType.put("String", String.class);
        typeNameToType.put("int", Integer.class);
        typeNameToType.put("boolean", Boolean.class);
        typeNameToType.put("double", Double.class);
        typeNameToType.put("pen", Pen.class);
        typeNameToType.put("brush", Brush.class);
        typeNameToType.put("dimension", Dimension.class);
        typeNameToType.put("font", ColorFont.class);
        typeNameToType.put("composite", Composite.class);
        typeNameToType.put("array", Array.class);
        typeNameToType.put("dataElementPath", DataElementPath.class);
        typeNameToType.put("color", Color.class);
        typeNameToType.put("Class", Class.class);
        typeNameToType.put("portOrientation", PortOrientation.class);
        typeNameToType.put( "variable", VariableName.class );
    }
    
    public static boolean isEligibleToTextUtil(Class propertyType)
    {
        return propertyType.isPrimitive() || XmlSerializationUtils.isPrimitiveWrapperElement( propertyType )
                || String.class.equals( propertyType ) || ru.biosoft.access.core.DataElementPath.class.equals( propertyType ) || Color.class.equals( propertyType )
                || Class.class.equals( propertyType ) || propertyType.isEnum();
    }
    
    protected static void setComment(Element element, String comment)
    {
        setOptionalAttribute(element, COMMENT_ATTR, comment);
    }

    /**
     * if value is not null and is not empty
     * set it as attribute
     */
    protected static void setOptionalAttribute(Element element, String name, String value)
    {
        if( value != null && !value.isEmpty() )
            element.setAttribute(name, validate(value));
    }
    

    /**
     * Validate strings before adding to the xml
     */
    protected static String validate(String str)
    {
        // TODO - implement
        return str;
    }
    
    public static void serializeDPS(Document doc, Element root, DynamicPropertySet dps, DynamicPropertySet registry,
            boolean writeIfdeafault)
    {
        for( DynamicProperty dp : dps )
            serializeDynamicProperty(doc, root, dp, registry, writeIfdeafault);
    }
    
    public static void serializeDPS(Document doc, Element root, DynamicPropertySet dps, DynamicPropertySet registry)
    {
        serializeDPS(doc, root, dps, registry, true);
    }

    public static void serializeDynamicProperty(Document doc, Element root, DynamicProperty dp, DynamicPropertySet registry)
    {
        serializeDynamicProperty(doc, root, dp, registry, true);
    }    
    
    public static void serializeDynamicProperty(Document doc, Element root, DynamicProperty dp, DynamicPropertySet registry, boolean writeIfDefault)
    {
        if( DPSUtils.isTransient( dp ) )
            return;

        String name = dp.getName();

        Element element;
        DynamicProperty templateProperty = registry != null ? registry.getProperty( name ) : null;
        if( templateProperty != null )
        {
            if (!writeIfDefault && templateProperty.getValue()!= null && templateProperty.getValue().equals(dp.getValue()))
                return;

            element = doc.createElement( PROPERTY_REF_ELEMENT );
        }
        else
        {
            element = doc.createElement( PROPERTY_ELEMENT );
            element.setAttribute( SHORT_DESCR_ATTR, dp.getShortDescription() );

            //write tags
            if( registry == null ) //write tags only for templates
            {
                Object tagValues = dp.getDescriptor().getValue( StringTagEditor.TAGS_KEY );
                if( tagValues != null )
                {
                    Element tagsElement = doc.createElement( TAGS_ELEMENT );
                    for( String tagValue : (String[])tagValues )
                    {
                        Element tag = doc.createElement( TAG_ELEMENT );
                        tag.setAttribute( NAME_ATTR, tagValue );
                        tag.setAttribute( VALUE_ATTR, tagValue );
                        tagsElement.appendChild( tag );
                    }
                    element.appendChild( tagsElement );
                }
            }
        }

        element.setAttribute( NAME_ATTR, name );

        Object value = dp.getValue();

        Class<?> propertyType = dp.getType();
        if( value != null && !propertyType.equals( value.getClass() ) )
        {
            propertyType = value.getClass();
        }

        if( isEligibleToTextUtil(propertyType))
        {
            if( templateProperty == null )
                element.setAttribute(TYPE_ATTR, getPropertyType(propertyType));

            if( value != null )
                element.setAttribute(VALUE_ATTR, TextUtil2.toString(value));
        }
        else if( Pen.class.equals( propertyType ) )
        {
            XmlSerializationUtils.serializePen( element, (Pen)value, ( templateProperty != null ) );
        }
        else if( Brush.class.equals( propertyType ) )
        {
            XmlSerializationUtils.serializeBrush( element, (Brush)value, ( templateProperty != null ) );
        }
        else if( Dimension.class.equals( propertyType ) )
        {
            XmlSerializationUtils.serializeDimension( element, (Dimension)value, ( templateProperty != null ) );
        }
        else if( Point.class.equals( propertyType ) )
        {
            XmlSerializationUtils.serializePoint( element, (Point)value, ( templateProperty != null ) );
        }
        else if( ColorFont.class.equals( propertyType ) )
        {
            XmlSerializationUtils.serializeFont( element, (ColorFont)value, ( templateProperty != null ) );
        }
        else if( propertyType.isArray() )
        {
            element.setAttribute( TYPE_ATTR, getPropertyType( Array.class ) );
            String type = getPropertyType( propertyType.getComponentType() );
            if( propertyType.getComponentType().getName().equals( DynamicProperty.class.getName() )
                    || propertyType.getComponentType().getName().equals( DynamicPropertySet.class.getName() ) )
            {
                type = getPropertyType( Composite.class );
            }
            element.setAttribute( ARRAY_ELEM_TYPE_ATTR, type );
            if( value instanceof DynamicProperty[] )
            {
                //create properties
                for( DynamicProperty property: (DynamicProperty[])value)
                    serializeDynamicProperty( doc, element, property, registry, writeIfDefault );
            }
            else if( value instanceof DynamicPropertySet[] )
            {
                //create properties
                for( DynamicPropertySet dps : (DynamicPropertySet[])value )
                {
                    serializeDynamicProperty(doc, element, new DynamicProperty(" ", DynamicPropertySet.class, dps), registry,
                            writeIfDefault);
                }
            }
            else
            {
                // create items
                if( value != null )
                {
                    for( int j = 0; j < java.lang.reflect.Array.getLength( value ); j++ )
                    {
                        Element itemElement = doc.createElement( ITEM_ELEMENT );
                        Object arrayElement = java.lang.reflect.Array.get( value, j );
                        Class<? extends Object> propertyClass = arrayElement.getClass();
                        if( String.class.equals( propertyClass ) || propertyClass.isPrimitive()
                                || XmlSerializationUtils.isPrimitiveWrapperElement( propertyClass ) )
                        {
                            itemElement.appendChild( doc.createTextNode( arrayElement.toString() ) );
                        }
                        else if( isEligibleToTextUtil(propertyClass))
                        {
                            itemElement.appendChild( doc.createTextNode( TextUtil2.toString(arrayElement) ) );
                        }
                        else
                        {
                            DynamicPropertySet dps = new DynamicPropertySetAsMap();
                            DPSUtils.writeBeanToDPS( arrayElement, dps, "" );
                            serializeDPS( doc, itemElement, dps, new DynamicPropertySetAsMap() );
                        }
                        element.appendChild( itemElement );
                    }
                }
            }
        }
        else if( PortOrientation.class.equals( propertyType ) )
        {
            serializePortOrientation( element, (PortOrientation)value, ( templateProperty != null ) );
        }
        else
        // it is composite
        {
            if( templateProperty == null )
            {
                element.setAttribute( TYPE_ATTR, "composite" );
            }
//            else
//            {
//                element.setAttribute( TYPE_ATTR, templateProperty.getType().toString() );
//            }

            DynamicPropertySet dps = new DynamicPropertySetAsMap();
            DynamicPropertySet nestedRegistry = new DynamicPropertySetAsMap();
            DynamicProperty property = registry == null ? null : registry.getProperty(name);

            if( value instanceof DynamicPropertySet )
            {
                dps = (DynamicPropertySet)value;
                nestedRegistry = property != null ? (DynamicPropertySet)property.getValue() : null;
            }
            else if( value != null )
            {
                DPSUtils.writeBeanToDPS(value, dps, "");
                element.setAttribute(TYPE_ATTR, getPropertyType(value.getClass()));
                if( property != null )
                    DPSUtils.writeBeanToDPS(property.getValue(), nestedRegistry, "");
            }
            serializeDPS( doc, element, dps, nestedRegistry, writeIfDefault  );
        }

        //properties attributes serializing
        if( dp.isHidden() )
            element.setAttribute( IS_HIDDEN_ATTR, "true" );
        if (dp.isReadOnly())
            element.setAttribute( IS_READONLY_ATTR, "true" );

        root.appendChild( element );
    }
    
    private static void serializePortOrientation(Element element, PortOrientation port, boolean isRef)
    {
        if( !isRef )
            element.setAttribute( TYPE_ATTR, "portOrientation" );
        if( port != null )
            element.setAttribute( VALUE_ATTR, port.toString() );
    }
}
