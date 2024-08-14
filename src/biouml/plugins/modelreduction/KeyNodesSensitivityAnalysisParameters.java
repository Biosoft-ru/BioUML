package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.SimulationEngineWrapper;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@PropertyName ( "Parameters" )
public class KeyNodesSensitivityAnalysisParameters extends SteadyStateAnalysisParameters
{
    public final static String ALL_VARIABLES = "All variables";
    public final static String PRODUCING_GENES = "Producing genes";
    public final static String ALL_GENES = "All genes";
    
    public final static String TYPE_STEADY_STATE = "Steady State";
    public final static String TYPE_TABLE_VALUE = "Table value";
    public final static String TYPE_TABLE_SERIES = "Time Course";
    public final static String TYPE_INITIAL_VALUE = "Initial value";
    private static final String ID_COLUMN = "ID";

    private String type = TYPE_STEADY_STATE;
    private DataElementPath tableData;
    private DataElementPath keyNodes;
    private TableDataCollection table;
    private String timeColumn;
    private String nameColumn;
    private String[] dataColumns;
    private DataElementPath result;
    private String steadyStateVariables = ALL_GENES;

    public StreamEx<String> getAvailableSteadyStateVariables()    
    {
        return StreamEx.of(ALL_VARIABLES, PRODUCING_GENES, ALL_GENES);
    }
    
    public StreamEx<String> getAvailableTypes()
    {
        return StreamEx.of(TYPE_STEADY_STATE, TYPE_INITIAL_VALUE, TYPE_TABLE_VALUE, TYPE_TABLE_SERIES);
    }

    public StreamEx<String> getAvailableColumns()
    {
        if( table == null )
            return StreamEx.empty();
        return StreamEx.of(TableDataCollectionUtils.getColumnNames(table)).append(ID_COLUMN);
    }

    public KeyNodesSensitivityAnalysisParameters()
    {
        setEngineWrapper(new SimulationEngineWrapper());
//        this.setValidationSize(10);
       
    }

    @Override
    public void setDiagram(Diagram diagram)
    {
        super.setDiagram(diagram);
        getEngineWrapper().getEngine().setCompletionTime(10000);
        getEngineWrapper().getEngine().setTimeIncrement(1);
        OdeSimulatorOptions options = (OdeSimulatorOptions)getEngineWrapper().getEngine().getSimulatorOptions();
        options.setAtol(1E-6);
        this.setAbsoluteTolerance(1E-6);

        this.setResult(DataElementPath.create(diagram).getSiblingPath(diagram.getName()+" Sensitivity results"));
    }
    
    @PropertyName ( "Type" )
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }

    @PropertyName ( "Experimental data" )
    public DataElementPath getTableData()
    {
        return tableData;
    }
    public void setTableData(DataElementPath tableData)
    {
        Object oldValue = this.tableData;
        try
        {
            table = tableData.getDataElement(TableDataCollection.class);
            this.tableData = tableData;
            firePropertyChange("tableData", oldValue, tableData);
        }
        catch( Exception ex )
        {

        }
    }

    public TableDataCollection getTable()
    {
        return table;
    }

    public void setTable(TableDataCollection table)
    {
        this.table = table;
        this.tableData = DataElementPath.create(table);
    }

    @PropertyName ( "Result table" )
    public DataElementPath getResult()
    {
        return result;
    }
    public void setResult(DataElementPath result)
    {
        this.result = result;
    }

    @PropertyName ( "Data columns" )
    public String[] getDataColumns()
    {
        return dataColumns;
    }
    public void setDataColumns(String[] dataColumns)
    {
        this.dataColumns = dataColumns;
    }

    @PropertyName ( "Time column" )
    public String getTimeColumn()
    {
        return timeColumn;
    }
    public void setTimeColumn(String timeColumn)
    {
        this.timeColumn = timeColumn;
    }

    @PropertyName ( "Name column" )
    public String getNameColumn()
    {
        return nameColumn;
    }
    public void setNameColumn(String nameColumn)
    {
        String oldValue = this.nameColumn;
        this.nameColumn = nameColumn;
        firePropertyChange("nameColumne", oldValue, nameColumn);
    }

    @PropertyName("Key Nodes")
    public DataElementPath getKeyNodes()
    {
        return keyNodes;
    }
    public void setKeyNodes(DataElementPath keyNodes)
    {
        DataElementPath oldValue = this.keyNodes;
        this.keyNodes = keyNodes;
        firePropertyChange("keyNodes", oldValue, keyNodes);
    }

    @PropertyName("Steady state variables")
    public String getSteadyStateVariables()
    {
        return steadyStateVariables;
    }
    public void setSteadyStateVariables(String steadyStateVariables)
    {
        this.steadyStateVariables = steadyStateVariables;
    }
}
