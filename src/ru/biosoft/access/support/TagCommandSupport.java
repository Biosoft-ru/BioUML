package ru.biosoft.access.support;

import ru.biosoft.access.core.DataElement;

public class TagCommandSupport<O extends DataElement> implements TagCommand
{
    protected TagEntryTransformer<? extends O> transformer;
    protected String tag;

    public TagCommandSupport(String tag, TagEntryTransformer<? extends O> transformer)
    {
        this.tag = tag;
        this.transformer = transformer;
    }

    @Override
    public void   start( String tag )             {}
    @Override
    public void   addValue( String value )        {}
    @Override
    public void   complete( String tag )          {}
    @Override
    public String getTag()                        { return tag; }
    @Override
    public String getTaggedValue()                { return ""; }
    @Override
    public String getTaggedValue(String value)    { return value; }
}