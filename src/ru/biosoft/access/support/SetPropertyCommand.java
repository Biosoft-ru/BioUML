package ru.biosoft.access.support;

import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Method;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.beans.Option;

public class SetPropertyCommand implements TagCommand
{
    public static final int DEFAULT_IDENT = 4;

    protected static final Logger log = Logger.getLogger(SetPropertyCommand.class.getName());
    public static final String endl = System.getProperty("line.separator");

    protected String tag;
    protected Class<?>  type;
    protected Method readMethod;
    protected Method writeMethod;

    protected TagEntryTransformer<?> transformer;

    protected boolean duplicateTags = true;

    protected StringBuffer value;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    public SetPropertyCommand(String tag, Class<?> type, Method readMethod, Method writeMethod,
                              TagEntryTransformer<?> transformer)
    {
        this(tag, type, readMethod, writeMethod, transformer, DEFAULT_IDENT, true);
    }

    public SetPropertyCommand(String tag, Class<?> type, Method readMethod, Method writeMethod,
                              TagEntryTransformer<?> transformer, int indent, boolean duplicateTags)
    {
        this.tag = tag;
        this.type        = type;
        this.readMethod  = readMethod;
        this.writeMethod = writeMethod;

        this.transformer = transformer;
        this.duplicateTags = duplicateTags;
        this.indent = indent;
    }

    public SetPropertyCommand(String tag, PropertyDescriptor descriptor, TagEntryTransformer<?> transformer)
    {
        this(tag, descriptor, transformer, DEFAULT_IDENT, true);
    }

    public SetPropertyCommand(String tag, PropertyDescriptor descriptor,
                              TagEntryTransformer<?> transformer, int indent, boolean duplicateTags)
    {
        this.tag = tag;

        this.type        = descriptor.getPropertyType();
        this.readMethod  = descriptor.getReadMethod();
        this.writeMethod = descriptor.getWriteMethod();

        this.transformer = transformer;
        this.duplicateTags = duplicateTags;
        this.indent = indent;
    }

    public SetPropertyCommand(String tag, Class<?> bean, Class<?> type, String readMethod, String writeMethod,
                              TagEntryTransformer<?> transformer)
    {
        this(tag, bean, type, readMethod, writeMethod, transformer, DEFAULT_IDENT, true);
    }

    public SetPropertyCommand(String tag, Class<?> bean, Class<?> type, String readMethod, String writeMethod,
                              TagEntryTransformer<?> transformer, int indent, boolean duplicateTags)
    {
        this.tag = tag;
        this.type = type;

        try
        {
            this.readMethod  = bean.getMethod(readMethod);
            this.writeMethod = bean.getMethod(writeMethod, type);
        }
        catch(Exception e)
        {
            log.log(Level.SEVERE, "Can not find read or write method for SetPropertyCommand\n" +
                      "  tag=" + tag +
                      "\n  bean=" + bean +
                      "\n  type=" + type +
                      "\n  readMethod=" +  readMethod +
                      "\n  writeMethod=" + writeMethod, e);
        }

        this.transformer = transformer;
        this.duplicateTags = duplicateTags;
        this.indent = indent;
    }


    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    protected int  indent = DEFAULT_IDENT;
    public int getIndent()
    {
        return indent;
    }
    public void setIndent(int indent)
    {
        this.indent = indent;
    }

    ////////////////////////////////////////////////////////////////////////////
    // TagCommand interface implementation
    //

    @Override
    public void start(String tag)
    {
        value = new StringBuffer();
    }

    @Override
    public void addValue(String appendValue)
    {
        if(appendValue == null || appendValue.length() == 0)
            return;

        if(value.length() > 0 )
            value.append(endl);

        value.append(appendValue);
    }

    @Override
    public void complete(String tag)
    {
        if( writeMethod==null )
            return;

        Object obj = transformer.processedObject;
        try
        {
            writeMethod.invoke(obj, stringToValue(value.toString()));
        }
        catch(Exception e)
        {
            log.log(Level.SEVERE, "Property parsing error: " + e
                       + "\n  tag=" + tag + ":\n  object=" + obj + ", class=" + obj.getClass(),  e);
        }
    }

    @Override
    public String getTag()
    {
        return tag;
    }

    @Override
    public String getTaggedValue()
    {
        StringBuilder taggedStr = new StringBuilder();
        try
        {
            Object obj = transformer.processedObject;
            String str = valueToString( readMethod.invoke(obj) );
            if( str == null  || str.length()==0 )
                return null;

            String tagStr = tag + TextUtil2.whiteSpace(Math.max(1, indent-tag.length()));
            String indentStr = TextUtil2.whiteSpace(indent);

            String line = null;
            BufferedReader reader = new BufferedReader( new StringReader(str) );
            while( (line=reader.readLine())!=null )
            {
                taggedStr.append(tagStr).append(line).append(endl);
                if( !duplicateTags )
                    tagStr = indentStr;
            }
        }
        catch( Exception exc )
        {
            log.log(Level.SEVERE, "Getting tagged value error", exc);
        }
        return taggedStr.toString();
    }

    @Override
    public String getTaggedValue( String value )
    {
        StringBuilder taggedStr = new StringBuilder();
        try
        {
            if( value!=null && value.length()!=0 )
            {
                String line = null;
                BufferedReader reader = new BufferedReader( new StringReader(value) );
                while( (line=reader.readLine())!=null )
                {
                    taggedStr.append(getTagStrWithIndent()).append(line).append(endl);
                }
            }
            else
            {
                return getTagStrWithIndent() + endl;
            }
        }
        catch( Exception exc )
        {
            log.log(Level.SEVERE, "Getting tagged value error", exc);
        }
        return taggedStr.toString();
    }

    ////////////////////////////////////////////////////////////////////////////

    protected Object stringToValue(String str)
    {
        if( type == String.class )
            return str;

        if( type == boolean.class )
            return str.equalsIgnoreCase("true");

        if( type == int.class )
            return Integer.valueOf(str);
        
        if( type == long.class )
            return Long.valueOf(str);

        if( type == float.class )
            return Float.valueOf(str);

        if( type == double.class )
            return Double.valueOf(str);
        
        if( type == ru.biosoft.access.core.DataElementPath.class)
            return DataElementPath.create(str);

        // create object that can be initialised by String value
        try
        {
            Class<?> t = type;
            if( t.isArray() )
                t = t.getComponentType();
            Object obj = t.getConstructor(String.class).newInstance(str);
            Object parent = transformer.processedObject;
            if( obj instanceof Option && parent instanceof Option )
                ((Option)obj).setParent((Option)parent);

            return obj;
        }
        catch(Throwable t)
        {
            log.log(Level.SEVERE, "Can not convert string value '" + str + "' to type " + type, t);
        }

        return null;
    }

    protected String valueToString(Object value)
    {
        if( value == null)
            return null;

        if( value instanceof SerializableAsText )
            return ((SerializableAsText)value).getAsText();

        return "" + value;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utilities
    //

    private String tagStrWithIndent = null;
    private String getTagStrWithIndent()
    {
        if( tagStrWithIndent==null )
        {
            tagStrWithIndent = tag + TextUtil2.whiteSpace(indent-tag.length());
        }
        return tagStrWithIndent;
    }
}
