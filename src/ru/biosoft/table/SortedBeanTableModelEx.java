package ru.biosoft.table;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.support.DataCollectionRowModelAdapter;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.swing.table.ColumnModel;
import com.developmentontheedge.beans.swing.table.RowHeaderBean;
import com.developmentontheedge.beans.swing.table.SortedBeanTableModelAdapter;

/**
 * BeanTableModel which automatically disabled sorting when number of rows exceeds the limit
 * @author lan
 */
public class SortedBeanTableModelEx extends SortedBeanTableModelAdapter implements BiosoftTableModel
{
    public static final int DEFAULT_MAX_BEAN_SORT_ROWS = 1000;
    private DataCollection<?> dc;
    private int maxRowsForSort;

    public SortedBeanTableModelEx(DataCollectionRowModelAdapter rm, ColumnModel columnModel)
    {
        this(rm, columnModel, DEFAULT_MAX_BEAN_SORT_ROWS);
    }

    public SortedBeanTableModelEx(DataCollectionRowModelAdapter rm, ColumnModel columnModel, int maxRowsForSort)
    {
        super(rm, columnModel);
        this.maxRowsForSort = maxRowsForSort;
        dc = rm.getDataCollection();
    }

    @Override
    public boolean isSortingSupported()
    {
        return rm.size() <= maxRowsForSort;
    }

    @Override
    public void sort()
    {
        if(!isSortingSupported()) return;
        super.sort();
    }

    @Override
    public Object getRealValue(int row, int column)
    {
        return ((Property)getValueAt(row, column)).getValue();
    }

    @Override
    public String getRowName(int rowIndex)
    {
        Object rowElement = getModelForRow(rowIndex);
        if(rowElement instanceof DataElement)
            return ((DataElement)rowElement).getName();
        return null;
    }

    @Override
    public void setRange(int rowFrom, int rowTo)
    {
    }

    @Override
    protected Property getPropertyAt(int row, int column)
    {
        try
        {
            if ( column == 0 && getRowHeader() )
            {
                RowHeaderBean rowHeaderBean = new RowHeaderBean();
                rowHeaderBean.setNumber( row + 1 );
                ComponentModel rowHeaderModel = ComponentFactory.getModel( rowHeaderBean );
                return rowHeaderModel.findProperty( "number" );
            }

            String propertyName = getColumnKey( column );
            if ( propertyName == null )
            {
                throw new DataElementReadException(dc, "col#" + column);
            }

            Object bean = rm.getBean( row );
            Property property = ComponentFactory.getModel( bean ).findProperty( propertyName );
            if(property == null)
            {
                throw new DataElementReadException((DataElement)bean, propertyName);
            }
            return property;
        }
        catch ( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }
}
