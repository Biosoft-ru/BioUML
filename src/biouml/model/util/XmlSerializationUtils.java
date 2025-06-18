package biouml.model.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;

public class XmlSerializationUtils extends DiagramXmlConstants
{
    protected static final Logger log = Logger.getLogger(XmlSerializationUtils.class.getName());

    public static Object getPrimitiveValue(Class<?> type, String s)
    {
        // order is important for performance, the most probable value first
        if( Integer.class.equals( type ) || int.class.equals( type ) )
        {
            return "".equals( s ) ? 0 : Integer.parseInt( s );
        }
        else if( Double.class.equals( type ) || double.class.equals( type ) )
        {
            return "".equals( s ) ? 0 : Double.parseDouble( s );
        }
        else if( Float.class.equals( type ) || float.class.equals( type ) )
        {
            return "".equals( s ) ? 0 : Float.parseFloat( s );
        }
        else if( Long.class.equals( type ) || long.class.equals( type ) )
        {
            return "".equals( s ) ? 0 : Long.parseLong( s );
        }
        else if( Boolean.class.equals( type ) || boolean.class.equals( type ) )
        {
            return "".equals( s ) ? false : Boolean.parseBoolean( s );
        }
        else if( Short.class.equals( type ) || short.class.equals( type ) )
        {
            return "".equals( s ) ? 0 : Short.parseShort( s );
        }
        else if( Character.class.equals( type ) || char.class.equals( type ) )
        {
            return "".equals( s ) ? '-' : s.charAt( 0 );
        }
        else if( Byte.class.equals( type ) || byte.class.equals( type ) )
        {
            return "".equals( s ) ? null : Byte.parseByte( s );
        }
        return null;
    }
    private static Set<Class<?>> wrappers = new HashSet<>( Arrays.asList( Integer.class, Float.class, Double.class, Long.class,
            Short.class, Boolean.class, Byte.class, Character.class ) );
    public static boolean isPrimitiveWrapperElement(Class<?> type)
    {
        return wrappers.contains( type );
    }

    /*
     * TODO: remove all deprecated fields and methods from this class.
     * For now they are here to provide compatibility with
     * earlier serialized filters arrays and layouters
     */
    @Deprecated
    public static Object deserialize(Element element)
    {
        if( !element.getNodeName().equals(OBJECT_ELEMENT) && !element.getNodeName().equals(ARRAY_ELEMENT) )
            return null;

        return readObject(element);
    }
    @Deprecated
    private static Object readObject(Element element)
    {
        String typeName = element.getAttribute(TYPE_ATTRIBUTE);
        Class<?> clazz = null;
        try
        {
            clazz = Class.forName(typeName);
        }
        catch( ClassNotFoundException | NoClassDefFoundError e )
        {
            try
            {
                clazz = ClassLoading.loadClass( typeName );
            }
            catch( LoggedClassNotFoundException e1 )
            {
                e1.log();
                return null;
            }
        }

        if( element.getNodeName().equals(ARRAY_ELEMENT) )
        {
            return readArray(element, clazz);
        }

        Object result = instantiate(clazz);

        if( result == null )
            return null;

        Element valueElement = DiagramXmlSupport.getElement(element, VALUE_ATTRIBUTE);

        // primitives - attributes of this node
        Field[] fields = getFields(clazz);
        for( Field field : fields )
            if( !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())
                    && ( field.getType().isPrimitive() || field.getType().equals(String.class)
                    || field.getType().equals(DataElementPath.class) || field.getType().equals(Color.class) ) )
            {
                String fieldValue = valueElement.getAttribute(field.getName());
                if( fieldValue != null && fieldValue.length() > 0 )
                    setFieldValue(result, field, fieldValue);
            }

        // all the other fields - children

        NodeList complexFieldsNodes = valueElement.getChildNodes();
        for(Element fieldElement : XmlUtil.elements(complexFieldsNodes))
        {
            Object fieldValue;
            if( fieldElement.getNodeName().equals(OBJECT_ELEMENT) || fieldElement.getNodeName().equals(ARRAY_ELEMENT) )
            {
                fieldValue = readObject(fieldElement);
            }
            else
            {
                Class<?> fieldType = getPrimitiveWrapperType(fieldElement.getNodeName());
                fieldValue = getPrimitiveWrapperValue(fieldType, fieldElement.getAttribute(VALUE_ATTRIBUTE));
            }
            String fieldName = fieldElement.getAttribute(NAME_ATTRIBUTE);
            Field field = getField(clazz, fieldName);
            if( field == null )
            {
                log.log(Level.SEVERE, "Could not find field named :" + fieldName + " for class :" + clazz.getCanonicalName());
                continue;
            }
            try
            {
                field.setAccessible(true);
                field.set(result, fieldValue);
            }
            catch( IllegalAccessException e)
            {
                //do nothing
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Could not set field", t);
            }
        }

        return result;
    }
    @Deprecated
    private static Object readArray(Element element, Class<?> clazz)
    {
        List<Object> result = new ArrayList<>();
        Element valueElement = DiagramXmlSupport.getElement(element, VALUE_ATTRIBUTE);
        for( String value : XmlStream.attributes( valueElement ).filterKeys( name -> name.startsWith( "val" ) ).values() )
        {
            try
            {
                result.add(clazz.getComponentType().getConstructor(String.class).newInstance(value));
            }
            catch( Exception e )
            {
            }
        }
        XmlStream.elements( valueElement ).map( XmlSerializationUtils::readObject ).nonNull().forEach( result::add );
        if( result.size() == 0 )
        {
            return null;
        }
        return result.toArray((Object[])Array.newInstance(clazz.getComponentType(), result.size()));
    }
    @Deprecated
    private static void setFieldValue(Object o, Field field, String fieldValue)
    {
        if( field == null )
            return;

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
            else if( fieldType.equals(DataElementPath.class) )
            {
                field.set(o, DataElementPath.create(fieldValue));
            }
            else if( fieldType.equals(Color.class) )
            {
                field.set(o, new Color(Integer.parseInt(fieldValue), true));
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Could not set field", t);
        }
    }
    @Deprecated
    private static Object instantiate(Class<?> clazz)
    {
        Object result = null;
        try
        {
            result = clazz.newInstance();
        }
        catch( Throwable t )
        {
            //log.log(Level.SEVERE, "Could not instantiate", t);
        }
        return result;
    }
    @Deprecated
    private static final String NAME_ATTRIBUTE = "n";
    @Deprecated
    public static final String OBJECT_ELEMENT = "o";
    @Deprecated
    public static final String ARRAY_ELEMENT = "a";
    @Deprecated
    private static final String TYPE_ATTRIBUTE = "t";
    @Deprecated
    private static final String VALUE_ATTRIBUTE = "v";
    @Deprecated
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
    @Deprecated
    private static Field[] getFields(Class<?> clazz)
    {
        List<Field> fields = getClassFields(clazz);
        return fields.toArray(new Field[fields.size()]);
    }
    @Deprecated
    private static List<Field> getClassFields(Class<?> clazz)
    {
        if( !clazz.isArray() && !clazz.isPrimitive() )
        {
            List<Field> fieldsList = StreamEx.of(clazz.getDeclaredFields()).toList();
            Class<?> superClass = clazz.getSuperclass();
            if( superClass != null )
                fieldsList.addAll(getClassFields(superClass));
            return fieldsList;
        }
        return new ArrayList<>();
    }
    @Deprecated
    private static final Map<Class<?>, String> primitiveWrapperXmlElements = new HashMap<>();
    @Deprecated
    private static final String INTEGER_WRAPPER_PRIMITIVE_ELEMENT = "wi";
    @Deprecated
    private static final String FLOAT_WRAPPER_PRIMITIVE_ELEMENT = "wf";
    @Deprecated
    private static final String DOUBLE_WRAPPER_PRIMITIVE_ELEMENT = "wd";
    @Deprecated
    private static final String LONG_WRAPPER_PRIMITIVE_ELEMENT = "wl";
    @Deprecated
    private static final String SHORT_WRAPPER_PRIMITIVE_ELEMENT = "ws";
    @Deprecated
    private static final String BOOLEAN_WRAPPER_PRIMITIVE_ELEMENT = "wb";
    @Deprecated
    private static final String BYTE_WRAPPER_PRIMITIVE_ELEMENT = "wy";
    @Deprecated
    private static final String CHARACTER_WRAPPER_PRIMITIVE_ELEMENT = "wc";
    static
    {
        primitiveWrapperXmlElements.put(Integer.class, INTEGER_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Float.class, FLOAT_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Double.class, DOUBLE_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Long.class, LONG_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Short.class, SHORT_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Boolean.class, BOOLEAN_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Character.class, CHARACTER_WRAPPER_PRIMITIVE_ELEMENT);
        primitiveWrapperXmlElements.put(Byte.class, BYTE_WRAPPER_PRIMITIVE_ELEMENT);
    }
    @Deprecated
    private static Class<?> getPrimitiveWrapperType(String name)
    {
        return StreamEx.ofKeys(primitiveWrapperXmlElements, name::equals).findAny().orElse( null );
    }
    @Deprecated
    private static Object getPrimitiveWrapperValue(Class<?> type, String s)
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

    /**
     * @param brush
     * @return String representation of Brush ready to use in {@link #readBrush(String)}
     */
    public static String getBrushString(Brush brush)
    {
        if( brush == null )
            return null;
        return ColorUtils.paintToString( brush.getPaint() );
    }
    public static Brush readBrush(String value)
    {
        String[] tokens = TextUtil2.split( value, ';' );
        if( tokens.length == 1 )
            return new Brush( ColorUtils.parsePaint( value ) );
        else if( tokens.length == 3 )
            return new Brush( new Color( Integer.parseInt( tokens[0] ), Integer.parseInt( tokens[1] ), Integer.parseInt( tokens[2] ) ) );
        else if( tokens.length == 6 )
            return new Brush( new Color( Integer.parseInt( tokens[0] ), Integer.parseInt( tokens[1] ), Integer.parseInt( tokens[2] ) ),
                    new Color( Integer.parseInt( tokens[3] ), Integer.parseInt( tokens[4] ), Integer.parseInt( tokens[5] ) ) );
        return null;
    }
    protected static void serializeBrush(Element element, Brush brush, boolean isRef)
    {
        if( !isRef )
        {
            element.setAttribute( TYPE_ATTR, "brush" );
        }
        String brushStr = getBrushString( brush );
        if( brushStr != null )
        {
            element.setAttribute( VALUE_ATTR, brushStr );
        }
    }

    /**
     * @param font
     * @return String representation of ColorFont ready to use in {@link #readFont(String)}
     */
    public static String getFontString(ColorFont font)
    {
        Font f = font.getFont();
        Color color = font.getColor();
        String string = f.getName() + ";" + f.getStyle() + ";" + f.getSize() + ";" + ColorUtils.colorToString( color );
        return string;
    }
    public static @CheckForNull ColorFont readFont(String value)
    {
        StringTokenizer st = new StringTokenizer( value, ";" );
        if( st.countTokens() == 6 )
        {
            return new ColorFont( new Font( st.nextToken(), Integer.parseInt( st.nextToken() ), Integer.parseInt( st.nextToken() ) ),
                    new Color( Integer.parseInt( st.nextToken() ), Integer.parseInt( st.nextToken() ), Integer.parseInt( st.nextToken() ) ) );
        }
        if( st.countTokens() == 4 )
        {
            return new ColorFont( new Font( st.nextToken(), Integer.parseInt( st.nextToken() ), Integer.parseInt( st.nextToken() ) ),
                    ColorUtils.parseColor( st.nextToken() ) );
        }
        return null;
    }
    protected static void serializeFont(Element element, ColorFont font, boolean isRef)
    {
        if( !isRef )
        {
            element.setAttribute( TYPE_ATTR, "font" );
        }
        if( font != null )
        {
            element.setAttribute( VALUE_ATTR, getFontString( font ) );
        }
    }

    /**
     * @param dimension
     * @return String representation of Dimension ready to use in {@link #readDimension(String)}
     */
    public static String getDimensionString(Dimension dimension)
    {
        return dimension.width + ";" + dimension.height;
    }
    public static Dimension readDimension(String value)
    {
        StringTokenizer st = new StringTokenizer( value, ";" );
        return st.countTokens() == 2 ? new Dimension( Integer.parseInt( st.nextToken() ), Integer.parseInt( st.nextToken() ) ) : null;
    }
    protected static void serializeDimension(Element element, Dimension dimension, boolean isRef)
    {
        if( !isRef )
        {
            element.setAttribute( TYPE_ATTR, "dimension" );
        }
        if( dimension != null )
        {
            element.setAttribute( VALUE_ATTR, getDimensionString( dimension ) );
        }
    }

    /**
     * @param pen
     * @return String representation of Pen ready to use in {@link #readPen(String)}
     */
    public static String getPenString(Pen pen)
    {
        Color color = pen.getColor();
        if( color == null )
            return String.valueOf( pen.getWidth() );
        return pen.getWidth() + ";" + ColorUtils.colorToString( color ) + ";" + pen.getStrokeAsString();
    }

    public static @Nonnull Pen readPen(String value)
    {
        if( value.isEmpty() )
            return new Pen();
        String[] tokens = TextUtil2.split(value, ';');
        Pen pen = new Pen(Float.parseFloat(tokens[0]));
        if( tokens.length > 1 )
        {
            if( tokens.length >= 4 )
            {
                if( tokens.length >= 5 )
                    pen.setColor(new Color(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]),
                            Integer.parseInt(tokens[4])));
                else
                    pen.setColor(new Color(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3])));
            }
            else
            {
                pen.setColor(ColorUtils.parseColor(tokens[1]));
            }
        }
        if( tokens.length == 3 )
        {
            pen.setStrokeAsString( tokens[2] );
        }
        return pen;
    }

    protected static void serializePen(Element element, Pen pen, boolean isRef)
    {
        if( !isRef )
        {
            element.setAttribute( TYPE_ATTR, "pen" );
        }
        if( pen != null )
        {
            element.setAttribute( VALUE_ATTR, getPenString( pen ) );
        }
    }

    /**
     * @param point
     * @return String representation of Point ready to use in {@link #readPoint(String)}
     */
    public static String getPointString(Point point)
    {
        return point.x + ";" + point.y;
    }
    public static Point readPoint(String str)
    {
        Point point = new Point();
        if( str != null && str.length() > 0 )
        {
            try
            {
                StringTokenizer tz = new StringTokenizer( str, "[] ,=xy;" );
                if( !str.contains( ";" ) )
                    tz.nextToken();
                int x = Integer.parseInt( tz.nextToken() );
                int y = Integer.parseInt( tz.nextToken() );
                point = new Point( x, y );
            }
            catch( NumberFormatException ex )
            {
            }
        }
        return point;
    }
    protected static void serializePoint(Element element, Point point, boolean isRef)
    {
        if( !isRef )
        {
            element.setAttribute( TYPE_ATTR, "point" );
        }
        if( point != null )
        {
            element.setAttribute( VALUE_ATTR, getPointString( point ) );
        }
    }
}
