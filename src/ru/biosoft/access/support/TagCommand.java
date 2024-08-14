package ru.biosoft.access.support;

public interface TagCommand
{
    void start( String tag );
    void addValue( String value );
    void complete( String tag );
    String getTag();
    String getTaggedValue();
    String getTaggedValue(String value);
}