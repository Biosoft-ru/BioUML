package ru.biosoft.access.support;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.MutableDataElement;

import com.developmentontheedge.beans.DynamicPropertySetSupport;

/**
 * General class allowing to present (wrap) the database {@link Entry} as
 * {@link DynamicPropertySetSupport}
 */
public class DatabaseEntry extends DynamicPropertySetSupport implements MutableDataElement
{
    public DatabaseEntry(DataCollection origin, String name, Entry entry)
    {
        this.origin = origin;
        this.name   = name;
        this.entry  = entry;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    private Entry entry;

    /** @returns the database entry wrapped by <code>DatabaseFieldSet</code>. */
    public Entry getEntry()
    {
        return entry;
    }

    ////////////////////////////////////////////////////////////////////////////
    // MutableDataElement interface implementation
    //

    private String name;
    @Override
    public String getName()
    {
        return name;
    }

    private DataCollection origin;
    @Override
    public DataCollection getOrigin()
    {
        return origin;
    }
}
