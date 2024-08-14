package biouml.plugins.simulation.plot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.util.ListUtil;

/**
 * @author anna
 *
 */
public class TableSimulationResultsWrapper extends StandardTableDataCollection
{
    private final double[] timeFilter;
    private Map<String, List<String>> resultVariables;
    private final List<String> nameList;
    private final Map<String, Integer> idToIndex;
    private double[][] values;
    private boolean initialized = false;
    private boolean initializing = false;
    private ColumnModel columnModel;
    
    @Override
    public ColumnModel getColumnModel()
    {
        init();
        return columnModel;
    }
    
    public TableSimulationResultsWrapper(DataCollection<?> parent, String name)
    {
        super(null, name);
        this.timeFilter = new double[3];
        nameList = new ArrayList<>();
        idToIndex = new HashMap<>();
    }
    
    public TableSimulationResultsWrapper(Map<String, List<String>> resultVariables, double[] timeFilter)
    {
        this(null, "plotData");
        this.resultVariables = resultVariables;
        System.arraycopy(timeFilter, 0, this.timeFilter, 0, 3);
    }
    
    private synchronized void init()
    {
        if( initialized )
            return;
        initializing = true;
        getInfo().getProperties().setProperty(INTEGER_IDS, "true");
        columnModel = new ColumnModel(this);
        int rowCount = 0;
        int currentColumn = 0;
        int colCount = ListUtil.sumTotalSize( resultVariables );
        for( Map.Entry<String, List<String>> entry : resultVariables.entrySet() )
        {
            SimulationResult sResult = DataElementPath.create(entry.getKey()).getDataElement(SimulationResult.class);
            sResult = sResult.approximate(timeFilter[0], timeFilter[1], timeFilter[2]);
            List<String> varNames = entry.getValue();
            double times[] = sResult.getTimes();
            rowCount = times.length;
            
            values = new double[rowCount][colCount];
            for( int i = 0; i < rowCount; i++ )
            {
                String name = String.valueOf(times[i]).replaceAll("\\.0$", "");
                nameList.add(name);
                idToIndex.put(name, i);
            }
            Map<String, Integer> vMap = sResult.getVariableMap();
            double sValues[][] = sResult.getValues();
            for( String varName : varNames )
            {
                Integer cc = vMap.get(varName);
                if( null != cc )
                {
                    //BeanTableModelAdaptor uses column name as property name to get value. Slash is treated as subproperty.  
                    String columnName = sResult.getName() + " " + varName.replace( "/", "_" );
                    columnModel.addColumn(columnName, Double.class);
                    for( int i = 0; i < rowCount; i++ )
                    {
                        values[i][currentColumn] = sValues[i][cc];
                    }
                    currentColumn++;
                }
                else
                {
                    //TODO: no such variable in result
                }
            }
        }
        initializing = false;
        initialized = true;
    }
    
    @Override
    public int getSize()
    {
        return getNameList().size();
    }

    @Override
    public String getName(int index)
    {
        return getNameList().get(index);
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        if(initializing) return Collections.emptyList();
        init();
        return nameList;
    }

    @Override
    protected RowDataElement doGet(String name) throws Exception
    {
        init();
        if(idToIndex.containsKey(name))
            return getAt(idToIndex.get(name));
        return null;
    }

    @Override
    public RowDataElement getAt(int index)
    {
        init();
        String name = getName(index);
        RowDataElement rde = new RowDataElement(name, this);
        Object[] result = new Object[columnModel.getColumnCount()];
        for(int i = 0; i < columnModel.getColumnCount(); i++)
        {
            result[i] = values[index][i];
        }
        rde.setValues(result);
        return rde;
    }

    @Override
    public Object getValueAt(int rowIdx, int columnIdx)
    {
        init();
        return values[rowIdx][columnIdx];
    }
}
