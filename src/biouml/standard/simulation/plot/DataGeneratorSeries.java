package biouml.standard.simulation.plot;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.mozilla.javascript.EcmaError;



import biouml.standard.simulation.SimulationDataGenerator;

/**
 * @author lan
 *
 */
public class DataGeneratorSeries extends Series
{
    private SimulationDataGenerator xGenerator, yGenerator;
    private int lastPointCount = 0;
    
    private class Listener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            int currentPointCount = getValuesCount();
            if(currentPointCount != lastPointCount)
            {
                lastPointCount = currentPointCount;
                firePropertyChange("plotData", null, null);
            }
        }
    };
    
    private Listener listener = new Listener();
    
    /**
     * 
     */
    public DataGeneratorSeries()
    {
        super();
    }

    /**
     * @param name
     */
    public DataGeneratorSeries(String name)
    {
        super(name);
    }

    /**
     * @return the xGenerator
     */
    public SimulationDataGenerator getXGenerator()
    {
        return xGenerator;
    }

    /**
     * @param xGenerator the xGenerator to set
     */
    public void setXGenerator(SimulationDataGenerator xGenerator)
    {
        if(this.xGenerator != null)
        {
            this.xGenerator.removePropertyChangeListener(listener);
        }
        this.xGenerator = xGenerator;
        if(this.xGenerator != null)
        {
            this.xGenerator.addPropertyChangeListener(listener);
        }
        firePropertyChange("plotData", null, null);
    }

    /**
     * @return the yGenerator
     */
    public SimulationDataGenerator getYGenerator()
    {
        return yGenerator;
    }

    /**
     * @param yGenerator the yGenerator to set
     */
    public void setYGenerator(SimulationDataGenerator yGenerator)
    {
        if(this.yGenerator != null)
        {
            this.yGenerator.removePropertyChangeListener(listener);
        }
        this.yGenerator = yGenerator;
        if(this.yGenerator != null)
        {
            this.yGenerator.addPropertyChangeListener(listener);
        }
        firePropertyChange("plotData", null, null);
    }

    @Override
    public String getXVar()
    {
        return this.xGenerator.getName();
    }

    @Override
    public String getYVar()
    {
        return this.yGenerator.getName();
    }

    @Override
    public int getValuesCount()
    {
        return Math.min(this.xGenerator.getPointsCount(), this.yGenerator.getPointsCount());
    }

    @Override
    public double[] getXValues()
    {
        return getValues(this.xGenerator);
    }

    @Override
    public double[] getYValues()
    {
        return getValues(this.yGenerator);
    }

    private double[] getValues(SimulationDataGenerator generator)
    {
        try
        {
            int count = getValuesCount();
            double[] result = new double[count];
            for(int i=0; i<count; i++)
            {
                try
                {
                    result[i] = generator.getValue(i);
                }
                catch( Exception e )
                {
                    //TODO: temporary fix for plots not to fail, should be eliminated
                    result[i] = 0;
                }
            }
            return result;
        }
        catch( EcmaError e )
        {
            throw new RuntimeException("Cannot obtain series: " + e.getErrorMessage());
        }
        catch( Exception e )
        {
            throw new RuntimeException("Cannot obtain series", e);
        }
    }
}
