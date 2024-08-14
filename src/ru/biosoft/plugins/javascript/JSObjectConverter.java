package ru.biosoft.plugins.javascript;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * JSObjectConverter is used by JScriptContext to transform objects
 */
public interface JSObjectConverter
{
    /**
     * Return true if object can be converted by this converter
     */
    public boolean canConvert(Object object);
    /**
     * Converts Object to string
     */
    public String convertToString(Object object);
    /**
     * Converts object from to data collection element
     */
    public DataElement convertToDataElement(DataCollection parent, Object object);
}
