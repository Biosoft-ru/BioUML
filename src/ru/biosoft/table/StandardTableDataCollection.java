package ru.biosoft.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;

public class StandardTableDataCollection extends TableDataCollection
{
    protected static final Logger log = Logger.getLogger(StandardTableDataCollection.class.getName());

    /**
     * Map of values for each gene.
     *
     * Value can be: - gene/clone attributes - for example ID/AC from external
     * databases), - measurement - ratio, intensity, etc.
     */
    protected Map<String, RowDataElement> idToRow = new LinkedHashMap<>();
    protected List<String> rowIndexToId = new ArrayList<>();
    protected volatile boolean isRowIndexToIdUpdated = false;

    /**
     * Base constructor
     */
    public StandardTableDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
    }

    /**
     * Easy constructor
     */
    public StandardTableDataCollection(DataCollection<?> parent, String name)
    {
        this(parent, getProperties(name));
    }

    public static Properties getProperties(String name)
    {
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        return properties;
    }

    @Override
    public @Nonnull Iterator<RowDataElement> iterator()
    {
        return idToRow.values().iterator();
    }

    @Override
    public int getSize()
    {
        updateRowIndexToId();
        return rowIndexToId.size();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        updateRowIndexToId();
        return rowIndexToId;
    }

    @Override
    protected RowDataElement doGet(String name) throws Exception
    {
        return idToRow.get(name);
    }

    @Override
    protected void doPut(RowDataElement dataElement, boolean isNew) throws Exception
    {
        Object[] values = dataElement.getValues();
        TableColumn[] columns = getColumnModel().getColumns();
        for( int i = 0; i < values.length; i++ )
            values[i] = i < columns.length ? columns[i].getType().convertValue(values[i]) : null;
        idToRow.put(dataElement.getName(), dataElement);
        isRowIndexToIdUpdated = false;
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        idToRow.remove(name);
        isRowIndexToIdUpdated = false;
    }

    @Override
    public void close() throws Exception
    {
        super.close();
        idToRow.clear();
    }

    protected void updateRowIndexToId()
    {
        if( !isRowIndexToIdUpdated )
            synchronized( this )
            {
                if( !isRowIndexToIdUpdated )
                {
                    rowIndexToId = new ArrayList<>(idToRow.keySet());
                    isRowIndexToIdUpdated = true;
                }
            }
    }

    protected RowDataElement getRowElement(String key)
    {
        try
        {
            return get(key);
        }
        catch( Exception e )
        {
        }
        return null;
    }

    @Override
    public RowDataElement getAt(int rowIdx)
    {
        return getRowElement(getName(rowIdx));
    }

    @Override
    public Object getValueAt(int rowIdx, int columnIdx)
    {
        return getAt(rowIdx).getValues()[columnIdx];
    }

    @Override
    public void setValueAt(int rowIdx, int columnIdx, Object value)
    {
        RowDataElement row = getAt(rowIdx);
        Object oldValue = row.getValues(false)[columnIdx];
        row.getValues()[columnIdx] = value;
        firePropertyChange("values", oldValue, value);
    }

    @Override
    public String getName(int i)
    {
        updateRowIndexToId();
        
        return rowIndexToId.get(i);
    }

    @Override
    public void sortTable(int columnNumber, boolean dir)
    {
        updateRowIndexToId();

        final int ind = columnNumber;
        final int direction = dir ? 1 : -1;

        if( ind == -1 )
        {
            //sort by name
            if(Boolean.valueOf(getInfo().getProperties().getProperty(INTEGER_IDS, "false")))
            {
                Collections.sort(rowIndexToId, (str1, str2) -> direction * (Integer.parseInt(str1)-Integer.parseInt(str2)));
            } else
            {
                if(direction == 1) Collections.sort(rowIndexToId);
                else Collections.sort(rowIndexToId, Collections.reverseOrder());
            }
        }
        else
        {
            Collections.sort(rowIndexToId, (str1, str2) -> {
                RowDataElement rde1 = getRowElement(str1);
                RowDataElement rde2 = getRowElement(str2);
                if( rde1 == rde2 )
                {
                    return 0;
                }
                Object obj1 = rde1.getValues()[ind];
                Object obj2 = rde2.getValues()[ind];
                if( obj1 instanceof Comparable && obj2 instanceof Comparable )
                {
                    return direction * ( (Comparable<Object>)obj1 ).compareTo(obj2);
                }
                return 0;
            });
        }
        SortOrder oldSortOrder = sortOrder;
        sortOrder = new SortOrder(columnNumber, dir);
        getInfo().getProperties().setProperty(SORT_ORDER_PROPERTY, sortOrder.toString());
        firePropertyChange("sortOrder", oldSortOrder, sortOrder);
    }
}
