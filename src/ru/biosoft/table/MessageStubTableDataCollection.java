package ru.biosoft.table;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollectionConfigConstants;


/**
 * TableDataCollection containing one row with given message
 * Useful to report errors where table is required
 * @author lan
 */
public class MessageStubTableDataCollection extends TableDataCollection
{
    private static final Properties DEFAULT_PROPERTIES = new Properties() {{
        setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "");
    }};
    
    private RowDataElement row;
    
    public MessageStubTableDataCollection(String message)
    {
        super(null, DEFAULT_PROPERTIES);
        row = new RowDataElement(message, this);
    }

    @Override
    public void sortTable(int columnNumber, boolean dir)
    {
    }

    @Override
    public RowDataElement getAt(int rowIdx)
    {
        return rowIdx == 0 ? row : null;
    }

    @Override
    public Object getValueAt(int rowIdx, int columnIdx)
    {
        return null;
    }

    @Override
    public void setValueAt(int rowIdx, int columnIdx, Object value)
    {
    }

    @Override
    public String getName(int rowIdx)
    {
        return rowIdx == 0 ? row.getName() : null;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    @Nonnull
    public List<String> getNameList()
    {
        return Collections.singletonList(row.getName());
    }

    @Override
    public Iterator<RowDataElement> getSortedIterator(String field, boolean direction, int from, int to)
    {
        return Collections.singleton( row ).iterator();
    }
}
