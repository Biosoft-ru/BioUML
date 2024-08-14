package ru.biosoft.table;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.table.columnbeans.Descriptor;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.document.TableDataCollectionColumnModel;
import ru.biosoft.table.exception.TableAddColumnException;
import ru.biosoft.table.exception.TableNoColumnException;
import ru.biosoft.table.exception.TableRemoveColumnException;
import ru.biosoft.util.TextUtil;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.swing.table.Column;

/**
 * Column model for {@link TableDataCollection}
 */
public class ColumnModel implements Iterable<TableColumn>
{
    /**
     * Auto-generated name for new column
     */
    public static final String NEW_COLUMN_NAME = "New Column";

    protected TableColumn[] columnsInfo = new TableColumn[0];
    protected TObjectIntMap<String> columnName2Index = new TObjectIntHashMap<>( 10, 0.5f, -1 );

    protected TableDataCollection origin;
    // Whether there are columns with expressions (used to speed up some operations)
    protected boolean expressions = false;

    public ColumnModel(TableDataCollection origin)
    {
        this.origin = origin;
    }

    /**
     * Create column info clone. New column will be bound to 'this' column model
     * @param dp base column info object
     * @param newName name for the cloned column
     */
    public TableColumn cloneTableColumn(TableColumn dp, String newName)
    {
        TableColumnProxy tableColumn = new TableColumnProxy(newName, dp.getDisplayName(), dp.getShortDescription(), dp.getType(), dp
                .getExpression());
        if( dp.isHidden() )
            tableColumn.setHidden(true);
        tableColumn.copyMetaData(dp);
        return tableColumn;
    }

    /**
     * Create column info clone
     * @param dp base column info object
     */
    public TableColumn cloneTableColumn(TableColumn dp)
    {
        return cloneTableColumn(dp, dp.getName());
    }

    public boolean hasExpressions()
    {
        return expressions;
    }

    /**
     * Add new column.
     * @param name column name
     * @param displayName column display name
     * @param shortDesc column description
     * @param type column type
     * @param expr JavaScript expression for column
     * @return newly added column
     */
    public TableColumn addColumn(String name, String displayName, String shortDesc, Class<?> valueClass, String expr)
    {
        return addColumn(new TableColumnProxy(name, displayName, shortDesc, DataType.fromClass( valueClass ), expr));
    }

    /**
     * Add new column.
     * @param name column name
     * @param valueClass column value class
     * @param expr JavaScript expression for column
     * @return newly added column
     */
    public TableColumn addColumn(String name, Class<?> valueClass, String expr)
    {
        return addColumn(new TableColumnProxy(name, valueClass, expr));
    }

    /**
     * Add new column.
     * @param name column name
     * @param type column type
     * @return newly added column
     */
    public TableColumn addColumn(String name, DataType type)
    {
        return addColumn(new TableColumnProxy(name, type.getType()));
    }
    
    public TableColumn addColumn(String name, Class<?> valueClass)
    {
        return addColumn(new TableColumnProxy(name, valueClass));
    }

    /**
     * Add descriptor as new column.
     * @param name column name
     * @param descriptor descriptor for the new column
     * @return newly added column
     */
    public TableColumn addColumn(String name, Descriptor descriptor)
    {
        TableColumn column = addColumn(cloneTableColumn(descriptor.createColumn(), name));
        column.setValue(TableColumn.ANNOTATION_SOURCE_PROPERTY, DataElementPath.create(descriptor).toString());
        return column;
    }

    public TableColumn addColumn(Descriptor descriptor)
    {
        TableColumn baseCol = descriptor.createColumn();
        TableColumn column = addColumn(cloneTableColumn(baseCol, generateUniqueColumnName(baseCol.getName())));
        column.setValue(TableColumn.ANNOTATION_SOURCE_PROPERTY, DataElementPath.create(descriptor).toString());
        return column;
    }

    /**
     * Add new column.
     * @param dp column info object.
     * @return newly added column
     */
    public TableColumn addColumn(TableColumn dp)
    {
        try
        {
            TableColumn[] newColumnInfo = new TableColumn[columnsInfo.length + 1];
            System.arraycopy(columnsInfo, 0, newColumnInfo, 0, columnsInfo.length);
            newColumnInfo[columnsInfo.length] = dp;
            columnsInfo = newColumnInfo;

            if( origin != null )
            {
                //add new column to all values
                origin.cachedElements().forEach(RowDataElement::addNewColumn);
                origin.forEach( RowDataElement::addNewColumn );
            }
            rebuildColumnName2Index();
        }
        catch( Exception e )
        {
            throw new TableAddColumnException(e, origin, dp.getName());
        }
        return dp;
    }

    /**
     * Add new column with default name.
     * @param type column type
     */
    public void addColumn(Class<?> valueClass)
    {
        addColumn(new TableColumnProxy(generateUniqueColumnName(), valueClass ));
    }

    /**
     * Returns true if there's a column with given name
     * @param name
     * @return
     */
    public boolean hasColumn(String name)
    {
        return optColumnIndex(name) > -1;
    }

    /**
     * Get Column object.
     * @param name column name
     * @return TableColumn object
     * @throws TableNoColumnException if no such column found
     */
    public @Nonnull TableColumn getColumn(String name) throws TableNoColumnException
    {
        return getColumn(getColumnIndex(name));
    }

    /**
     * Get Column object.
     * @param index column index
     * @return TableColumn object
     * @throws TableNoColumnException if no such column found
     */
    public @Nonnull TableColumn getColumn(int index) throws TableNoColumnException
    {
        TableColumn column = index >= 0 && index < this.columnsInfo.length ? this.columnsInfo[index] : null;
        if(column == null)
            throw new TableNoColumnException(origin, "#"+index);
        return column;
    }

    /**
     * Get column count.
     */
    public int getColumnCount()
    {
        return this.columnsInfo.length;
    }

    /**
     * Get column info array for all columns
     * for internal use only
     */
    TableColumn[] getColumns()
    {
        return columnsInfo;
    }

    @Override
    public Iterator<TableColumn> iterator()
    {
        return Collections.unmodifiableList(Arrays.asList(columnsInfo)).iterator();
    }
    
    public StreamEx<TableColumn> stream()
    {
        return StreamEx.of(columnsInfo);
    }

    /**
     * Get column index by name.
     * @param name column name
     * @return column index or -1 if column not exists.
     */
    public int optColumnIndex(String name)
    {
        if( name == null )
            return -1;
        return columnName2Index.get(name.toLowerCase());
    }
    
    /**
     * Get column index by name.
     * @param name column name
     * @return column index
     * @throws TableNoColumnException if no column with such name exists
     */
    public int getColumnIndex(String name) throws TableNoColumnException
    {
        int result = optColumnIndex(name);
        if(result == -1)
            throw new TableNoColumnException(origin, name);
        return result;
    }

    /**
     * Generates column name which doesn't already exists in current model using default new column name as the base
     */
    public String generateUniqueColumnName()
    {
        return generateUniqueColumnName(NEW_COLUMN_NAME);
    }

    /**
     * Generates column name which doesn't already exists in current model
     * @param baseName - base name to generate new column name by
     * @return generated name (equals to baseName if it wasn't in model, otherwise some suffix will be added)
     */
    public String generateUniqueColumnName(String baseName)
    {
        String name = baseName;
        int i = 0;
        while( hasColumn(name) || name.equals(DataCollectionConfigConstants.NAME_PROPERTY) )
        {
            name = baseName + "_" + ( ++i );
        }
        return name;
    }

    /**
     * Remove column.
     * @param columnPos column index
     */
    public void removeColumn(int columnPos)
    {
        try
        {
            TableColumn[] newColumnInfo = new TableColumn[columnsInfo.length - 1];
            System.arraycopy(columnsInfo, 0, newColumnInfo, 0, columnPos);
            System.arraycopy(columnsInfo, columnPos + 1, newColumnInfo, columnPos, columnsInfo.length - columnPos - 1);
            columnsInfo = newColumnInfo;

            for( RowDataElement rowDataElement : origin )
            {
                Object[] oldValues = rowDataElement.getValues(false);
                Object[] newValues = new Object[oldValues.length - 1];

                System.arraycopy(oldValues, 0, newValues, 0, columnPos);
                System.arraycopy(oldValues, columnPos + 1, newValues, columnPos, newValues.length - columnPos);

                rowDataElement.setValues(newValues);
            }

            rebuildColumnName2Index();
        }
        catch( Exception e )
        {
            throw new TableRemoveColumnException(e, origin, "#"+columnPos);
        }
    }

    /**
     * Rename column.
     * @param columnPos column index
     * @param newName new column name
     */
    public void renameColumn(int columnPos, String newName)
    {
        TableColumn newColumn = cloneTableColumn( columnsInfo[columnPos], newName);
        newColumn.setDisplayName(newName);
        columnsInfo[columnPos] = newColumn;
        rebuildColumnName2Index();
    }

    
    /**
     * Generate {@link Column} for swing table.
     */
    public com.developmentontheedge.beans.swing.table.ColumnModel getSwingColumnModel()
    {
        TableColumn[] columns = new TableColumn[1 + columnsInfo.length];
        System.arraycopy(columnsInfo, 0, columns, 1, columnsInfo.length);
        //for some reason BeanExplorer needs key "name" for first column
        columns[0] = new TableColumn(DataCollectionConfigConstants.NAME_PROPERTY, "ID", "ID", "", Boolean.valueOf(origin.getInfo().getProperty(TableDataCollection.INTEGER_IDS))
                ? DataType.Integer : DataType.Text, null);
        if( origin.getReferenceType() != null )
            columns[0].setValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, origin.getReferenceType());

        Column[] swingColumns = new Column[columns.length];
        System.arraycopy(columns, 0, swingColumns, 0, columns.length);
        return new TableDataCollectionColumnModel(origin, swingColumns);
    }

    protected void rebuildColumnName2Index()
    {
        columnName2Index.clear();
        expressions = false;
        for( int i = 0; i < columnsInfo.length; i++ )
        {
            if( !TextUtil.isEmpty(columnsInfo[i].getExpression()) )
                expressions = true;
            columnName2Index.put(columnsInfo[i].getName().toLowerCase(), i);
        }
    }

    /**
     * Column proxy class for columns with listeners support
     */
    @SuppressWarnings ( "serial" )
    public class TableColumnProxy extends TableColumn implements PropertyChangeListener
    {
        public TableColumnProxy(String name, Class<?> type)
        {
            super(name, type);
        }

        public TableColumnProxy(String name, Class<?> type, String expression)
        {
            super(name, type, expression);
        }

        public TableColumnProxy(String name, String displayName, String shortDesc, DataType type, String expression)
        {
            super(name, displayName, shortDesc, type, expression);
        }

        @Override
        public void setDisplayName(String displayName)
        {
            super.setDisplayName(displayName);
        }

        @Override
        public void setName(String name)
        {
            String oldValue = getName();
            renameColumn(getColumnIndex(getName()), name);
            origin.cachedElements().forEach(rde -> ComponentFactory.recreateChildProperties(ComponentFactory.getModel(rde)));
            origin.firePropertyChange(new PropertyChangeEvent(this, "columnInfo.name", oldValue, name));
        }

        @Override
        public void setShortDescription(String shortDesc)
        {
            String oldValue = this.getShortDescription();
            super.setShortDescription(shortDesc);
            origin.firePropertyChange(new PropertyChangeEvent(this, "columnInfo.shortDesc", oldValue, shortDesc));
        }

        @Override
        public void setType(DataType type)
        {
            DataType oldType = getType();
            super.setType(type);
            origin.cachedElements().forEach(rde -> ComponentFactory.recreateChildProperties(ComponentFactory.getModel(rde)));
            origin.firePropertyChange(new PropertyChangeEvent(this, "columnInfo.type", oldType, type));
        }

        @Override
        public void setNature(Nature nature)
        {
            Nature oldNature = super.getNature();
            super.setNature(nature);
            origin.firePropertyChange(new PropertyChangeEvent(this, "columnInfo.nature", oldNature, nature));
        }

        @Override
        public void setExpression(String value)
        {
            Object oldValue = getExpression();
            super.setExpression(value);
            if( !TextUtil.isEmpty(value) )
                expressions = true;
            if( ( oldValue == null && value != null ) || ( oldValue != null && value == null )
                    || ( oldValue != null && value != null && !oldValue.equals(value) ) )
                origin.recalculateTable(null);
            origin.firePropertyChange(new PropertyChangeEvent(this, "columnInfo.value", oldValue, value));
        }

        @Override
        public void setHidden(boolean hidden)
        {
            boolean oldValue = isHidden();
            super.setHidden(hidden);
            origin.firePropertyChange(new PropertyChangeEvent(this, "columnInfo.hidden", oldValue, hidden));
        }

        @Override
        public void setSample(Sample sample)
        {
            Sample oldSample = super.getSample();
            super.setSample(sample);

            if( oldSample != null )
            {
                oldSample.removePropertyChangeListener(this);
            }

            if( sample != null )
            {
                sample.addPropertyChangeListener(this);
            }

            origin.firePropertyChange(new PropertyChangeEvent(this, "columnInfo.sample", oldSample, sample));
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            origin.firePropertyChange(new PropertyChangeEvent(this, "columnInfo", evt.getOldValue(), evt.getNewValue()));
        }
    }
}
