package ru.biosoft.table.columnbeans;

import one.util.streamex.StreamEx;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

import com.developmentontheedge.beans.Option;

public class ColumnsEditor extends GenericMultiSelectEditor
{
    @Override
    protected Column[] getAvailableValues()
    {
        TableDataCollection tableDataCollection = null;
        try
        {
            tableDataCollection = (TableDataCollection)getBean().getClass().getMethod("getTable").invoke(getBean());
        }
        catch( Exception ex )
        {
        }
        
        Boolean useNumerical = false;
        try
        {
            useNumerical = (Boolean)getBean().getClass().getMethod("isNumerical").invoke(getBean());
        }
        catch( Exception e )
        {
        }
        StreamEx<Column> columns;
        if( tableDataCollection != null )
        {
            columns = tableDataCollection.columns()
                .filter( useNumerical ? c -> c.getType().isNumeric() : c -> true )
                .map( c -> new Column((Option)getBean(), c.getName()) );
        } else
        {
            columns = StreamEx.of();
        }
        Object useAllColumns = getDescriptor().getValue("useAllColumns");
        // Default is true
        if (useAllColumns == null || Boolean.parseBoolean(useAllColumns.toString()))
        {
            columns = columns.prepend( ColumnGroup.ALL_COLUMNS );
        }
        return columns.toArray( Column[]::new );
    }
    /**
     * Returns text describing current selection
     */
    @Override
    protected String getText(Object[] vals)
    {
        if( vals.length == 0 )
            return "not selected";
        try
        {
            TableDataCollection table = (TableDataCollection)getBean().getClass().getMethod("getTable", (Class<?>[])null).invoke(getBean(),
                    (Object[])null);
            String[] names = StreamEx.of(vals).select( Column.class ).map( Column::getName ).toArray( String[]::new );
            int[] indices = TableDataCollectionUtils.getColumnIndexes(table, names);
            return "Selected indices: " + TableDataCollectionUtils.getStringDescription(indices);
        }
        catch( Exception ex )
        {
            return super.getText(vals);
        }
    }
}
