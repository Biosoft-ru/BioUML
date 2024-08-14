package biouml.standard.type;

import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.Sample;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@SuppressWarnings ( "serial" )
public class TableExperiment extends Referrer implements DataCollection<RowDataElement>
{
    public static final String SERIES_ATTR = "series";
    public static final String ID_ATTR = "ID";

    protected static final Logger log = Logger.getLogger(TableExperiment.class.getName());

    private TableDataCollection tableData;

    private DataCollection<Sample> samples;
    private String platform;
    private String species;

    public TableExperiment(DataCollection<?> origin, String name)
    {
        super(origin, name);
        tableData = new StandardTableDataCollection(null, new Properties());
    }

    public int getColumnCount()
    {
        return this.tableData.getColumnModel().getColumnCount();
    }

    public void removeColumn(int columnPos)
    {
        this.tableData.getColumnModel().removeColumn(columnPos);
    }

    public int getRowCount()
    {
        return this.tableData.getSize();
    }

    public Object[] getRow(int index)
    {
        return TableDataCollectionUtils.getRowValues(this.tableData, this.tableData.getName(index));
    }


    public double[][] getMatrix(String[] columnNames, double threshold, double mdCode) throws Exception
    {
        int[] indices = StreamEx.of(columnNames).mapToInt( this.getTableData().getColumnModel()::getColumnIndex ).toArray();
        return getMatrix(indices, threshold, mdCode);
    }

    public double[][] getMatrix(String[] columnNames) throws Exception
    {
        int[] indices = StreamEx.of(columnNames).mapToInt( this.getTableData().getColumnModel()::getColumnIndex ).toArray();
        return getMatrix(indices);
    }


    public double[][] getMatrix(int[] columnIndices) throws Exception
    {
        return TableDataCollectionUtils.getMatrix(this.tableData, columnIndices);
    }

    public double[][] getMatrix(int[] columnIndices, double lowerBoundary, double upperBoundary) throws Exception
    {
        return TableDataCollectionUtils.getMatrix(this.tableData, columnIndices, lowerBoundary, upperBoundary);
    }

    public double[][] getMatrix() throws Exception
    {
        return TableDataCollectionUtils.getMatrix(this.tableData);
    }

    public Object[] getRow(String id)
    {
        return TableDataCollectionUtils.getRowValues(this.tableData, id);
    }

    public void addRow(String key, Object[] rowValues)
    {
        TableDataCollectionUtils.addRow(this.tableData, key, rowValues);
    }

    public TableDataCollection getTableData()
    {
        return tableData;
    }

    public void setTableData(TableDataCollection tableData)
    {
        this.tableData = tableData;
    }

    public String getSpecies()
    {
        return species;
    }

    public void setSpecies(String species)
    {
        String oldValue = this.species;
        this.species = species;
        firePropertyChange("species", oldValue, species);
    }

    public String getPlatform()
    {
        return platform;
    }
    public void setPlatform(String platform)
    {
        String oldValue = this.platform;
        this.platform = platform;
        firePropertyChange("platform", oldValue, platform);
    }

    public DataCollection<Sample> getSamples()
    {
        if( samples == null )
        {
            samples = new VectorDataCollection<>("Samples");

            for( TableColumn col : tableData.getColumnModel() )
            {
                if( col.getNature() == TableColumn.Nature.SAMPLE )
                {
                    try
                    {
                        samples.put(col.getSample());
                    }
                    catch( Exception ex )
                    {
                        log.log( Level.SEVERE, ex.getMessage(), ex );
                    }
                }
            }
        }
        return samples;
    }

    private DataCollection<DataElement> groups;
    public DataCollection<DataElement> getGroups()
    {
        if( groups == null )
        {
            groups = new VectorDataCollection<>("Groups");
        }
        return groups;
    }

    /**
     * Get value from microarray.
     *
     * @todo - rewrite
     */
    public Object getValue(String gene, String sample)
    {
        return TableDataCollectionUtils.getRowValues(tableData, gene, new String[] {sample})[0];
    }

    /**
     * Returns identifier (key) for gene with index i from matrix.
     */
    public String getGeneId(int i)
    {
        return tableData.getName(i);
    }

    /**
     * Get minimum value in table
     */
    public double getMinValue()
    {
        if( minValue == 0.0 && maxValue == 0.0 )
        {
            findMinMax();
        }
        return minValue;
    }

    /**
     * Get maximum value in table
     */
    public double getMaxValue()
    {
        if( minValue == 0.0 && maxValue == 0.0 )
        {
            findMinMax();
        }
        return maxValue;
    }

    private double minValue = 0.0;
    private double maxValue = 0.0;

    private void findMinMax()
    {
        DoubleSummaryStatistics statistics = TableDataCollectionUtils.findMinMax( this.tableData );
        minValue = statistics.getMin();
        maxValue = statistics.getMax();
    }

    @Override
    public TableExperiment clone()
    {
        TableExperiment result = new TableExperiment(getOrigin(), getName());
        result.setDescription(getDescription());
        result.setSpecies(getSpecies());
        result.setPlatform(getPlatform());
        result.setTitle(getTitle());
        result.setComment(getComment());
        result.setDate(getDate());

        TableDataCollection clonedTableData = this.tableData.clone(null, this.tableData.getName());
        result.setTableData(clonedTableData);

        if( null != getLiteratureReferences() )
            result.setLiteratureReferences(getLiteratureReferences().clone());
        if( null != getDatabaseReferences() )
            result.setDatabaseReferences(getDatabaseReferences().clone());

        DataCollection<DataElement> groups = result.getGroups();
        for( ru.biosoft.access.core.DataElement group : getGroups() )
        {
            groups.put(group);
        }

        DataCollection<Sample> samples = result.getSamples();
        for( Sample sample : getSamples() )
        {
            samples.put( sample );
        }

        return result;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        super.addPropertyChangeListener(l);
        this.tableData.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        super.removePropertyChangeListener(l);
        this.tableData.removePropertyChangeListener(l);
    }

    @Override
    public void addDataCollectionListener(DataCollectionListener l)
    {
        this.tableData.addDataCollectionListener(l);
    }

    @Override
    public void close() throws Exception
    {
        this.tableData.close();
    }

    @Override
    public boolean contains(String name)
    {
        return this.tableData.contains(name);
    }

    @Override
    public boolean contains(DataElement element)
    {
        return this.tableData.contains(element);
    }

    @Override
    public RowDataElement get(String name) throws Exception
    {
        return this.tableData.get(name);
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        return this.tableData.getDescriptor(name);
    }

    @Override
    public @Nonnull DataElementPath getCompletePath()
    {
        return this.tableData.getCompletePath();
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return RowDataElement.class;
    }

    @Override
    public DataElement getFromCache(String dataElementName)
    {
        return this.tableData.getFromCache(dataElementName);
    }

    @Override
    public DataCollectionInfo getInfo()
    {
        return this.tableData.getInfo();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return this.tableData.getNameList();
    }

    @Override
    public int getSize()
    {
        return this.tableData.getSize();
    }

    @Override
    public boolean isEmpty()
    {
        return getSize() == 0;
    }

    @Override
    public boolean isMutable()
    {
        return this.tableData.isMutable();
    }

    @Override
    public @Nonnull Iterator<RowDataElement> iterator()
    {
        return this.tableData.iterator();
    }

    @Override
    public void propagateElementChanged(DataCollection<?> source, DataCollectionEvent primaryEvent)
    {
        this.tableData.propagateElementChanged(source, primaryEvent);
    }

    @Override
    public void propagateElementWillChange(DataCollection<?> source, DataCollectionEvent primaryEvent)
    {
        this.tableData.propagateElementWillChange(source, primaryEvent);
    }

    @Override
    public RowDataElement put(RowDataElement element) throws DataElementPutException
    {
        return this.tableData.put(element);
    }

    @Override
    public void release(String dataElementName)
    {
        this.tableData.release(dataElementName);
    }

    @Override
    public void remove(@Nonnull String name) throws Exception
    {
        this.tableData.remove(name);
    }

    @Override
    public void removeDataCollectionListener(DataCollectionListener l)
    {
        this.tableData.removeDataCollectionListener(l);
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        return RowDataElement.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public void reinitialize() throws LoggedException
    {
    }
}
