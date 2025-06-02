package biouml.standard.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import biouml.model.dynamics.Variable;
import biouml.standard.type.BaseSupport;

/**
 * Base simulation result features.
 */
@SuppressWarnings ( "serial" )
public abstract class SimulationResultSupport extends BaseSupport
{
    protected DataElementPath diagramName;
    protected String simulatorName;
    protected String description;
    protected ArrayList<Variable> initialValues = new ArrayList<>();
    public Map<String, Integer> variableShortMap;
    protected Map<String, Integer> variableMap;//TODO: remove this map and use variablePathMapping everywhere
    protected Map<String, Integer> variablePathMap;

    protected double initialTime;
    protected double completionTime;

    public SimulationResultSupport(DataCollection<?> origin, String name)
    {
        super( origin, name, TYPE_SIMULATION_RESULT );
    }

    public String getDiagramName()//TODO: remove?
    {
        return diagramName != null? diagramName.toString(): "";
    }
    public void setDiagramName(String diagramName)//TODO: remove?
    {
        this.diagramName = DataElementPath.create( diagramName );
    }

    public DataElementPath getDiagramPath()
    {
        return this.diagramName;
    }
    public void setDiagramPath(DataElementPath path)
    {
        this.diagramName = path;
    }

    @Override
    public String getTitle()
    {
        return title;
    }
    @Override
    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSimulatorName()
    {
        return simulatorName;
    }
    public void setSimulatorName(String simulatorName)
    {
        this.simulatorName = simulatorName;
    }

    public double getInitialTime()
    {
        return initialTime;
    }
    public void setInitialTime(double initialTime)
    {
        this.initialTime = initialTime;
    }

    public double getCompletionTime()
    {
        return completionTime;
    }
    public void setCompletionTime(double completionTime)
    {
        this.completionTime = completionTime;
    }

    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }

    public ArrayList<Variable> getInitialValues()
    {
        return initialValues;
    }
    public void addInitialValue(Variable var)
    {
        initialValues.add( var );
    }

    public Map<String, Integer> getVariableMap()
    {
        return variableMap;
    }
    public void setVariableMap(Map<String, Integer> variableMap)
    {
        this.variableMap = variableMap;
        if( variableMap != null )
            initShortMap( variableMap );
    }

    public Map<String, Integer> getVariablePathMap()
    {
        return variablePathMap != null ? variablePathMap : variableMap; //old simulation results does not contain variablePathMap
    }

    public void setVariablePathMap(Map<String, Integer> variablePathMap)
    {
        this.variablePathMap = variablePathMap;
    }

    public abstract boolean isNotFilled();

    public abstract void setValues(String v, double[] values);

    private void initShortMap(Map<String, Integer> variablesMap)
    {
        variableShortMap = new HashMap<String, Integer>();
        for( Entry<String, Integer> e : variablesMap.entrySet() )
        {
            String key = e.getKey();
            int dotIndex = key.lastIndexOf( "." );
            if( dotIndex > -1 )
                key = key.substring( dotIndex + 1 );
            else if( key.startsWith( "$$" ) )
                key = key.substring( 2 );
            else if( key.startsWith( "$" ) )
                key = key.substring( 1 );
            variableShortMap.put( key, e.getValue() );
        }
    }
}