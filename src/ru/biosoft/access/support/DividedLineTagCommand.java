package ru.biosoft.access.support;

import ru.biosoft.access.core.DataElement;

abstract public class DividedLineTagCommand<O extends DataElement> implements TagCommand
{
    protected String tag;
    protected TagEntryTransformer<? extends O> transformer;

    private StringBuffer line;

    public DividedLineTagCommand(String tag, TagEntryTransformer<? extends O> transformer)
    {
        this.tag = tag;
        this.transformer = transformer;
    }

    abstract public void addLine(String line);

    abstract public void startTag(String tag);

    abstract public void endTag(String tag);

    @Override
    public final void start(String tag)
    {
        line = new StringBuffer();
        startTag(tag);
    }

    @Override
    public void addValue(String value)
    {
        if( value == null || value.length() == 0 )
            return;

        if( value.startsWith("$") )
        {
            line.append(value.substring(1));
        }
        else
        {
            if( line.length() == 0 )
            {
                line.append(value);
            }
            else
            {
                addLine(line.toString());
                line = new StringBuffer(value);
            }
        }
    }

    @Override
    public final void complete(String tag)
    {
        if( line.length() > 0 )
        {
            addLine(line.toString());
        }
        endTag(tag);
    }

    @Override
    public String getTag()
    {
        return tag;
    }
}
