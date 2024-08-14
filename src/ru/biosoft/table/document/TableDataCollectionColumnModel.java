package ru.biosoft.table.document;

import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.swing.table.Column;
import com.developmentontheedge.beans.swing.table.ColumnWithSort;
import com.developmentontheedge.beans.swing.table.SortedColumnModel;

public class TableDataCollectionColumnModel extends SortedColumnModel
{
    TableDataCollection source;

    public TableDataCollectionColumnModel(TableDataCollection source, Column[] fields)
    {
        super(fields);
        this.source = source;
    }

    @Override
    public void sort()
    {
        Column[] columns = getColumns();
        for( int i = 0; i < columns.length; i++ )
        {
            ColumnWithSort col = (ColumnWithSort)columns[i];
            if( col.getSorting() != ColumnWithSort.SORTING_NONE )
            {
                source.sortTable(i - 1, col.getSorting() == ColumnWithSort.SORTING_ASCENT );
                break;
            }
        }
    }
}
