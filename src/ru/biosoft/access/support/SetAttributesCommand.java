package ru.biosoft.access.support;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSerializer;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.util.TextUtil2;

public class SetAttributesCommand implements TagCommand
{
    public static final int DEFAULT_IDENT = 4;

    protected static final Logger log = Logger.getLogger(SetPropertyCommand.class.getName());
    public static final String endl = System.getProperty("line.separator");

    private static final String tag = "AT";
    protected Method readMethod;

    protected TagEntryTransformer<?> transformer;

    protected StringBuffer value;

    protected DynamicPropertySetSerializer dpss;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    public SetAttributesCommand()
    {
        dpss = new DynamicPropertySetSerializer();
    }

    public void init(Method readMethod, TagEntryTransformer<?> transformer)
    {
        this.readMethod = readMethod;
        this.transformer = transformer;
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
        if( value.length() == 0 )
            return;

        Object obj = transformer.getProcessedObject();
        InputStream is = new ByteArrayInputStream(value.toString().getBytes());
        try
        {
            DynamicPropertySet dps = (DynamicPropertySet)readMethod.invoke(obj);
            cleanDPS(dps);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader());
            dpss.load(dps, is, ClassLoading.getClassLoader());
            Thread.currentThread().setContextClassLoader(cl);
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
        Object obj = transformer.getProcessedObject();
        StringBuilder taggedStr = new StringBuilder("");
        try( OutputStream os = new ByteArrayOutputStream() )
        {
            DynamicPropertySet dps = (DynamicPropertySet)readMethod.invoke(obj);
            dpss.save(os, dps);
            String value = os.toString();

            String tagStrWithIndent = tag + TextUtil2.whiteSpace(indent - tag.length());

            if( value != null && value.length() != 0 )
            {
                String line = null;
                try( BufferedReader reader = new BufferedReader(new StringReader(value)) )
                {
                    while( ( line = reader.readLine() ) != null )
                        taggedStr.append(tagStrWithIndent).append(line).append(endl);
                }
            }
            else
            {
                return tagStrWithIndent + endl;
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
        String tagStrWithIndent = tag + TextUtil2.whiteSpace(indent - tag.length());
        StringBuilder taggedStr = new StringBuilder();
        try
        {
            if( value != null && value.length() != 0 )
            {
                String line = null;
                try( BufferedReader reader = new BufferedReader(new StringReader(value)) )
                {
                    while( ( line = reader.readLine() ) != null )
                        taggedStr.append(tagStrWithIndent).append(line).append(endl);
                }
            }
            else
            {
                taggedStr.append(tagStrWithIndent).append(endl);
            }
        }
        catch( Exception exc )
        {
            log.log(Level.SEVERE, "Getting tagged value error", exc);
        }
        return taggedStr.toString();
    }

    protected void cleanDPS(DynamicPropertySet dps)
    {
        List<String> names = new ArrayList<>(dps.size());
        Iterator<String> iter = dps.nameIterator();
        while( iter.hasNext() )
            names.add( iter.next() );
        for(String name : names)
            dps.remove( name );
    }
}
