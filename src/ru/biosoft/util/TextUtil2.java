package ru.biosoft.util;

import java.awt.Color;
import java.beans.PropertyDescriptor;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang.StringEscapeUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.support.SerializableAsText;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.util.bean.JSONBean;

/**
 * Various utils for working with text date.
 *
 * @version 2.5.0, 21 February 2000
 * @author Fedor A. Kolpakov
 * 
 * Changed
 * @author anna
 */
public class TextUtil2
{
    public static final String SERIALIZABLE_PROPERTY = "serializableProperty";

    public static boolean isEmpty(String s)
    {
        return s == null || s.isEmpty();
    }
    
    public static boolean nonEmpty(String s)
    {
        return s != null && !s.isEmpty();
    }
    
    public static @Nonnull String nullToEmpty(@CheckForNull String s)
    {
        return s == null ? "" : s;
    }

    /**
     * Parse integer from the string.
     * String can contains leading and trailing spaces.
     *
     * @param s string that contains the integer
     * @param from start position in the string
     */
    public static int parseInt(String s, int from) throws NumberFormatException
    {
        if( s == null )
            throw new NumberFormatException("Util: null string");

        int result = 0;
        boolean negative = false;
        int i = from, max = s.length();
        if( max < i )
            throw new NumberFormatException("Util: empty substring");

        // cut liders spaces
        while( s.charAt(i) == ' ' && i < max )
            i++;

        if( s.charAt(i) == '-' )
        {
            negative = true;
            i++;
        }

        if( s.charAt(i) == '+' )
            i++;

        while( i < max )
        {
            int digit = Character.digit(s.charAt(i++), 10);
            if( digit < 0 )
                break;
            result = result * 10 + digit;
        }

        if( negative )
            return -result;
        return result;
    }

    ////////////////////////////////////////

    /**
     * Returns the specified section from the specified string.
     *
     * Section is substring between <section> and </section>.
     *
     * @param section section name
     * @param str the string containing this section
     * @param offset position in the string from which first section
     * will be found.
     */
    public static String getSection(String section, String str, int offset)
    {
        if( str != null )
        {
            String beg = '<' + section;
            String end = "</" + section;

            int start = str.indexOf(beg, offset);
            if( start != -1 )
            {
                start += beg.length();
                start = str.indexOf(">", start) + 1;
                int stop = str.indexOf(end, start);
                if( stop != -1 )
                    return str.substring(start, stop).trim();
                /*                else
                 // DEBUG

                 System.out.println( "_getSection: section has not end\n" +
                 "Section : "   + section + '\n' +
                 "source  : \n" + str);
                 */
            }
        }

        return null;
    }

    /**
     * Returns the specified section from the specified string.
     *
     * Section is substring between <section> and </section>.
     *
     * @param section section name
     * @param str the string containing this section
     */
    public static String getSection(String section, String str)
    {
        return getSection(section, str, 0);
    }

    /**
     * Returns specified field from the specified entry.
     *
     * <p>If field consists from several lines, they must be
     * a single block, without other fields inside them.
     *
     * @param entry entry text
     * @param field field (line) name
     * @param delimiter if the field consists from several lines
     * the specified character will be inserted as delimiter between lines.
     */
    public static String getField(String entry, String field, char delimiter)
    {
        int start = entry.indexOf(field);
        if( start == -1 )
            return null;
        if( start != 0 && entry.charAt(start - 1) != '\n' )
        {
            start = entry.indexOf("\n" + field, start);
            if( start == -1 )
                return null;
            start++;
        }

        int end;
        StringBuffer answer = new StringBuffer();
        String s;
        boolean isFirst = true;
        while( entry.startsWith(field, start) )
        {
            end = entry.indexOf('\n', start + field.length());
            s = ( entry.substring(start + field.length(), end) ).trim();

            // append delimiter (space) between lines
            if( isFirst )
                isFirst = false;
            else
                answer.append(delimiter);

            answer.append(s);
            start = end + 1;
        }

        return answer.toString();
    }

    /**
     * Returns specified field from the specified entry.<p>
     *
     * If the field consists from several lines space is used as delimiter.
     *
     * @param entry entry text
     * @param field field (line) name
     *
     * @see #getField(String entry, String field, char delimiter)
     */
    public static String getField(String entry, String field)
    {
        return getField(entry, field, ' ');
    }

    /**
     * Add the field to the specified string
     * if the field value is not empty.
     *
     * @param str string to which the field name and value
     * will be added.
     * @param fieldName field name.
     * @param fieldValue field value
     * @param maxStrLen the maximum field string length.
     * If the field value is longer then maxFieldLen,
     * it will be divided to several strings.
     */
    public static void addField(StringBuffer str, String fieldName, String fieldValue, int maxStrLen, String delimiter)
    {
        if( fieldValue != null )
        {
            fieldValue = fieldValue.trim();
            if( fieldValue.length() == 0 )
                return;

            StringTokenizer tokens = new StringTokenizer(fieldValue, " \t\r\n");
            StringBuffer value = new StringBuffer();
            while( tokens.hasMoreTokens() )
            {
                value.append(fieldName);
                value.append(delimiter);

                while( tokens.hasMoreTokens() )
                {
                    value.append(tokens.nextToken());
                    if( value.length() > maxStrLen )
                        break;
                }

                value.append("\r\n");
                str.append(value);
            }
        }
    }

    public static void addField(StringBuffer str, String fieldName, String fieldValue, int maxFieldLen)
    {
        addField(str, fieldName, fieldValue, maxFieldLen, "  ");
    }

    public static void addField(StringBuffer str, String fieldName, String fieldValue)
    {
        addField(str, fieldName, fieldValue, 65, "  ");
    }

    public static void addField(StringBuffer str, String fieldName, String fieldValue, String delimiter)
    {
        addField(str, fieldName, fieldValue, 65, delimiter);
    }

    public static boolean isFullPath(String path)
    {
        if( path.startsWith("databases/") || path.startsWith("data/") )
            return true;

        return false;
    }
    
    /**
     * Converts a windows wildcard pattern to a regex pattern
     *
     * @param wildcard - Wildcard pattern containing * and ?
     * @return - a regex pattern that is equivalent to the windows wildcard pattern
     */
    public static String wildcardToRegex(String wildcard)
    {
        if( wildcard == null )
            return null;

        StringBuilder buffer = new StringBuilder();

        char[] chars = wildcard.toCharArray();
        for(char c : chars)
        {
            if( c == '*' )
                buffer.append(".*");
            else if( c == '?' )
                buffer.append(".");
            else if( "+()^$.{}[]|\\".indexOf(c) != -1 )
                buffer.append('\\').append(c); // prefix all metacharacters with backslash
            else
                buffer.append(c);
        }

        return buffer.toString();
    }
    
    public static boolean isFloatingPointNumber(String str)
    {
        try
        {
            Double.parseDouble(str);
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    
    public static boolean isIntegerNumber(String str)
    {
        if(str.isEmpty())
            return false;
        int i = 0;
        char c = str.charAt( 0 );
        if(c == '-' || c == '+')
        {
            if(str.length() == 1)
                return false;
            i++;
        }
        for(;i < str.length();i++)
        {
            c = str.charAt( i );
            if(c < '0' || c > '9')
                return false;
        }
        return true;
    }
    
    /**
     * Changes first letter of the string to lower case if it's not abbreviation
     * @param source
     * @return
     */
    public static String toLower(String source)
    {
        if(source != null && source.length() >= 2 && Character.isUpperCase(source.charAt(0)) && Character.isLowerCase(source.charAt(1)))
        {
            return source.substring(0,1).toLowerCase()+source.substring(1);
        }
        return source;
    }
    
    /**
     * Serialize object to String for later deserialization via fromString
     * In worst case it calls toString of the object
     * TODO: check whether deserialization is possible for given object
     * Please note that class name is not serialized
     * @param obj object to serialize
     * @return
     */
    public static String toString(Object obj)
    {
        if(obj == null) return "";
        if(obj.getClass().isArray())
        {
            JsonArray result = toJSONArray(obj);
            return result.toString();
        }
        if(obj instanceof Class)
        {
            return ((Class<?>)obj).getName();
        }
        if(obj instanceof JSONBean)
        {
            JsonObject result = toJSONObject((JSONBean)obj);
            return result.toString();
        }
        if(obj instanceof SerializableAsText)
        {
            return ((SerializableAsText)obj).getAsText();
        }
        if( obj instanceof DynamicPropertySet )
            try
            {
                return writeDPSToJSON((DynamicPropertySet)obj);
            }
            catch( Exception e )
            {
                return null;
            }
        if( obj instanceof Color )
        {
            return ColorUtils.colorToString((Color)obj);
        }
        return obj.toString();
    }

    private static JsonArray toJSONArray(Object obj)
    {
        int length = Array.getLength(obj);
        JsonArray result = new JsonArray();
        for(int i=0; i<length; i++)
        {
            Object child = Array.get(obj, i);
            if(child == null)
                result.add( Json.NULL );
            else if(child.getClass().isArray())
                result.add(toJSONArray(child));
            else if(child instanceof JSONBean)
                result.add(toJSONObject((JSONBean)child));
            else result.add(toString(child));
        }
        return result;
    }
    
    private static boolean skipProperty(Property property)
    {
        if(!property.getDescriptor().isHidden())
            return false;
        Object value = property.getDescriptor().getValue( SERIALIZABLE_PROPERTY );
        if(value instanceof Boolean && ((Boolean)value))
            return false;
        return true;
    }
    
    private static JsonObject toJSONObject(JSONBean child)
    {
        return BeanUtil.properties( child ).remove( TextUtil2::skipProperty ).mapToEntry( Property::getName, property -> {
            Object value = property.getValue();
            if( value == null )
                return Json.NULL;
            if(value.getClass().isArray())
                return toJSONArray(value);
            else if(value instanceof JSONBean)
                return toJSONObject((JSONBean)value);
            else
                return Json.value( toString( value ) );
        }).collect( JsonUtils.toObject() );
    }

    /**
     * Instantiate object from String
     */
    public static Object fromString(Class<?> type, String valueStr)
    {
        return fromString( type, valueStr, false );
    }
    
    public static Object fromString(Class<?> type, String valueStr, boolean strict) throws ParseException
    {
        if(valueStr == null) return null;
        if(type.isArray())
        {
            try
            {
                JsonArray arr = Json.parse( valueStr ).asArray();
                Object result = fromJSONArray(type, arr, strict);
                return result;
            }
            catch(Exception e)
            {
                try
                {
                    Object fromString = fromString(type.getComponentType(), valueStr, strict);
                    if(fromString == null) return null;
                    Object result = Array.newInstance(type.getComponentType(), 1);
                    Array.set(result, 0, fromString);
                    return result;
                }
                catch( Exception e1 )
                {
                }
            }
            if(strict)
                throw new ParseException(valueStr, type);
            return null;
        }
        if(type.isEnum())
        {
            try
            {
                @SuppressWarnings ( {"unchecked", "rawtypes"} )
                Enum<?> value = Enum.valueOf( (Class<? extends Enum>)type, valueStr );
                return value;
            }
            catch( Exception e )
            {
                if(strict)
                    throw new ParseException(valueStr, type);
            }
        }
        if( DynamicPropertySet.class.isAssignableFrom(type) )
            try
            {
                return readDPSFromJSON(valueStr);
            }
            catch( Exception e )
            {
                if(strict)
                    throw new ParseException(valueStr, type);
                return null;
            }
        if(type.isPrimitive())
        {
            if(type.equals(double.class)) type = Double.class;
            if(type.equals(int.class)) type = Integer.class;
            if(type.equals(long.class)) type = Long.class;
            if(type.equals(boolean.class)) type = Boolean.class;
            if(type.equals(float.class)) type = Float.class;
        }
        if(Class.class.equals( type ))
        {
            try
            {
                return valueStr.isEmpty() ? null : ClassLoading.loadClass( valueStr );
            }
            catch( LoggedClassNotFoundException e )
            {
                if(strict)
                    throw new ParseException(valueStr, type);
                return null;
            }
        }
        if(JSONBean.class.isAssignableFrom(type))
        {
            try
            {
                JsonObject json = Json.parse( valueStr ).asObject();
                return fromJSONObject(type, json, strict);
            }
            catch(Exception e)
            {
                if(strict)
                    throw new ParseException(valueStr, type);
            }
        }
        if(Color.class.equals(type))
        {
            return ColorUtils.parseColor(valueStr);
        }
        try
        {
            Method method = type.getMethod("createInstance", String.class);
            return method.invoke(null, valueStr);
        }
        catch( Exception e1 )
        {
            try
            {
                return type.getConstructor(String.class).newInstance(valueStr);
            }
            catch( Exception e )
            {
            }
        }
        if(strict)
            throw new ParseException(valueStr, type);
        return null;
    }

    private static Object fromJSONArray(Class<?> type, JsonArray arr, boolean strict) throws Exception
    {
        Object result = Array.newInstance(type.getComponentType(), arr.size());
        for(int i=0; i<arr.size(); i++)
        {
            if(arr.get(i).isNull())
                continue;
            if( type.getComponentType().isArray() )
                Array.set(result, i, fromJSONArray(type.getComponentType(), arr.get(i).asArray(), strict));
            else if( JSONBean.class.isAssignableFrom(type.getComponentType()) )
                Array.set(result, i, fromJSONObject(type.getComponentType(), arr.get(i).asObject(), strict));
            else Array.set(result, i, fromString(type.getComponentType(), arr.get(i).asString(), strict));
        }
        return result;
    }

    private static Object fromJSONObject(Class<?> type, JsonObject json, boolean strict) throws Exception
    {
        Object bean = type.newInstance();
        ComponentModel model = ComponentFactory.getModel(bean, Policy.UI, true);
        for(Member m : json)
        {
            try
            {
                Property property = model.findProperty(m.getName());
                if(property == null) continue;
                if(m.getValue().isNull())
                    property.setValue( null );
                else if(JSONBean.class.isAssignableFrom(property.getValueClass()))
                    property.setValue(fromJSONObject(property.getValueClass(), m.getValue().asObject(), strict));
                else if(property.getValueClass().isArray())
                    property.setValue(fromJSONArray(property.getValueClass(), m.getValue().asArray(), strict));
                else
                    property.setValue(fromString(property.getValueClass(), m.getValue().asString(), strict));
            }
            catch( Exception e )
            {
                throw e;
            }
        }
        return bean;
    }

    public static DynamicPropertySet readDPSFromJSON(String json) throws Exception
    {
        DynamicPropertySet dps = new DynamicPropertySetSupport();
        JsonArray jsonDPS = Json.parse( json ).asArray();
        for( JsonValue val : jsonDPS )
        {
            JsonObject jsonDynamicProperty = val.asObject();

            Class<?> type = ClassLoading.loadClass( jsonDynamicProperty.get("type").asString() );
            Object value = TextUtil2.fromString(type, jsonDynamicProperty.get("value").asString());

            String name = jsonDynamicProperty.get("name").asString();
            PropertyDescriptor descriptor = BeanUtil.createDescriptor(name);
            descriptor.setBound(jsonDynamicProperty.getBoolean("bound", false));
            descriptor.setConstrained(jsonDynamicProperty.getBoolean("constrained", false));
            descriptor.setDisplayName(jsonDynamicProperty.getString("displayName", name));
            descriptor.setExpert(jsonDynamicProperty.getBoolean("expert", false));
            descriptor.setHidden(jsonDynamicProperty.getBoolean("hidden", false));
            descriptor.setPreferred(jsonDynamicProperty.getBoolean("preffered", false));
            
            Object editorClass = jsonDynamicProperty.get("propertyEditorClass");
            descriptor.setPropertyEditorClass(editorClass instanceof String ? ClassLoading.loadClass( (String)editorClass ) : null);
            
            descriptor.setShortDescription(jsonDynamicProperty.getString("shortDescription", ""));

            for(Member m : jsonDynamicProperty.get("attributes").asObject())
            {
                descriptor.setValue( m.getName(), m.getValue().toString() );
            }
            dps.add(new DynamicProperty(descriptor, type, value));
        }
        return dps;
    }

    public static String writeDPSToJSON(DynamicPropertySet dps)
    {
        JsonArray jsonDPS = new JsonArray();
        for( DynamicProperty dp : dps )
        {
            PropertyDescriptor descriptor = dp.getDescriptor();
            Class<?> editorClass = descriptor.getPropertyEditorClass();

            JsonObject attributes = new JsonObject();
            StreamEx.of(descriptor.attributeNames())
                .mapToEntry( name -> String.valueOf( descriptor.getValue( name ) ) )
                .forKeyValue( attributes::add );

            JsonObject jsonDynamicProperty = new JsonObject()
                .add("name", dp.getName())
                .add("type", dp.getType().getName())
                .add("value", TextUtil2.toString(dp.getValue()))
                .add("bound", descriptor.isBound())
                .add("constrained", descriptor.isConstrained())
                .add("displayName", descriptor.getDisplayName())
                .add("expert", descriptor.isExpert())
                .add("hidden", descriptor.isHidden())
                .add("preffered", descriptor.isPreferred())
                .add( "propertyEditorClass",
                            editorClass == null ? Json.NULL : Json.value( editorClass.getName() ) )
                .add("shortDescription", descriptor.getShortDescription())
                .add("attributes", attributes);

            jsonDPS.add(jsonDynamicProperty);
        }
        return jsonDPS.toString();
    }

    private static final Pattern nonWord = Pattern.compile( "[^\\w,$]" );

    /**
     * Inserts breaks (spaces) into string if it contains too long words without breaks.
     * @param str
     * @return
     */
    public static String insertBreaks(String str)
    {
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        while( pos < str.length() - 60 )
        {
            Matcher matcher = nonWord.matcher(str);
            int limitPos = pos + 60;
            if( !matcher.find(pos) || matcher.start() > limitPos )
            {
                String slice = str.substring( pos, limitPos );
                int len = IntStreamEx.ofChars( "_," ).map( ch -> slice.lastIndexOf( ch ) + 1 ).greater( 0 ).max()
                        .orElse( limitPos-pos );
                sb.append( str, pos, pos + len ).append( ' ' );
                pos += len;
            }
            else
            {
                //TODO: rework this
                int end = matcher.start() + 1;
                sb.append( str, pos, end );
                pos = end;
            }
        }
        return sb.length() == 0 ? str : sb.append( str.substring( pos ) ).toString();
    }

    /**
     * @param content
     * @return
     */
    public static String stripUnicodeMagic(String content)
    {
        if( !content.isEmpty() && ( content.charAt( 0 ) == 239 ) && ( content.charAt( 1 ) == 187 ) && ( content.charAt( 2 ) == 191 ) )
        {
            //HACK: magic remove unicode begin symbols
            content = content.substring(3);
        }
        return content;
    }
    
    /**
     * Works exactly like Pattern.compile(String.valueOf(delimiter), Pattern.LITERAL).split(string, -1),
     * or like org.apache.commons.lang.StringUtils.splitPreserveAllTokens but faster
     * benchmarks:
     * string: "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb,bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
     * Pattern.split: 3365 ns, StringUtils.splitPreserveAllTokens: 984 ns, this method: 703 ns
     * string: "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
     * Pattern.split: 890 ns, StringUtils.splitPreserveAllTokens: 344 ns, this method: 93 ns
     * string: "a,a,a,a,a,a,a,a,a,a,a,a,a,a"
     * Pattern.split: 3865 ns, StringUtils.splitPreserveAllTokens: 896 ns, this method: 688 ns
     * string: "aaaaa,bbbbb,ccccc"
     * Pattern.split: 1265 ns, StringUtils.splitPreserveAllTokens: 359 ns, this method: 219 ns
     * string: ",,,,,,,,,,"
     * Pattern.split: 2656 ns, StringUtils.splitPreserveAllTokens: 719 ns, this method: 500 ns
     * @param string
     * @param delimiter
     * @return
     */
    public static @Nonnull String[] split(String string, char delimiter)
    {
        int n = 1;
        int i = 0;
        while(true)
        {
            i=string.indexOf(delimiter, i);
            if(i == -1) break;
            n++;
            i++;
        }
        if(n == 1) return new String[] {string};
        
        String[] result = new String[n];
        n = 0;
        i = 0;
        int start = 0;
        while(true)
        {
            i = string.indexOf(delimiter, start);
            if(i == -1) break;
            result[n++] = string.substring(start, i);
            start = i+1;
        }
        result[n] = string.substring(start);
        return result;
    }
    
    /**
     * Splits a string into two substrings, trimming them
     * @param str input string
     * @param pos split position
     * @return array of two elements: the first is the string before pos, the second is the string after pos
     * Examples:
     * splitPos("BF  TNFalpha", 2) -> String[] {"BF", "TNFalpha"}
     * splitPos("BF", 2) -> String[] {"BF", ""}
     */
    public static String[] splitPos(String str, int pos)
    {
        return new String[] {str.substring(0, pos).trim(), str.substring(pos).trim()};
    }

    public static String calculateTemplate(String template, Object bean)
    {
        return calculateTemplate(template, bean, false);
    }

    public static String calculateTemplate(String template, Object bean, boolean ignoreErrors)
    {
        try
        {
            String[] fields = split(template, '$');
            ComponentModel model = ComponentFactory.getModel(bean);
            StringBuilder result = new StringBuilder();
            for( int i = 0; i < fields.length; i++ )
            {
                if( i % 2 == 0 )
                {
                    result.append(fields[i]);
                }
                else
                {
                    try
                    {
                        String[] components = split(fields[i], '/');
                        ComponentModel curModel = null;
                        Object curValue = null;
                        for( String propertyComponent : components )
                        {
                            if( curModel == null )
                                curModel = model;
                            else
                            {
                                if( curValue == null )
                                    return null;
                                curModel = ComponentFactory.getModel(curValue);
                            }
                            curValue = curModel.findProperty(propertyComponent).getValue();
                        }
                        result.append(curValue.toString());
                    }
                    catch( Exception e )
                    {
                        if(!ignoreErrors) return null;
                        result.append('$'+fields[i]+'$');
                    }
                }
            }
            return result.toString();
        }
        catch( Exception e )
        {
            return null;
        }
    }

    /**
     * Returns character copied specified number of times
     * @param base - character to copy
     * @param count - number of copies
     * @return string containing count copies of base or empty string if count <= 0
     * Example:
     * times('a', 2) -> 'aa';
     * times('b', -1) -> "";
     */
    public static @Nonnull String times(char base, int count)
    {
        if(count <= 0) return "";
        char[] result = new char[count];
        Arrays.fill( result, base );
        return new String(result);
    }
    
    public static @Nonnull String whiteSpace(int count)
    {
        return times(' ', count);
    }

    /**
     * interprets an Object as Stream of Strings
     * @param obj
     * @return Stream of Strings
     */
    public static StreamEx<String> stream(Object obj)
    {
        if(obj instanceof String[])
        {
            return StreamEx.of( (String[])obj );
        }
        if(obj instanceof String)
        {
            return StreamEx.of((String)obj);
        }
        return StreamEx.of();
    }
    
    public static String encodeURL(String src)
    {
        try
        {
            return URLEncoder.encode( src, "UTF-8" );
        }
        catch( UnsupportedEncodingException e )
        {
            throw new InternalException( e );
        }
    }
    
    public static String decodeURL(String src)
    {
        try
        {
            return URLDecoder.decode( src, "UTF-8" );
        }
        catch( UnsupportedEncodingException e )
        {
            throw new InternalException( e );
        }
    }

    public static String joinTruncate(Iterable<?> list, int maxLen, String delim, String truncatedSuffix)
    {
        if( maxLen <= 0 )
            throw new IllegalArgumentException();
        Iterator<?> it = list.iterator();
        if( !it.hasNext() )
            return "";
        StringBuilder sb = new StringBuilder( it.next().toString() );
        String truncated = null;
        while( it.hasNext() )
        {
            String s = it.next().toString();
            if( truncated == null
                    && sb.length() + ( delim.length() + s.length() ) + ( delim.length() + truncatedSuffix.length() ) > maxLen )
                truncated = sb.toString() + delim + truncatedSuffix;
            if( sb.length() + ( delim.length() + s.length() ) > maxLen )
                return truncated;
            sb.append( delim ).append( s );
        }
        return sb.toString();
    }
    
    public static class ParseException extends RuntimeException
    {
        public ParseException(String str, Class<?> type)
        {
            super( "Can't parse " + type.getName() + " from string: " + str );
        }
    }

    /**
     * Replace image paths in html for images to be loaded from plugin resources
    */
    public static String processHTMLImages(String html, String baseId)
    {
        Pattern pattern = Pattern.compile( "<img([^>]*) src=\"([^\"]+)\"" );
        Matcher matcher = pattern.matcher( html );
        int start = 0;
        while( matcher.find( start ) )
        {
            html = html.substring( 0, matcher.start() ) + "<img" + matcher.group( 1 ) + " src=\"../biouml/web/img?id="
                    + ( matcher.group( 2 ).contains( "://" ) ? "" : baseId )
                    + TextUtil2.encodeURL( StringEscapeUtils.unescapeHtml( matcher.group( 2 ) ) ) + "\"" + html.substring( matcher.end() );
            start = matcher.end();
            matcher = pattern.matcher( html );
        }
        return html;
    }

    /**
     * Substitute string "fromText" in "text" for string "toText".
     * Substituted text will be returned as a result.
     *
     * @param text text, where fromText is substituting for another text.
     * @param fromText text for substituting
     * @param toText text, that is substituting fromText
     * @return returns substituted text
     */
    public static String subst( String text, String fromText, String toText )
    {
        return subst( text, fromText, toText, "" );
    }

    /**
     * Substitute string "fromText" in "text" for another string.
     * Substitution string is "toText" or, if "toText" is empty (isEmpty), then "defText".
     * Substituted text will be returned as a result.
     *
     * @param text text, where fromText is substituting for another text.
     * @param fromText text for substituting
     * @param toText text, that is substituting fromText
     * @param defText text, that is substituting fromText, if "toText" is empty (isEmpty)
     * @return returns substituted text
     */
    public static String subst( String text, String fromText, String toText, String defText )
    {
        if( text == null )
        {
            return text;
        }
        int prevPos = 0;
        String newText = toText == null || "".equals( toText ) ? defText : toText;
        for( int pos = text.indexOf( fromText, prevPos ); pos >= 0;
             pos = text.indexOf( fromText, prevPos + newText.length() ) )
        {
            prevPos = pos;
            text = new StringBuffer( text ).replace( pos, pos + fromText.length(), newText ).toString();
        }
        return text;
    }

}
