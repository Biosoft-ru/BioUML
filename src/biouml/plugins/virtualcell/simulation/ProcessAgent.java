package biouml.plugins.virtualcell.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Span;
import biouml.plugins.virtualcell.core.Pool;

/**
 * @author Axec
 */
public abstract class ProcessAgent extends SimulationAgent
{
    /**
     * ID to index
     */
    Map<String, Integer> nameToIndex = new HashMap<>();
    Map<String, Pool> parametersMap = new HashMap<>();
    Map<String, Pool> inputMap = new HashMap<>();
    Map<String, Pool> outputMap = new HashMap<>();

    double delta;

    public ProcessAgent(String name, Span span)
    {
        super( name, span );
        delta = span.getTime( 1 ) - span.getTimeStart();
    }

    public void addInpuPool(String variable, Pool pool)
    {
        inputMap.put( variable, pool );
        
    }

    public void addOutputPool(String variable, Pool pool)
    {
        outputMap.put( variable, pool );
    }

    public void addParametersPool(String variable, Pool pool)
    {
        parametersMap.put( variable, pool );
    }

    @Override
    public void iterate()
    {
        read();

        doStep();

        write();
    }

    public abstract void doStep();

    public void init()
    {
        Pool pool = inputMap.values().iterator().next();
        if( pool != null )
            initPoolVariables(pool);
        initParameters();
    }

    public void initPoolVariables(Pool pool)
    {
        int index = 0;
        for( Object name : pool.getNameList() )
        {
            nameToIndex.put( name.toString(), index++ );
        }
    }

    public void initParameters()
    {
        for( Entry<String, Pool> e : parametersMap.entrySet() )
            read( e.getKey(), e.getValue() );
    }

    public void read()
    {
        for( Entry<String, Pool> e : inputMap.entrySet() )
            read( e.getKey(), e.getValue() );
    }

    public void write()
    {
        for( Entry<String, Pool> e : outputMap.entrySet() )
            write( e.getKey(), e.getValue() );
    }

    public void write(String variable, Pool pool)
    {
        if( pool instanceof TablePool )
        {
            TablePool tp = (TablePool)pool;

            for( String name : nameToIndex.keySet() )
            {
                double value = getValue( variable, name );
                tp.setValue( name, value );
            }
        }
    }

    public void read(String variable, Pool pool)
    {
        if( pool instanceof TablePool )
        {
            TablePool tp = (TablePool)pool;

            for( String name : nameToIndex.keySet() )
            {
                double value = tp.getValue( name );
                setValue( variable, name, value );
            }
        }
    }

    public abstract void setValue(String variable, String name, double value);
    public abstract double getValue(String variable, String name);

    @Override
    public double[] getCurrentValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getVariableNames()
    {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public double[] getUpdatedValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void setUpdated() throws Exception
    {
        // TODO Auto-generated method stub

    }


}
