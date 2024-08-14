package ru.biosoft.access;

import java.util.LinkedHashMap;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * Vector data collection ordered in addition order
 * @author lan
 */
public class LinkedVectorDataCollection<T extends DataElement> extends VectorDataCollection<T>
{
    public LinkedVectorDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        elements = new LinkedHashMap<>();
    }

    public LinkedVectorDataCollection(String name, DataCollection<?> parent, Properties properties)
    {
        super(name, parent, properties);
        elements = new LinkedHashMap<>();
    }
}
