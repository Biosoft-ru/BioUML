package biouml.plugins.agentmodeling;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.logging.Level;
import java.util.HashMap;

import org.jfree.data.xy.XYSeries;

import ru.biosoft.graphics.Pen;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;

public class PlotAgent extends SimulationAgent
{

    private HashMap<String, XYSeries> dataset = new HashMap<>();
    private HashMap<String, Pen> stringToPen = new HashMap<>();

    private int index = 0;

    protected double timeIncrement = 0.01;

    public PlotAgent(String name) throws Exception
    {
        this(name, new UniformSpan(0, 100, 0.01));
    }

    public PlotAgent(String name, Span span) throws Exception
    {
        super(name, span);
        init();
    }

    @Override
    public double getPriority()
    {
        return OBSERVER_AGENT_PRIORITY;
    }

    @Override
    public void iterate()
    {
        try
        {
            index++;
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
            currentTime = Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public void addVariable(String name)
    {
        dataset.put(name, new XYSeries(name));
    }

    public void setSpec(String variableName, float width, Color color)
    {
        stringToPen.put(variableName, new Pen(width, color));
    }

    public void setSpec(String variableName, float width, Color color, float[] dash)
    {
        stringToPen.put(variableName, new Pen(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, dash, 0), color));
    }

    public void setSpec(String variableName, Pen pen)
    {
        stringToPen.put(variableName, pen);
    }

    public Pen getSpec(String variableName)
    {
        if( !stringToPen.containsKey(variableName) )
            return null;
        return stringToPen.get(variableName);
    }

    @Override
    public boolean containsVariable(String name)
    {
        return dataset.containsKey(name);
    }

    @Override
    public void setCurrentValue(String name, double value) throws Exception
    {
        XYSeries series = dataset.get(name);
        series.add(currentTime, value);
    }

    @Override
    public void init() throws Exception
    {
        super.init();
        for (XYSeries series: dataset.values())
            series.clear();
        index = 0;
    }

    public double getInitialTime()
    {
        return initialTime;
    }
    public void setInitialTime(double t)
    {
        span = new UniformSpan(initialTime, getCompletionTime(), getTimeIncrement());
        initialTime = t;
    }

    public double getCompletionTime()
    {
        return completionTime;
    }
    public void setCompletionTime(double t)
    {
        span = new UniformSpan(getInitialTime(), completionTime, getTimeIncrement());
        completionTime = t;
    }


    public double getTimeIncrement()
    {
        return this.timeIncrement;
    }
    public void setTimeIncrement(double t)
    {
        span = new UniformSpan(getInitialTime(), getCompletionTime(), timeIncrement);
        timeIncrement = t;
    }

    public HashMap<String, XYSeries> getDataSet()
    {
        return dataset;
    }

    @Override
    public double[] getCurrentValues()
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
    protected void setUpdated()
    {
        // TODO Auto-generated method stub     
    }


}
