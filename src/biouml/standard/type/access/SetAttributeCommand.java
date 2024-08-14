package biouml.standard.type.access;

import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Constructor;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.support.SerializableAsText;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.standard.type.BaseSupport;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

public class SetAttributeCommand implements TagCommand
{
    public static final int DEFAULT_IDENT = 12;

    protected static final Logger log = Logger.getLogger(SetAttributeCommand.class.getName());
    private static final String endl = System.getProperty("line.separator");

    protected String tag;
    protected String propertyName;
    protected Class<?> type;
    protected PropertyDescriptor descriptor;

    protected TagEntryTransformer<? extends BaseSupport> transformer;

    protected boolean duplicateTags = true;

    protected StringBuffer value;
    //protected String delimiter = endl;
    protected Object[] args = new Object[1];

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    public SetAttributeCommand(String tag, String propertyName, Class<?> type, TagEntryTransformer<? extends BaseSupport> transformer)
    {
        this.tag = tag;
        this.type = type;
        this.propertyName = propertyName;
        this.transformer = transformer;
        this.descriptor = StaticDescriptor.create(propertyName, propertyName.substring(0,1).toUpperCase()+propertyName.substring(1));
    }



    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    protected int indent = DEFAULT_IDENT;
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
        if( appendValue == null || appendValue.length() == 0 )
            return;

        if( value.length() > 0 )
            value.append(endl);

        value.append(appendValue);
    }

    @Override
    public void complete(String tag)
    {
        BaseSupport obj = transformer.getProcessedObject();
        try
        {
            DynamicProperty property = new DynamicProperty(descriptor, type, stringToValue(value.toString()));
            obj.getAttributes().add(property);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Property parsing error: " + e + "\n  tag=" + tag + ":\n  object=" + obj + ", class=" + obj.getClass(), e);
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

        BaseSupport obj = transformer.getProcessedObject();

        String str = valueToString(obj.getAttributes().getValue(propertyName));
        if( str == null || str.isEmpty() )
            return null;

        String tagStr = tag + TextUtil.whiteSpace(indent - tag.length());
        String indentStr = TextUtil.whiteSpace(indent);

        try (BufferedReader reader = new BufferedReader(new StringReader(str)))
        {
            String line = null;
            while( ( line = reader.readLine() ) != null )
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
    public String getTaggedValue(String value)
    {
        StringBuilder taggedStr = new StringBuilder();
        try
        {
            if( value != null && value.length() != 0 )
            {
                try( BufferedReader reader = new BufferedReader(new StringReader(value)) )
                {
                    String line = null;
                    while( ( line = reader.readLine() ) != null )
                        taggedStr.append(getTagStrWithIndent()).append(line).append(endl);
                }
            }
            else
            {
                taggedStr.append(getTagStrWithIndent()).append(endl);
            }
        }
        catch( Exception exc )
        {
            log.log(Level.SEVERE, "Getting tagged value error", exc);
        }
        return taggedStr.toString();
    }

    ////////////////////////////////////////////////////////////////////////////

    protected Object stringToValue(String str) throws Exception
    {
        if( type == String.class )
            return str;

        if( type == boolean.class )
            return str.equalsIgnoreCase("true");

        if( type == int.class )
            return Integer.valueOf(str);

        if( type == float.class )
            return Float.valueOf(str);

        if( type == double.class )
            return Double.valueOf(str);

        // create object that can be initialised by String value
        try
        {
            Class<?>[] params = {String.class};
            Class<?> t = type;
            if( t.isArray() )
                t = t.getComponentType();
            Constructor<?> c = t.getConstructor(params);
            args[0] = str;

            Object obj = c.newInstance(args);
            Object parent = transformer.getProcessedObject();
            if( obj instanceof Option && parent instanceof Option )
                ( (Option)obj ).setParent((Option)parent);

            return obj;
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not convert string value '" + str + "' to type " + type, t);
        }

        return null;
    }

    protected String valueToString(Object value)
    {
        if( value == null )
            return null;

        if( value instanceof SerializableAsText )
            return ( (SerializableAsText)value ).getAsText();

        return "" + value;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utilities
    //

    private String tagStrWithIndent = null;
    private String getTagStrWithIndent()
    {
        if( tagStrWithIndent == null )
            tagStrWithIndent = tag + TextUtil.whiteSpace(indent - tag.length());
        return tagStrWithIndent;
    }
}
