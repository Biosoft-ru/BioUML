package ru.biosoft.table.columnbeans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import one.util.streamex.StreamEx;

import org.json.JSONArray;
import org.json.JSONException;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.CompositeProperty;

public class ColumnGroup extends Option implements PropertyChangeListener
{
    private static final long serialVersionUID = 1L;

    public final static String ALL_COLUMNS_STR = "(all columns)";
    public final static Column ALL_COLUMNS = new Column(null, ALL_COLUMNS_STR)
    {
        @Override
        public void addPropertyChangeListener(PropertyChangeListener l)
        {
        }
    };

    private Column[] columns = new Column[] {};
    private int[] columnIndices;
    private DataElementPath tablePath;
    //TODO: fix groups in table data collection
    //private String groupName;
    transient private TableDataCollection table;
    transient private boolean isNumerical;

    private static class ColumnComparator implements Comparator<Column>
    {
        @Override
        public int compare(Column col1, Column col2)
        {
            int priority1 = col1.equals(ALL_COLUMNS)?1:0;
            int priority2 = col2.equals(ALL_COLUMNS)?1:0;
            return priority1 != priority2 ? priority2-priority1 : col1.compareTo(col2);
        }
    }
    private static ColumnComparator columnComparator = new ColumnComparator();

    public ColumnGroup()
    {
        this(null);
    }

    public ColumnGroup(Option parent)
    {
        this(parent, new String[0]);
    }

    public ColumnGroup(Option parent, String[] names)
    {
        this(parent, names, null);
    }

    public ColumnGroup(Option parent, DataElementPath path)
    {
        this(parent, TableDataCollectionUtils.getColumnNames(path.getDataElement(TableDataCollection.class)), path);
    }

    public ColumnGroup(Option parent, String[] names, DataElementPath path)
    {
        this(parent, names, new double[names.length], path);
    }

    public boolean isNumerical()
    {
        return isNumerical;
    }

    public void setNumerical(boolean numerical)
    {
        boolean oldValue = isNumerical;
        isNumerical = numerical;
        firePropertyChange("isNumerical", oldValue, numerical);
    }

    public ColumnGroup(Option parent, String[] names, double[] timePoints, DataElementPath path)
    {
        super(parent);
        Column[] columns;
        if( names == null || names.length == 0 )
        {
            columns = new Column[] {ColumnGroup.ALL_COLUMNS};
        }
        else
        {
            columns = new Column[names.length];
            for( int i = 0; i < columns.length; i++ )
            {
                columns[i] = new Column(this, names[i], timePoints[i]);
            }
        }
        Arrays.sort(columns, columnComparator);
        setColumns(columns);
        if( parent instanceof PropertyChangeListener )
            this.addPropertyChangeListener((PropertyChangeListener)parent);

        try
        {
            table = path.getDataElement(TableDataCollection.class);
            tablePath = path;
            calculateIndices();
        }
        catch( Exception ex )
        {

        }
    }

    public Boolean canBeNull()
    {
        if( getParent() == null )
            return false;
        CompositeProperty model = ComponentFactory.getModel(getParent());
        for( int i = 0; i < model.getPropertyCount(); i++ )
        {
            if( model.getPropertyAt(i).getValue() == this )
            {
                return model.getPropertyAt(i).getBooleanAttribute(BeanInfoConstants.CAN_BE_NULL);
            }
        }
        return false;
    }

    public DataElementPath getTablePath()
    {
        return tablePath;
    }

    public void setTablePath(DataElementPath tablePath)
    {
        DataElementPath oldValue = this.tablePath;
        if( tablePath != null )
        {
            DataElement de = tablePath.optDataElement();
            if(de instanceof TableDataCollection)
                this.table = (TableDataCollection)de;
        }
        this.tablePath = tablePath;
        Column[] newColumns = new Column[] {ALL_COLUMNS};

        if( table != null )
        {
            if( checkAllColumnsSelected(columns) )
            {
                newColumns = getAllColumnsFromTable();
            }
            // Preserve already selected columns if names are equal
            else
            {
                newColumns = StreamEx.of( getColumns() ).filter( column -> this.table.getColumnModel().hasColumn( column.getName() ) )
                        .toArray( Column[]::new );
            }
        }
        doSetColumns(newColumns);
        firePropertyChange("table", oldValue, tablePath);
    }

    public boolean checkAllColumnsSelected(Column[] columns)
    {
        for( Column column : columns )
        {
            if( column.equals(ALL_COLUMNS) )
                return true;
        }
        return false;
    }

    public Column[] getAllColumnsFromTable()
    {
        if( table == null )
            return new Column[] {ALL_COLUMNS};
        return table.columns()
                .filter( isNumerical ? c -> c.getType().isNumeric() : c -> true )
                .map( c -> new Column( this, c.getName() ) )
                .prepend( ALL_COLUMNS ).toArray( Column[]::new );
    }

    public void setAllColumnsFromTable()
    {
        setColumns(new Column[]{ALL_COLUMNS});
    }

    public TableDataCollection getTable()
    {
        if( tablePath == null )
        {
            return null;
        }
        if( table == null )
        {
            table = tablePath.getDataElement(TableDataCollection.class);
        }
        return table;
    }

    public void setTable(TableDataCollection newTable)
    {
        setTablePath(DataElementPath.create(newTable));
    }

    protected void doSetColumns(Column[] newColumns)
    {
        Arrays.sort(newColumns, columnComparator);
        Column[] oldValue = columns.clone();
        columns = newColumns;
        for( Column column : columns )
        {
            column.removePropertyChangeListener(this);
            column.addPropertyChangeListener(this);
        }
        firePropertyChange("columns", oldValue, columns);
    }

    public void setColumns(Column[] str)
    {
        if( checkAllColumnsSelected(str) )
        {
            str = getAllColumnsFromTable();
        }
        doSetColumns(str);
        firePropertyChange("*", null, null);
        firePropertyChange("columns", null, null);
    }

    public Column[] getColumnsForEditor()
    {
        return columns;
    }

    public Column[] getColumns()
    {
        if( checkAllColumnsSelected(columns) )
        {
            if( table != null && columns.length == 1 )
                columns = getAllColumnsFromTable();
            Column[] c = new Column[columns.length - 1];
            System.arraycopy(columns, 1, c, 0, c.length);
            return c;
        }
        return columns;
    }

    public String[] getNames()
    {
        Column[] columns = getColumns();
        return StreamEx.of(columns).map( Column::getName ).toArray( String[]::new );
    }
    public String[] getNewNames()
    {
        Column[] columns = getColumns();
        return StreamEx.of(columns).map( Column::getNewName ).toArray( String[]::new );
    }

    //utility method for data retrieving
    public double[] getTimePoints()
    {
        return StreamEx.of(getColumns()).mapToDouble( Column::getTimePoint ).toArray();
    }

    public String getNamesDescription()
    {
        if( columns == null )
            return "";
        return StreamEx.of(columns).map( Column::getName ).joining( "," );
    }
    public String getNewNamesDescription()
    {
        if( columns == null )
            return "";
        return StreamEx.of(columns).map( Column::getNewName ).joining( "," );
    }

    public String getTimePointsDescription()
    {
        if( columns == null )
            return "";
        return StreamEx.of(columns).map( Column::getTimePoint ).joining( "," );
    }

    public String calcColumnName(Integer index, Object column)
    {
        return ( (Column)column ).getName();
    }

    /**
     * Convert object to string
     */
    @Override
    public String toString()
    {
        JSONArray array = new JSONArray();
        array.put(tablePath == null?"":tablePath);
        for( Column column : columns )
        {
            JSONArray columnArray = new JSONArray();
            columnArray.put(column.getName());
            if(!column.getNewName().equals(column.getName()))
                columnArray.put( column.getNewName() );
            try
            {
                columnArray.put(column.getTimePoint());
            }
            catch( JSONException e )
            {
                columnArray.put("0.0");
            }
            array.put(columnArray);
        }
        return array.toString();
    }

    public static ColumnGroup createInstance(String serialized)
    {
        return readObject(null, serialized);
    }

    /**
     * Restore object from string
     */
    public static ColumnGroup readObject(Option parent, String str)
    {
        if( str == null )
            return null;
        try
        {
            JSONArray array = new JSONArray(str);
            ColumnGroup result = new ColumnGroup(parent);
            result.setTablePath(DataElementPath.create(array.optString(0)));
            List<Column> columns = new ArrayList<>();
            for(int i=1;i<array.length();i++)
            {
                JSONArray columnArray = array.optJSONArray(i);
                if(columnArray == null) continue;
                Column column = columnArray.length() >= 3?new Column(result, columnArray.optString(0), columnArray.optString(1), columnArray.optDouble(2)):
                    new Column(result, columnArray.optString(0), columnArray.optString(0), columnArray.optDouble(1));
                columns.add(column);
            }
            result.setColumns(columns.toArray(new Column[columns.size()]));
            return result;
        }
        catch(JSONException e)
        {
            // Old format
            String[] elements = TextUtil2.split( str, ';' );
            ColumnGroup result = new ColumnGroup(parent);
            if( elements.length > 0 )
            {
                String path = elements[0];
                DataElementPath table = DataElementPath.create(path);
                if( table != null )
                {
                    result.setTablePath(table);
                }
            }
            List<Column> columns = new ArrayList<>();
            for( int i = 1; i < elements.length; i++ )
            {
                String[] cParams = TextUtil2.split( elements[i], ':' );
                if( cParams.length == 3 )
                {
                    Column column = new Column(result, cParams[0], cParams[1], Column.generateTimePoint(cParams[2]));
                    columns.add(column);
                }
            }
            result.setColumns(columns.toArray(new Column[columns.size()]));
            return result;
        }
    }

    protected void resetColumns()
    {
        setColumns(columns.clone());
    }

    public void calculateIndices()
    {
        columnIndices = TableDataCollectionUtils.getColumnIndexes(table, this.getNames());
    }

    //Method for easy access to data
    public double[] getDoubleRow(String rowName)
    {
        if( columnIndices == null )
            calculateIndices();
        return TableDataCollectionUtils.getDoubleRow(table, columnIndices, rowName);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        if( event.getSource() instanceof Column )
        {
            for( int i = 0; i < columns.length; i++ )
            {
                if( ( (Column)event.getSource() ).getName().equals(columns[i].getName()) )
                {
                    columns[i] = (Column)event.getSource();
                    break;
                }
            }
        }
    }

    public ColumnGroup clone(Option parent)
    {
        ColumnGroup result = new ColumnGroup(parent);
        result.setTable(table);
        result.setColumns(this.columns.clone());
        return result;
    }
}