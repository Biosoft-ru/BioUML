package ru.biosoft.treetable;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.tree.TreeModel;
import one.util.streamex.IntStreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.Column;
import com.developmentontheedge.beans.swing.table.ColumnModel;
import com.developmentontheedge.beans.swing.treetable.AbstractTreeTableModel;

/**
 * Implementation of {@link com.developmentontheedge.beans.swing.treetable.TreeTableModel}, based on {@link TreeModel} and table collection
 */
public class TreeTableModel extends AbstractTreeTableModel
{
    protected static final Logger log = Logger.getLogger(TreeTableModel.class.getName());

    protected DataCollection table;
    protected ColumnModel columnModel;
    protected boolean hideBranchesAbsentInTable;
    protected int[] columnIndex;

    public TreeTableModel(TreeTableElement viewModel)
    {
        super(DataElementPath.create(viewModel.getTree()));
        this.table = viewModel.getTable();
        this.columnModel = getColumnModel(table);
        initColumnIndex();
        this.hideBranchesAbsentInTable = viewModel.isHideBranchesAbsentInTable();
    }

    private void initColumnIndex()
    {
        columnIndex = IntStreamEx.ofIndices( columnModel.getColumns(), Column::getEnabled ).toArray();
    }
    
    protected ColumnModel getColumnModel(DataCollection dc)
    {
        ColumnModel columnModel;
        if( dc instanceof TableDataCollection )
        {
            columnModel = ( (TableDataCollection)dc ).getColumnModel().getSwingColumnModel();
        }
        else if( dc.getSize() > 0 )
        {
            columnModel = new ColumnModel(dc.iterator().next());
        }
        else
        {
            columnModel = new ColumnModel(dc.getDataElementType(), PropertyInspector.SHOW_USUAL);
        }
        return columnModel;
    }

    @Override
    public int getColumnCount()
    {
        return columnIndex.length;
    }

    @Override
    public String getColumnName(int col)
    {
        return getColumn(col).getName();
    }

    @Override
    public Class getColumnClass(int column)
    {
        return column == 0 ? com.developmentontheedge.beans.swing.treetable.TreeTableModel.class : Property.class;
    }
    
    public Column getColumn(int col)
    {
        return columnModel.getColumns(columnIndex[col]);
    }

    @Override
    public Object getValueAt(Object node, int column)
    {
        if( node instanceof DataElementPath )
        {
            String id = ( (DataElementPath)node ).getName();
            if( column == 0 )
            {
                return id;
            }
            try
            {
                DataElement rowDe = table.get(id);
                if( rowDe == null )
                    return null;
                String columnKey = getColumn(column).getColumnKey();
                return ComponentFactory.getModel(rowDe).findProperty(columnKey);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot get value for row: " + id, e);
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(Object node, int column)
    {
        if( column == 0 )
            return true;
        return false;
    }

    @Override
    public void setValueAt(Object aValue, Object node, int column)
    {
        throw new UnsupportedOperationException();
    }
    
    private final WeakHashMap<Object, Object[]> childrenCache = new WeakHashMap<>();
    protected synchronized Object[] getChildren(Object parent)
    {
        Object[] cached = childrenCache.get(parent);
        if(cached == null)
        {
            DataCollection<?> dc = ((DataElementPath)parent).optDataCollection();
            List<Object> result = new ArrayList<>();
            if(dc != null)
            {
                for(String name: dc.getNameList())
                {
                    if(hideBranchesAbsentInTable && !table.contains(name)) continue;
                    result.add(((DataElementPath)parent).getChildPath(name));
                }
            }
            cached = result.toArray();
            childrenCache.put(parent, cached);
        }
        return cached;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        return getChildren(parent)[index];
    }

    @Override
    public int getChildCount(Object parent)
    {
        return getChildren(parent).length;
    }
}
