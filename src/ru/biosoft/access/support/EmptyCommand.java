package ru.biosoft.access.support;

import ru.biosoft.access.support.TagCommand;

public class EmptyCommand implements TagCommand
{
    protected String tag;

    public EmptyCommand(String tag)
    {
        this.tag = tag;
    }

    @Override
    public void start(String tag)
    {
    }

    @Override
    public void addValue(String appendValue)
    {
    }

    @Override
    public void complete(String tag)
    {
    }

    @Override
    public String getTag()
    {
        return tag;
    }

    @Override
    public String getTaggedValue()
    {
        return "";
    }

    @Override
    public String getTaggedValue(String value)
    {
        return "";
    }
}