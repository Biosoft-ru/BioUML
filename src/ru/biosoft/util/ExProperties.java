package ru.biosoft.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.PluginEntry;
import ru.biosoft.util.entry.RegularFileEntry;

import com.developmentontheedge.application.Application;

/**
 * This class stores properties in more user-friendly way (though compatible with original format, thus load is not redefined)
 * @author lan
 */
public class ExProperties extends Properties
{
    public static void createConfigs(Map<String, Properties> propertySets, File root) throws IOException
    {
        for( Entry<String, Properties> configData : propertySets.entrySet() )
        {
            String fileName = configData.getKey();
            File file = new File(root, fileName);
            file.getParentFile().mkdirs();
            Properties properties = configData.getValue();
            store(properties, file);
        }
    }

    public static Map<String, Properties> createPropertySets(String[][][] propertyLists)
    {
        Map<String, Properties> result = new LinkedHashMap<>();
        for(String[][] propertyList: propertyLists)
        {
            Properties properties = new ExProperties();
            for(int i=1; i<propertyList.length; i++)
                properties.setProperty(propertyList[i][0], propertyList[i][1]);
            result.put(propertyList[0][0], properties);
        }
        return result;
    }
    
    public ExProperties()
    {
    }
    
    public ExProperties(File file) throws IOException
    {
        loadEntry( new RegularFileEntry( file ) );
    }
    
    public ExProperties(PluginEntry entry) throws IOException
    {
        loadEntry( entry );
    }
    
    private void loadEntry(PluginEntry entry) throws IOException
    {
        try(InputStream is = entry.getInputStream();
                InputStreamReader reader = new InputStreamReader( is, StandardCharsets.UTF_8 ))
        {
            load( reader );
        }
        String name = getProperty(DataCollectionConfigConstants.NAME_PROPERTY);
        if(name == null)
        {
            if(entry.getName().endsWith(DataCollectionConfigConstants.DEFAULT_NODE_CONFIG_SUFFIX))
            {
                name = entry.getName().substring(0, entry.getName().length()-DataCollectionConfigConstants.DEFAULT_NODE_CONFIG_SUFFIX.length());
            } else if(entry.getName().endsWith(DataCollectionConfigConstants.DEFAULT_FORMAT_CONFIG_SUFFIX))
            {
                name = entry.getName().substring(0, entry.getName().length()-DataCollectionConfigConstants.DEFAULT_FORMAT_CONFIG_SUFFIX.length());
            } else if(entry.getParent() != null)
            {
                name = entry.getParent().getName();
            }
            if(name != null)
            {
                setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
            }
        }
        String configPath = getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
        if ( configPath == null )
            setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, entry.getParent().toString());
    }
    
    /**
     * Replace $token$ with specified value in all String properties.
     * @param props All properties which will be parsed and replaced.
     * @param token token (without bound $) which should be replaced.
     * @param value value for replacing token.
     */
    static public void replaceToken( Properties props, String token, String value )
    {
        if( props==null )
            return;

        final String template = "$"+token+"$";
        for(Entry<Object, Object> entry : props.entrySet())
        {
            Object o = entry.getValue();
            if( !(o instanceof String) ) continue;
            String prop = (String)o;
            int pos = prop.indexOf(template);
            if( pos>0 )
            {
                prop = prop.substring(0,pos) +
                       value +
                       prop.substring(pos+template.length());
                entry.setValue( prop );
            }
        }
    }
    
    public static String getPluginsString(Properties properties, String newPlugins)
    {
        String oldPlugins = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
        return StreamEx.of( oldPlugins, newPlugins ).nonNull().flatMap( str -> StreamEx.split(str, ';') ).sorted().distinct()
                .joining( ";" );
    }
    
    public static String getPluginsString(Properties properties, Class<?> clazz)
    {
        return getPluginsString(properties, ClassLoading.getPluginForClass( clazz ));
    }
    
    public static void addPlugins(Properties properties, String newPlugins)
    {
        String pluginsString = getPluginsString(properties, newPlugins);
        if(TextUtil.isEmpty(pluginsString))
            properties.remove(DataCollectionConfigConstants.PLUGINS_PROPERTY);
        else
            properties.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, pluginsString);
    }
    
    public static void addPlugin(Properties properties, Class<?> clazz)
    {
        addPlugins(properties, ClassLoading.getPluginForClass( clazz ));
    }
    
    /**
     * Parse subproperties in format
     * {@code prefix.something=key;name1=value1;name2=value2...}
     * into {@code key => {name1 => value1, name2 => value2, ...}}
     * 
     * @param properties properties to extract subproperties from
     * @param prefix prefix to look for
     * @return
     */
    public static Map<String, Map<String, String>> getSubProperties(Properties properties, String prefix)
    {
        return EntryStream.of( properties )
            .mapKeys( Object::toString )
            .filterKeys( key -> key.startsWith( prefix+"." ) )
            .mapKeys( key -> key.substring( prefix.length()+1 ) )
            .mapValues( val -> Arrays.asList(TextUtil.split( val.toString(), ';' )) )
            .<Map<String, String>>mapValues( list -> StreamEx.of(list).map( str -> str.split( "=" ) )
                    .toMap(arr -> arr.length == 1 ? "default" : arr[0].trim(), arr -> arr[arr.length-1].trim()) )
            .toMap();
    }
    
    public Map<String, Map<String, String>> getSubProperties(String prefix)
    {
        return getSubProperties( this, prefix );
    }
    
    public static void store(Properties properties, File file) throws IOException
    {
        try(FileOutputStream out = new FileOutputStream(file); Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
        {
            properties.store(writer, "");
        }
    }

    @Override
    public void store(Writer writer, String comments) throws IOException
    {
        store0( ( writer instanceof BufferedWriter ) ? (BufferedWriter)writer : new BufferedWriter(writer), comments, false);
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException
    {
        store0(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)), comments, false);
    }

    private void store0(BufferedWriter bw, String comments, boolean escUnicode) throws IOException
    {
        bw.write( "#Created by " + Application.getGlobalValue("BioUML" ) + "\n" );
        if( comments != null && !comments.isEmpty() )
        {
            writeComments(bw, comments);
        }
        bw.newLine();
        synchronized( this )
        {
            Map<String, String> keyMap = new TreeMap<>();
            for( Enumeration e = keys(); e.hasMoreElements(); )
            {
                String keyStr = (String)e.nextElement();
                if(keyStr.equals(DataCollectionConfigConstants.NAME_PROPERTY)) keyMap.put("$$$$.1", keyStr);
                else if(keyStr.equals(DataCollectionConfigConstants.CLASS_PROPERTY)) keyMap.put("$$$$.2", keyStr);
                else if(keyStr.equals(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY)) keyMap.put("$$$$.3", keyStr);
                else if(keyStr.equals(DataCollectionConfigConstants.TRANSFORMER_CLASS)) keyMap.put("$$$$.4", keyStr);
                else keyMap.put(keyStr, keyStr);
            }
            
            String lastKeyId = "";
            for( Entry<String, String> entry: keyMap.entrySet() )
            {
                String keyId = entry.getKey();
                if(!lastKeyId.isEmpty())
                {
                    int pos = lastKeyId.indexOf('.');
                    String lastSection = pos > 0 ? lastKeyId.substring(0, pos) : "";
                    pos = keyId.indexOf('.');
                    String section = pos > 0 ? keyId.substring(0, pos) : "";
                    if(!section.equals(lastSection)) bw.newLine();
                }
                lastKeyId = keyId;
                String key = entry.getValue();
                String val = (String)get(key);
                key = saveConvert(key, true, escUnicode);
                /* No need to escape embedded and trailing spaces for value, hence
                 * pass false to flag.
                 */
                val = saveConvert(val, false, escUnicode);
                bw.write(key + "=" + val);
                bw.newLine();
            }
        }
        bw.flush();
    }
    
    // The following methods were copied from java.util.Properties class implementation
    // as they are declared as private and cannot be reused

    /*
     * Converts unicodes to encoded &#92;uxxxx and escapes
     * special characters with a preceding slash
     */
    private String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode)
    {
        int len = theString.length();
        int bufLen = len * 2;
        if( bufLen < 0 )
        {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuffer outBuffer = new StringBuffer(bufLen);
    
        for( int x = 0; x < len; x++ )
        {
            char aChar = theString.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if( ( aChar > 61 ) && ( aChar < 127 ) )
            {
                if( aChar == '\\' )
                {
                    outBuffer.append('\\');
                    outBuffer.append('\\');
                    continue;
                }
                outBuffer.append(aChar);
                continue;
            }
            switch( aChar )
            {
                case ' ':
                    if( x == 0 || escapeSpace )
                        outBuffer.append('\\');
                    outBuffer.append(' ');
                    break;
                case '\t':
                    outBuffer.append('\\');
                    outBuffer.append('t');
                    break;
                case '\n':
                    outBuffer.append('\\');
                    outBuffer.append('n');
                    break;
                case '\r':
                    outBuffer.append('\\');
                    outBuffer.append('r');
                    break;
                case '\f':
                    outBuffer.append('\\');
                    outBuffer.append('f');
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    outBuffer.append('\\');
                    outBuffer.append(aChar);
                    break;
                default:
                    if( ( ( aChar < 0x0020 ) || ( aChar > 0x007e ) ) && escapeUnicode )
                    {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex( ( aChar >> 12 ) & 0xF));
                        outBuffer.append(toHex( ( aChar >> 8 ) & 0xF));
                        outBuffer.append(toHex( ( aChar >> 4 ) & 0xF));
                        outBuffer.append(toHex(aChar & 0xF));
                    }
                    else
                    {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    /**
     * Convert a nibble to a hex character
     * @param   nibble  the nibble to convert.
     */
    private static char toHex(int nibble)
    {
        return hexDigit[ ( nibble & 0xF )];
    }

    /** A table of hex digits */
    private static final char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static void writeComments(BufferedWriter bw, String comments) throws IOException
    {
        bw.write("#");
        int len = comments.length();
        int current = 0;
        int last = 0;
        char[] uu = new char[6];
        uu[0] = '\\';
        uu[1] = 'u';
        while( current < len )
        {
            char c = comments.charAt(current);
            if( c > '\u00ff' || c == '\n' || c == '\r' )
            {
                if( last != current )
                    bw.write(comments.substring(last, current));
                if( c > '\u00ff' )
                {
                    uu[2] = toHex( ( c >> 12 ) & 0xf);
                    uu[3] = toHex( ( c >> 8 ) & 0xf);
                    uu[4] = toHex( ( c >> 4 ) & 0xf);
                    uu[5] = toHex(c & 0xf);
                    bw.write(new String(uu));
                }
                else
                {
                    bw.newLine();
                    if( c == '\r' && current != len - 1 && comments.charAt(current + 1) == '\n' )
                    {
                        current++;
                    }
                    if( current == len - 1 || ( comments.charAt(current + 1) != '#' && comments.charAt(current + 1) != '!' ) )
                        bw.write("#");
                }
                last = current + 1;
            }
            current++;
        }
        if( last != current )
            bw.write(comments.substring(last, current));
        bw.newLine();
    }
}
