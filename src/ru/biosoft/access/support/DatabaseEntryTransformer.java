package ru.biosoft.access.support;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.Entry;

abstract public class DatabaseEntryTransformer extends AbstractTransformer<Entry, DatabaseEntry>
{
    protected static final Logger log = Logger.getLogger( DatabaseEntryTransformer.class.getName() );

    @Override
    public Class<? extends Entry> getInputType()
    {
        return Entry.class;
    }

    /** @pending todo */
    @Override
    public Entry transformOutput(DatabaseEntry dbEntry) throws Exception
    {
        String ln = System.getProperty("line.separator");

        StringBuffer data = new StringBuffer();

        Iterator<DynamicProperty> fields = dbEntry.propertyIterator();
        while( fields.hasNext() )
        {
            try
            {
                DatabaseField dbField = (DatabaseField)fields.next();
                String value = (String)dbField.getValue();
                if(value != null)
                {
                    data.append(dbField.getFieldTag());
                    data.append("  ");
                    data.append(value);
                    data.append(ln);
                }
            }
            catch (Throwable t)
            {
                log.log(Level.SEVERE, "Entry writing error: entry=" + dbEntry.getName(), t);
            }
        }

        data.append("//");
        data.append(ln);

        return new Entry(dbEntry.getOrigin(), dbEntry.getName(), data.toString());
    }
}
