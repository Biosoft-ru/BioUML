package ru.biosoft.table;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.WrappedException;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.swing.table.ColumnWithSort;

import one.util.streamex.StreamEx;
import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.SortableDataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.plugins.javascript.JScriptContext;
import ru.biosoft.plugins.javascript.JavaScriptUtils;
import ru.biosoft.table.columnbeans.Descriptor;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.exception.TableNoColumnException;
import ru.biosoft.util.TextUtil;

/**
 * Base class for table collections
 */
@ClassIcon ( "resources/table.gif" )
@PropertyName ( "table" )
public abstract class TableDataCollection extends AbstractDataCollection<RowDataElement> implements CloneableDataElement,
        SortableDataCollection<RowDataElement>
{
    //public static final String DataCollectionConfigConstants.NAME_PROPERTY = "name";
    public static final String GENERATED_IDS = DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX + "generatedIds";
    public static final String INTEGER_IDS = DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX + "integerIds";

    protected static final Logger log = Logger.getLogger(TableDataCollection.class.getName());

    public static final String SORT_ORDER_PROPERTY = "SortOrder";
    /**
     * Column model for table
     */
    protected ColumnModel columnModel;

    public TableDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        columnEventSupport = new ColumnEventSupport();
        String sortOrderStr = getInfo().getProperty(SORT_ORDER_PROPERTY);
        if( sortOrderStr != null )
        {
            sortOrder = new SortOrder(sortOrderStr);
        }
        this.columnModel = new ColumnModel(this);
    }

    /**
     * Get column model for table
     */
    public ColumnModel getColumnModel()
    {
        return columnModel;
    }

    public StreamEx<TableColumn> columns()
    {
        return getColumnModel().stream();
    }

    /**
     * Sort table by column.
     * @param columnNumber index of data column, -1 for sorting by names
     * @param dir direction of sorting (false = descending, true = ascending)
     */
    abstract public void sortTable(int columnNumber, boolean dir);

    /**
     * Get table row by index
     * @param rowIdx index
     */
    abstract public RowDataElement getAt(int rowIdx);

    /**
     * Get value of table cell
     * @param rowIdx row index
     * @param columnIdx column index
     */
    abstract public Object getValueAt(int rowIdx, int columnIdx);

    /**
     * Set value to table cell
     * @param rowIdx row index
     * @param columnIdx column index
     * @param value cell value
     */
    abstract public void setValueAt(int rowIdx, int columnIdx, Object value);

    /**
     * Get row element name by row index
     * @param rowIdx row index
     */
    abstract public String getName(int rowIdx);

    //
    // Groups
    //
    private DataCollection<SampleGroup> groups;
    public DataCollection<SampleGroup> getGroups()
    {
        if( groups == null )
        {
            groups = new VectorDataCollection<>( "Groups", SampleGroup.class, null );
        }
        return groups;
    }

    //
    // Samples
    //
    public DataCollection<Sample> getSamples()
    {
        DataCollection<Sample> samples = new VectorDataCollection<>( "Samples", null, new Properties() );

        for( TableColumn col : getColumnModel() )
        {
            if( col.getNature() == TableColumn.Nature.SAMPLE )
            {
                try
                {
                    samples.put(col.getSample());
                }
                catch( Exception ex )
                {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }

        return samples;
    }

    //
    // Calculated column support
    //
    /**
     * Recalculate values for row
     * @param rde row element
     */
    protected Map<String, Script> scriptText2PreparedScript = new HashMap<>();
    public void evaluateRowCalculation(RowDataElement rde)
    {
        for( int i = 0; i < getColumnModel().getColumnCount(); i++ )
        {
            TableColumn col = getColumnModel().getColumn(i);
            if( !col.isExpressionEmpty() )
            {
                Context context = JScriptContext.getContext();
                ImporterTopLevel scope = JScriptContext.getScope();

                String expression = prepareExpression(col.getExpression(), col.getType());
                JavaScriptUtils.defineColumnVariables(rde, scope, false);
                try
                {
                    Script prepared = scriptText2PreparedScript.computeIfAbsent(expression, k -> context.compileString(expression, "", 1, null));
                    if( prepared != null )
                    {
                        AtomicReference<Object> ref = new AtomicReference<>();
                        BiosoftSecurityManager.runInSandbox( () -> {
                            Object value = prepared.exec( context, scope );
                            ref.set( value );
                        } );
                        Object value = ref.get();
                        if( value instanceof NativeJavaObject )
                            value = ( (NativeJavaObject)value ).unwrap();
                        value = col.getType().convertValue(value);
                        rde.getValues(false)[i] = value;
                    }
                }
                catch( Throwable e )
                {
                    if(e instanceof WrappedException)
                    {
                        e = ((WrappedException)e).getCause();
                    }
                    rde.getValues(false)[i] = e;
                }
            }
        }
    }

    private String prepareExpression(String expr, DataType columnType)
    {
        String expression = expr;
        if( columnType == DataType.Integer )
            expression = "parseInt(" + expression + ")";
        else if( columnType == DataType.Float )
            expression = "parseFloat(" + expression + ")";
        return expression;
    }

    /**
     * Add new row to the table
     * This is alternative way for mass row addition as it bypass checks and may unite additions into batches
     * Note that all items should have unique keys as it's not guaranteed to be checked in this function
     * After mass addition finalizeAddition must be called
     * @throws Exception
     */
    public void addRow(RowDataElement rde) throws Exception
    {
        doPut(rde, true);
    }

    public void finalizeAddition() throws LoggedException
    {
    }

    public void recalculateColumn(String name)
    {
        TableColumn column = getColumnModel().getColumn(name);
        String descriptor = column.getValue(TableColumn.ANNOTATION_SOURCE_PROPERTY);
        if( descriptor == null )
            return;
        DataElement de = CollectionFactory.getDataElement(descriptor);
        if( ! ( de instanceof Descriptor ) )
            return;
        int i = getColumnModel().getColumnIndex(name);
        try
        {
            Map<String, Object> values = ( (Descriptor)de ).getColumnValues(getNameList());
            for( Entry<String, Object> entry : values.entrySet() )
            {
                RowDataElement row = ( get(entry.getKey()) );
                row.getValues()[i] = entry.getValue();
                doPut(row, false);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, getCompletePath() + ": cannot fill column " + column.getName(), e);
        }
    }

    public void recalculateTable(FunctionJobControl jobControl)
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
            jobControl.setPreparedness(0);
        }
        for( RowDataElement rowDataElement : v_cache.values() )
        {
            if( rowDataElement != null )
                rowDataElement.markNonEvaluated();
        }
        for( TableColumn column : getColumnModel() )
        {
            recalculateColumn(column.getName());
        }
        firePropertyChange("*", null, null);
        if( jobControl != null )
        {
            jobControl.functionFinished();
        }
    }

    //
    // Listener support
    //
    protected ColumnEventSupport columnEventSupport;

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        columnEventSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        columnEventSupport.removePropertyChangeListener(l);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        columnEventSupport.setNotificationEnabled(isNotificationEnabled());
        columnEventSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Reports a bound property update to any of the registered listeners. No
     * event is fired if old and new values are equal and non-null.
     */
    public void firePropertyChange(PropertyChangeEvent evt)
    {
        columnEventSupport.setNotificationEnabled(isNotificationEnabled());
        columnEventSupport.firePropertyChange(evt);
    }

    protected static class ColumnEventSupport extends Option
    {
        @Override
        public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
        {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
        @Override
        public void firePropertyChange(PropertyChangeEvent evt)
        {
            super.firePropertyChange(evt);
        }
    }

    protected class SortOrder
    {
        private int columnNumber;
        /**
         * True if ascending sort
         * False otherwise
         */
        private boolean dir;

        /**
         * @return index of column to sort by
         */
        public int getColumnNumber()
        {
            return columnNumber;
        }

        /**
         * @return true if ascending sort, false otherwise
         */
        public boolean getDirection()
        {
            return dir;
        }
        @Override
        public boolean equals(Object o)
        {
            if( o == null || ! ( o instanceof SortOrder ) )
                return false;
            SortOrder so = (SortOrder)o;
            return so.columnNumber == columnNumber && so.dir == dir;
        }

        @Override
        public int hashCode()
        {
            throw new UnsupportedOperationException();
        }

        public SortOrder(int columnNumber, boolean dir)
        {
            this.columnNumber = columnNumber;
            this.dir = dir;
        }

        public void set()
        {
            try
            {
                columnModel.getColumn(getColumnNumber()).setSorting(
                        getDirection() ? ColumnWithSort.SORTING_ASCENT : ColumnWithSort.SORTING_DESCENT);
            }
            catch( TableNoColumnException e )
            {
            }
        }

        @Override
        public String toString()
        {
            return columnNumber + ":" + dir;
        }

        public SortOrder(String sortOrder)
        {
            String[] so = TextUtil.split( sortOrder, ':' );
            if( so.length == 2 )
            {
                try
                {
                    columnNumber = Integer.parseInt(so[0]);
                }
                catch( NumberFormatException e )
                {
                }
                dir = Boolean.parseBoolean(so[1]);
            }
        }
    }

    protected SortOrder sortOrder = new SortOrder( -1, true);

    /**
     * Make a clone of table
     * @param parent parent data collection of new table
     * @param new table name
     */
    @Override
    public final TableDataCollection clone(DataCollection parent, @Nonnull String name)
    {
        return clone(parent, name, Filter.INCLUDE_ALL_FILTER);
    }

    /**
     * Make a clone of table
     * @param parent parent data collection of new table
     * @param new table name
     */
    public final TableDataCollection clone(DataCollection parent, @Nonnull String name, @Nonnull Filter<? super RowDataElement> filter)
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection(parent, name);
        result.getInfo().setDescription(getInfo().getDescription());
        DataCollectionUtils.copyPersistentInfo(result, this);
        DataCollectionUtils.copyAnalysisParametersInfo( this, result );

        TableColumn[] columns = getColumnModel().getColumns();
        if( columns != null )
        {
            for( TableColumn dp : columns )
            {
                try
                {
                    TableColumn column = result.getColumnModel().cloneTableColumn(dp);
                    result.getColumnModel().addColumn(column);
                }
                catch( Throwable t )
                {
                }
            }
        }

        for( RowDataElement rowDataElement : this )
        {
            if(!filter.isAcceptable( rowDataElement ))
                continue;
            Object[] oldValues = rowDataElement.getValues(false);
            Object[] newValues = new Object[oldValues.length];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);

            TableDataCollectionUtils.addRow(result, rowDataElement.getName(), newValues, true);
        }
        try
        {
            result.finalizeAddition();
        }
        catch( Exception e )
        {
        }

        return result;
    }

    public String getReferenceType()
    {
        ReferenceType referenceType = ReferenceTypeRegistry.optReferenceType(getInfo().getProperty(
                ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY));
        return referenceType == null ? null : referenceType.toString();
    }

    public void setReferenceType(String referenceType)
    {
        ReferenceType type = ReferenceTypeRegistry.optReferenceType(referenceType);
        if( type != null )
        {
            ReferenceTypeRegistry.setCollectionReferenceType(this, type);
            try
            {
                getCompletePath().save(this);
            }
            catch( Exception e )
            {
                ExceptionRegistry.log(e);
            }
        }
    }

    public String getSpecies()
    {
        return getInfo().getProperty(DataCollectionUtils.SPECIES_PROPERTY);
    }

    public void setSpecies(String species)
    {
        if( species == null )
            return;
        Properties properties = getInfo().getProperties();
        properties.setProperty( DataCollectionUtils.SPECIES_PROPERTY, species );
    }

    public void setDescription(String description)
    {
        getInfo().setDescription( TextUtil.nullToEmpty( description ) );
        try
        {
            getCompletePath().save(this);
        }
        catch( Exception e )
        {
            ExceptionRegistry.log(e);
        }
    }

    @Override
    public boolean isSortingSupported()
    {
        return true;
    }

    @Override
    public String[] getSortableFields()
    {
        return columns().map( TableColumn::getName ).prepend( DataCollectionConfigConstants.NAME_PROPERTY ).toArray( String[]::new );
    }

    @Override
    public List<String> getSortedNameList(String field, boolean direction)
    {
        if( field.equals(DataCollectionConfigConstants.NAME_PROPERTY) )
            sortTable( -1, direction);
        else
            sortTable(getColumnModel().optColumnIndex(field), direction);
        return getNameList();
    }

    @Override
    public @Nonnull
    Class<? extends DataElement> getDataElementType()
    {
        return RowDataElement.class;
    }

    @Override
    public Iterator<RowDataElement> getSortedIterator(String field, boolean direction, int from, int to)
    {
        List<String> sortedNameList = getSortedNameList(field, direction);
        return AbstractDataCollection.createDataCollectionIterator( this, sortedNameList.subList( from, to ).iterator() );
    }
}
