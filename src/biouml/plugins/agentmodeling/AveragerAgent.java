package biouml.plugins.agentmodeling;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;

public class AveragerAgent extends SimulationAgent
{

    public static final String STEPS_FOR_AVERAGE = "Steps for average";
    private int index = 0;

    protected int size = 10;
    protected double timeIncrement = 1;

    Map<String, ArrayDeque<Double>> variables = new HashMap<>();
    Map<String, Double> initialValues = new HashMap<>();
    Map<String, Double> movingAverage = new HashMap<>();

    public AveragerAgent(String name) throws Exception
    {
        this(name, new UniformSpan(0, 100, 1), 10);
    }

    public AveragerAgent(String name, Span span, int size) throws Exception
    {
        super(name, span);
        this.size = size;
        init();
    }

    @Override
    public void iterate()
    {
        try
        {
            index++;

            EntryStream.of(variables).removeValues( Collection::isEmpty )
                .mapValues( varData -> DoubleStreamEx.of(varData).limit( size ).average().getAsDouble() )
                .forKeyValue( movingAverage::put );

            if( index >= span.getLength() )
            {
                isAlive = false;
            }
            else
            {
                currentTime = span.getTime(index);
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            isAlive = false;
        }
    }

    @Override
    public void addVariable(String name)
    {
        variables.put(name, new ArrayDeque<Double>(size));
        movingAverage.put(name, 0.0);
        initialValues.put( name, 0.0 );
    }

    @Override
    public void addVariable(String name, double initialValue)
    {
        variables.put(name, new ArrayDeque<Double>(size));
        movingAverage.put(name, initialValue);
        initialValues.put(name, initialValue);
    }

    public String[] getVariables()
    {
        return variables.keySet().toArray(new String[variables.size()]);
    }

    @Override
    public boolean containsVariable(String name)
    {
        return variables.containsKey(name);
    }

    @Override
    public void setCurrentValue(String name, double value) throws Exception
    {
        ArrayDeque<Double> values = variables.get(name);
        if( index < size )
        {
            values.add(value);
        }
        else
        {
            values.pollLast();
            values.push(value);
        }
    }

    @Override
    public double getPriority()
    {
        return SECOND_AGENT_PRIORITY;
    }

    @Override
    public double getCurrentValue(String name) throws Exception
    {
        return movingAverage.get(name);
    }

    //Time boundaries management
    @Override
    public void init() throws Exception
    {
        super.init();
        index = 0;
        for (Entry<String, Double> entry: movingAverage.entrySet())
        {
            entry.setValue(initialValues.get(entry.getKey()));
            variables.get(entry.getKey()).clear();
        }
    }

    @Override
    public double[] getCurrentValues()
    {
        return StreamEx.ofValues( movingAverage ).mapToDouble( x -> x ).toArray();
    }

    @Override
    public String[] getVariableNames()
    {
        return movingAverage.keySet().toArray(new String[movingAverage.size()]);
    }

    @Override
    public double[] getUpdatedValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void setUpdated()
    {
        // TODO Auto-generated method stub

    }
}
