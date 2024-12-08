package biouml.plugins.virtualcell.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Span;

/**
 * @author Axec
 */
public abstract class ProcessAgent extends SimulationAgent
{
    /**
     * ID to index
     */
    Map<String, Integer> nameToIndex = new HashMap<>();
    Map<String, MapPool> parametersMap = new HashMap<>();
    Map<String, MapPool> inputMap = new HashMap<>();
    Map<String, MapPool> outputMap = new HashMap<>();

    double delta;

    public ProcessAgent(String name, Span span)
    {
        super( name, span );
        delta = span.getTime( 1 ) - span.getTimeStart();
    }

    public void addInpuPool(String variable, MapPool pool)
    {
        inputMap.put( variable, pool );

    }

    public void addOutputPool(String variable, MapPool pool)
    {
        outputMap.put( variable, pool );
    }

    public void addParametersPool(String variable, MapPool pool)
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
        if( !inputMap.isEmpty() )
        {
            MapPool pool = inputMap.values().iterator().next();
            if( pool instanceof MapPool )
                initPoolVariables( (MapPool)pool );
        }
        initParameters();
    }

    public void initPoolVariables(MapPool pool)
    {
        int index = 0;
        for( String name : pool.getNames() )
        {
            nameToIndex.put( name.toString(), index++ );
        }
    }

    public void initParameters()
    {
        for( Entry<String, MapPool> e : parametersMap.entrySet() )
            read( e.getKey(), e.getValue() );
    }

    public void read()
    {
        for( Entry<String, MapPool> e : inputMap.entrySet() )
            read( e.getKey(), e.getValue() );
    }

    public void write()
    {
        for( Entry<String, MapPool> e : outputMap.entrySet() )
            write( e.getKey(), e.getValue() );
    }

    public void write(String variable, MapPool pool)
    {
        if( pool instanceof MapPool )
        {
            MapPool tp = (MapPool)pool;

            for( String name : nameToIndex.keySet() )
            {
                double value = getValue( variable, name );
                tp.setValue( name, value );
            }
        }
    }

    public void read(String variable, MapPool pool)
    {
        if( pool instanceof MapPool )
        {
            MapPool tp = (MapPool)pool;

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
