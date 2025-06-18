package ru.biosoft.access.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.Vector;

import one.util.streamex.StreamEx;

import ru.biosoft.util.TextUtil2;

public class SetArrayPropertyCommand extends SetPropertyCommand
{
    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    public SetArrayPropertyCommand(String tag, Class<?> type, Method readMethod, Method writeMethod,
                              TagEntryTransformer<?> transformer)
    {
        super(tag, type, readMethod, writeMethod, transformer, DEFAULT_IDENT, true);
    }

    public SetArrayPropertyCommand(String tag, Class<?> type, Method readMethod, Method writeMethod,
                              TagEntryTransformer<?> transformer, int indent)
    {
        super(tag, type, readMethod, writeMethod, transformer, indent, true);
    }

    public SetArrayPropertyCommand(String tag, Class<?> type, Method readMethod, Method writeMethod,
                              TagEntryTransformer<?> transformer, int indent, boolean duplicateTags)
    {
        super(tag, type, readMethod, writeMethod, transformer, indent, duplicateTags);
    }

    public SetArrayPropertyCommand(String tag, PropertyDescriptor descriptor, TagEntryTransformer<?> transformer)
    {
        super(tag, descriptor, transformer, DEFAULT_IDENT, true);
    }

    public SetArrayPropertyCommand(String tag, PropertyDescriptor descriptor,
                              TagEntryTransformer<?> transformer, int indent, boolean duplicateTags)
    {
        super(tag, descriptor, transformer, indent, duplicateTags);
    }

    public SetArrayPropertyCommand(String tag, Class<?> bean, Class<?> type, String readMethod, String writeMethod,
                              TagEntryTransformer<?> transformer)
    {
        super(tag, bean, type, readMethod, writeMethod, transformer, DEFAULT_IDENT, true);
    }

    public SetArrayPropertyCommand(String tag, Class<?> bean, Class<?> type, String readMethod, String writeMethod,
                              TagEntryTransformer<?> transformer, int indent, boolean duplicateTags)
    {
        super(tag, bean, type, readMethod, writeMethod, transformer, indent, duplicateTags);
    }


    ////////////////////////////////////////////////////////////////////////////
    // TagCommand interface implementation
    //

    protected Vector<String> values;
    @Override
    public void start(String tag)
    {
        values = new Vector<>();
    }

    @Override
    public void addValue(String appendValue)
    {
        if(appendValue == null || appendValue.length() == 0)
            return;

        values.add(appendValue);
    }

    @Override
    public void complete(String tag)
    {
        if( writeMethod==null || values.isEmpty() )
            return;

        try
        {
            Object[] vals = StreamEx.of( values ).map( this::stringToValue )
                    .toArray( size -> (Object[])Array.newInstance( type.getComponentType(), size ) );
            Object obj = transformer.getProcessedObject();
            writeMethod.invoke(obj, new Object[] {vals});
        }
        catch(Exception e)
        {
            log.log(Level.SEVERE, "Property parsing error: " +
                      "  tag=" + tag + ", setProperty=" + writeMethod.getName() +
                      "\n  value=" + values, e);
        }
    }

    @Override
    public String getTaggedValue()
    {
        StringBuilder result = null;
        try
        {
            Object obj = transformer.getProcessedObject();
            obj = readMethod.invoke(obj);

            if( obj == null )
                return null;

            Object[] vals = (Object[])obj;
            if( vals.length == 0 )
                return null;

            String tagStr = getTag();
            String indentStr = TextUtil2.whiteSpace(indent);
            tagStr += TextUtil2.whiteSpace(Math.max(1, indent-tagStr.length()));

            result = new StringBuilder();
            for( Object val : vals )
            {
                result.append(tagStr);
                result.append( valueToString(val) );
                result.append(endl);

                if( !duplicateTags )
                    tagStr = indentStr;
            }
        }
        catch( Exception exc )
        {
            log.log(Level.SEVERE, "Getting tagged value error", exc);
        }

        if( result == null || result.length() == 0 )
            return null;

        return result.toString();
    }
}
