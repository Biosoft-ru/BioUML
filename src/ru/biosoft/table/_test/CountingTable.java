package ru.biosoft.table._test;

import java.util.Iterator;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;

/**
 * In-memory table that counts complete passes over its rows and lookups of single rows by key, the two ways joins read tables
 */
public class CountingTable extends StandardTableDataCollection
{
    public int passes;
    public int lookups;

    public CountingTable(DataCollection<?> parent, String name)
    {
        super( parent, name );
    }

    public void resetCounts()
    {
        passes = 0;
        lookups = 0;
    }

    @Override
    public RowDataElement get(String name) throws Exception
    {
        lookups++;
        return super.get( name );
    }

    @Override
    public @Nonnull Iterator<RowDataElement> iterator()
    {
        Iterator<RowDataElement> rows = super.iterator();
        return new Iterator<RowDataElement>()
        {
            private boolean counted;

            @Override
            public boolean hasNext()
            {
                boolean hasNext = rows.hasNext();
                if( !hasNext && !counted )
                {
                    counted = true;
                    passes++;
                }
                return hasNext;
            }

            @Override
            public RowDataElement next()
            {
                return rows.next();
            }
        };
    }
}
