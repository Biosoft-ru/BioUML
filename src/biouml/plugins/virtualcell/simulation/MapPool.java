package biouml.plugins.virtualcell.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Reaction;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class MapPool 
{
    private Map<String, Double> values = new HashMap<>();
    private boolean isSaved =false;
    private double saveStep;
    private String name;
    
    public MapPool(String name)
    {
        this.name = name;
    }
    
    public Set<String> getNames()
    {
        return values.keySet();
    }

    public double getValue(String name)
    {
        return values.get( name );
    }

    public void setValue(String name, double value)
    {
        values.put( name, value );
    }

    public void load(TableDataCollection tdc, String column)
    {
        int index = TableDataCollectionUtils.getColumnIndexes( tdc, new String[] {column} )[0];
        if (index == -1)
            index = TableDataCollectionUtils.getColumnIndexes( tdc, new String[] {column.toLowerCase()} )[0];//dirty hack
        for( RowDataElement rde : tdc )
        {
            double value = (double)rde.getValues()[index];
            String name = rde.getName();
            values.put( name, value );
        }
    }
    /**
     * Init pool from diagram parameters initial values
     */
    public void loadFromParameters(Diagram diagram)
    {
        for (Variable var: diagram.getRole( EModel.class ).getVariables())
        {
            if (var instanceof VariableRole)
                continue;
            double value = var.getInitialValue();
            values.put( name, value );
        }
    }
    
    public void loadFromRates(Diagram diagram)
    {
        for (Reaction r: DiagramUtility.getReactions( diagram ))
        {
            values.put( r.getName(), 0.0 );
        }
    }

    public void save(DataElementPath dep, String column)
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( dep );
        tdc.getColumnModel().addColumn( column, DataType.Float );
        for( Entry<String, Double> e : values.entrySet() )
        {
            TableDataCollectionUtils.addRow( tdc, e.getKey(), new Object[] {e.getValue()} );
        }
        tdc.getOrigin().put( tdc );
    }
    
    public boolean isSaved()
    {
        return isSaved;
    }

    public void setSaved(boolean isSaved)
    {
        this.isSaved = isSaved;
    }

    public double getSaveStep()
    {
        return saveStep;
    }

    public void setSaveStep(double saveStep)
    {
        this.saveStep = saveStep;
    }
    
    public String getName()
    {
        return name;
    }
    
}