package ru.biosoft.util;

import java.util.Properties;

import ru.biosoft.util.j2html.tags.Tag;

/**
 * Web table cell code generator interface for processing plugin-specific item types
 * TODO: Move to proper place
 *
 */

public interface ControlCodeGenerator
{
    Tag<?> getControlCode(Object value) throws Exception;
    Class<?> getSupportedItemType();

    default boolean needProperties()
    {
        return false;
    }

    default Tag<?> getControlCode(Object value, Properties properties) throws Exception
    {
        return getControlCode( value );
    }

    default boolean isApplicable(Properties properties)
    {
        return false;
    }
}