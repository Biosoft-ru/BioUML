package ru.biosoft.table._test;

import javax.swing.table.TableModel;

import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.swing.table.Column;
import com.developmentontheedge.beans.swing.table.ColumnModel;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.table.ColumnEx;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.BiosoftTableModel;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class TableModelTest extends TestCase
{
    public void testSortableModel() throws Exception
    {
        TableDataCollection table = new StandardTableDataCollection(null, "test");
        table.getColumnModel().addColumn("text", String.class);
        table.getColumnModel().addColumn("int", Integer.class);
        TableDataCollectionUtils.addRow(table, "rowA", new Object[] {"2", 2});
        TableDataCollectionUtils.addRow(table, "rowB", new Object[] {"10", 10});
        TableDataCollectionUtils.addRow(table, "rowC", new Object[] {"0", 0});
        
        ColumnModel columnModel = table.getColumnModel().getSwingColumnModel();
        BiosoftTableModel model = TableDataCollectionUtils.getTableModel(table, columnModel);
        assertTrue(model.isSortingSupported());
        assertEquals(3, model.getRowCount());
        assertEquals(3, model.getColumnCount());
        
        sort(columnModel, model, 0, ColumnEx.SORTING_DESCENT);
        assertEquals("rowC", ((Property)model.getValueAt(0, 0)).getValue());
        assertEquals("rowB", ((Property)model.getValueAt(1, 0)).getValue());
        assertEquals("rowA", ((Property)model.getValueAt(2, 0)).getValue());
        assertEquals("0", ((Property)model.getValueAt(0, 1)).getValue());
        assertEquals("10", ((Property)model.getValueAt(1, 1)).getValue());
        assertEquals("2", ((Property)model.getValueAt(2, 1)).getValue());
        assertEquals(0, ((Property)model.getValueAt(0, 2)).getValue());
        assertEquals(10, ((Property)model.getValueAt(1, 2)).getValue());
        assertEquals(2, ((Property)model.getValueAt(2, 2)).getValue());
        
        sort(columnModel, model, 1, ColumnEx.SORTING_DESCENT);
        assertEquals("rowA", ((Property)model.getValueAt(0, 0)).getValue());
        assertEquals("rowB", ((Property)model.getValueAt(1, 0)).getValue());
        assertEquals("rowC", ((Property)model.getValueAt(2, 0)).getValue());

        sort(columnModel, model, 2, ColumnEx.SORTING_ASCENT);
        assertEquals("rowC", ((Property)model.getValueAt(0, 0)).getValue());
        assertEquals("rowA", ((Property)model.getValueAt(1, 0)).getValue());
        assertEquals("rowB", ((Property)model.getValueAt(2, 0)).getValue());
        
        FilteredDataCollection<RowDataElement> ftable = new FilteredDataCollection<>( table, new Filter<DataElement>()
        {
            @Override
            public boolean isEnabled()
            {
                return true;
            }
            
            @Override
            public boolean isAcceptable(DataElement de)
            {
                return !de.getName().equals("rowB");
            }
        });

        model = TableDataCollectionUtils.getTableModel(ftable, columnModel);
        assertTrue(model.isSortingSupported());
        assertEquals(2, model.getRowCount());

        sort(columnModel, model, 0, ColumnEx.SORTING_DESCENT);
        assertEquals("rowC", ((Property)model.getValueAt(0, 0)).getValue());
        assertEquals("rowA", ((Property)model.getValueAt(1, 0)).getValue());
        assertEquals("0", ((Property)model.getValueAt(0, 1)).getValue());
        assertEquals("2", ((Property)model.getValueAt(1, 1)).getValue());
        assertEquals(0, ((Property)model.getValueAt(0, 2)).getValue());
        assertEquals(2, ((Property)model.getValueAt(1, 2)).getValue());
        
        sort(columnModel, model, 1, ColumnEx.SORTING_DESCENT);
        assertEquals("rowA", ((Property)model.getValueAt(0, 0)).getValue());
        assertEquals("rowC", ((Property)model.getValueAt(1, 0)).getValue());

        sort(columnModel, model, 2, ColumnEx.SORTING_ASCENT);
        assertEquals("rowC", ((Property)model.getValueAt(0, 0)).getValue());
        assertEquals("rowA", ((Property)model.getValueAt(1, 0)).getValue());
    }

    private void sort(ColumnModel columnModel, TableModel model, int col, int direction)
    {
        for(Column column: columnModel.getColumns())
        {
            ((ColumnEx)column).setSorting(ColumnEx.SORTING_NONE);
        }
        ((ColumnEx)columnModel.getColumns(col)).setSorting(direction);
        ((BiosoftTableModel)model).sort();
    }
}
