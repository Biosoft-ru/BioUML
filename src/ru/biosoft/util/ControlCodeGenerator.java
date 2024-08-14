package ru.biosoft.util;

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
}