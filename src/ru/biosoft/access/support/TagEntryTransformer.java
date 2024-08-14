package ru.biosoft.access.support;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.Entry;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.access.core.DataElementReadException;

abstract public class TagEntryTransformer<O extends DataElement> extends AbstractTransformer<Entry, O>
{
    protected static final Logger log = Logger.getLogger(TagEntryTransformer.class.getName());
    public static final String endl = System.getProperty("line.separator");

    abstract public String getStartTag();
    abstract public String getEndTag();

    protected Map<String, TagCommand> commands = new HashMap<>();
    protected List<String> tagOrder = new LinkedList<>();

    protected Object processedObject;
    public O getProcessedObject()
    {
        return (O)processedObject;
    }

    public void addCommand(TagCommand command)
    {
        commands.put(command.getTag(), command);
        if( !tagOrder.contains(command.getTag()) )
            tagOrder.add(command.getTag());
    }

    public void removeCommand(String name)
    {
        commands.remove(name);
        tagOrder.remove(name);
    }

    /**
     * Tag indicates to stop parsing
     */
    protected String breakTag = null;
    public void setBreakTag(String breakTag)
    {
        this.breakTag = breakTag;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Transform input: entry -> some object
    //

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    synchronized public O transformInput(Entry input) throws Exception
    {
        Class<O> clazz = getOutputType();

        if( clazz.isInstance(input) )
            return (O)input;

        Constructor<O> constructor = null;
        try
        {
            constructor = clazz.getConstructor(DataCollection.class, String.class);
        }
        catch( NoSuchMethodException e )
        {
            constructor = clazz.getConstructor();
        }

        O de = constructor.newInstance(getTransformedCollection(), input.getName());

        readObject(de, input.getReader());

        return de;
    }

    private String delimitersPriority[] = {" ", "\t"};
    public void setDelimitersPriority(String newDelimitersPriority[])
    {
        delimitersPriority = newDelimitersPriority;
    }

    public void readObject(Object obj, Reader entryReader) throws Exception
    {
        if( entryReader == null )
            return;

        processedObject = obj;
        try (BufferedReader reader = entryReader instanceof BufferedReader ? (BufferedReader)entryReader
                : new BufferedReader( entryReader ))
        {
            String line = null;
            TagCommand command = null;
            boolean tagFlag;
            String value;
            boolean stopParse = false;
            while( ( line = reader.readLine() ) != null )
            {
                tagFlag = true;
                if( line.length() > 0 && ( line.charAt(0) == ' ' || line.charAt(0) == '\t' ) )
                    tagFlag = false;

                TagCommand nextCommand = command;

                if( !tagFlag )
                    value = line.trim();
                else
                {
                    int offset = StreamEx.of(delimitersPriority).mapToInt( line::indexOf ).without( -1 ).findFirst().orElse( -1 );

                    String tag = line;
                    if( 0 < offset )
                        tag = line.substring(0, offset);

                    if( tag.trim().startsWith("//") )
                    {
                        value = null;
                        nextCommand = null;
                    }
                    else if( ( breakTag != null ) && ( tag.startsWith(breakTag) ) )
                    {
                        value = line;
                        nextCommand = command;
                        stopParse = true;
                    }
                    else if( null == commands.get(tag) )
                    {
                        value = line;
                        nextCommand = command;
                    }
                    else
                    {
                        nextCommand = commands.get(tag);
                        value = "";
                        if( 0 < offset )
                            value = line.substring(offset + 1).trim();
                    }
                }
                if( nextCommand == command && command != null )
                {
                    command.addValue(value);
                }
                else
                {
                    if( command != null )
                    {
                        command.complete(command.getTag());
                    }
                    command = nextCommand;
                    if( command != null )
                    {
                        command.start(command.getTag());
                        command.addValue(value);
                    }
                }

                if( stopParse )
                {
                    break;
                }
            }

            if( command != null )
                command.complete(command.getTag());
        }
        finally
        {
            processedObject = null;
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    // Transform output: some object -> entry
    //


    protected Class<? extends DataElement> outputType;
    @Override
    public Class<O> getOutputType()
    {
        if( outputType == null )
        {
            Properties properties = getTransformedCollection().getInfo().getProperties();
            String name = properties.getProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
            if(name == null)
            {
                name = getTransformedCollection().getDataElementType().getName();
            }
            String plugins = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
            if( name != null )
            {
                try
                {
                    outputType = ClassLoading.loadSubClass( name, plugins, DataElement.class );
                }
                catch( LoggedClassNotFoundException e )
                {
                    throw new DataElementReadException(e, getTransformedCollection(), DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
                }
            }
            else
            {
                throw new DataElementReadException(getTransformedCollection(), DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
            }
        }

        return (Class<O>)outputType;
    }

    @Override
    synchronized public Entry transformOutput(O input) throws Exception
    {
        processedObject = input;

        String startTag = getStartTag();
        StringWriter data = new StringWriter();
        TagCommand command = commands.get(startTag);
        if( command == null )
            data.write(startTag + "  " + input.getName() + endl);
        else
            data.write(command.getTaggedValue(input.getName()));

        writeObject(input, data);

        data.write(getEndTag() + endl);
        return new Entry(input.getOrigin(), input.getName(), data.toString());
    }

    public void writeObject(Object obj, Writer writer) throws Exception
    {
        processedObject = obj;

        String startTag = getStartTag();
        TagCommand command;
        for(String name: tagOrder)
        {
            if( name.equals(startTag) )
                continue;

            command = commands.get(name);
            String str = command.getTaggedValue();
            if( str != null )
            {
                writer.write(str);
                if( !str.endsWith(endl) )
                    writer.write(endl);
            }
        }

        processedObject = null;
    }

}
