package ru.biosoft.table;

import java.util.Iterator;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.SortableDataCollection;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.support.DataCollectionRowModelAdapter;
import ru.biosoft.util.BeanUtil;

import com.developmentontheedge.beans.swing.table.BeanTableModelAdapter;
import com.developmentontheedge.beans.swing.table.Column;
import com.developmentontheedge.beans.swing.table.ColumnModel;
import com.developmentontheedge.beans.swing.table.ColumnWithSort;

/**
 * @author lan
 */
public class SortedCollectionModel extends BeanTableModelAdapter implements BiosoftTableModel
{
    private final class DataCollectionRowModelAdapterExtension extends DataCollectionRowModelAdapter
    {
        private int lastPos = -1;
        private DataElement lastElement;
        private Iterator<? extends DataElement> iterator;

        private DataCollectionRowModelAdapterExtension(DataCollection dc)
        {
            super(dc);
        }

        @Override
        public DataElement getBean(int index)
        {
            if(lastPos == index) return lastElement;
            if(lastPos < 0 || lastPos > index || lastPos < index-5)
            {
                iterator = dc.getSortedIterator(sortKey, direction, index, rowTo);
                lastPos = index-1;
            }
            while(lastPos < index)
            {
                lastPos++;
                lastElement = iterator.next();
            }
            return lastElement;
        }
    }

    SortableDataCollection<? extends DataElement> dc;
    String sortKey = "";
    boolean direction = true;
    int rowFrom;
    int rowTo;

    public SortedCollectionModel(SortableDataCollection<? extends DataElement> dc, ColumnModel columnModel)
    {
        super(null, columnModel);
        this.dc = dc;
        this.rm = new DataCollectionRowModelAdapterExtension(dc);
        this.rowFrom = 0;
        this.rowTo = dc.getSize();
    }

    @Override
    public void sort()
    {
        Column[] columns = columnModel.getColumns();
        for( Column column : columns )
        {
            ColumnWithSort col = (ColumnWithSort)column;
            if( col.getSorting() != ColumnWithSort.SORTING_NONE )
            {
                sortKey = col.getColumnKey();
                direction = col.getSorting() == ColumnWithSort.SORTING_ASCENT;
                break;
            }
        }
    }

    @Override
    public String getRowName(int rowIndex)
    {
        return ((DataElement)rm.getBean(rowIndex)).getName();
    }

    @Override
    public boolean isSortingSupported()
    {
        return dc.isSortingSupported();
    }

    @Override
    public Object getRealValue(int row, int column)
    {
        try
        {
            if ( column == 0 && getRowHeader() )
            {
                return row + 1;
            }

            String propertyName = getColumnKey( column );
            if ( propertyName == null )
            {
                throw new DataElementReadException(dc, "column#" + column);
            }

            Object bean = rm.getBean( row );
            if ( bean == null )
            {
                throw new DataElementReadException(dc, "row#" + row);
            }
            return BeanUtil.getBeanPropertyValue(bean, propertyName);
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    @Override
    public void setRange(int rowFrom, int rowTo)
    {
        this.rowFrom = rowFrom;
        this.rowTo = rowTo;
    }
}
