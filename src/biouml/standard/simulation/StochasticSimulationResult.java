package biouml.standard.simulation;

import java.util.Arrays;
import biouml.plugins.simulation.CycledResultListener;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class StochasticSimulationResult extends SimulationResult implements CycledResultListener
{
    public static String TYPE_STOCHASTIC = "Stochastic";
    
    /** array of all recieved values: span points x variable x repeats */
    protected double[][][] fullValues;//or store only calculated values?
    protected double[][] median;
    protected double[][] sd;
    protected double[][] q1;
    protected double[][] q3;
    protected int cycle = 0;
    protected int cycles;
    protected int index = 0;
    protected int vars;

    public StochasticSimulationResult(DataCollection<?> origin, String name)
    {
        super( origin, name );
    }
    
    public StochasticSimulationResult(DataCollection<?> origin, String name, int cycles, int tPoints)
    {
        super( origin, name );
        this.size = tPoints;
        this.cycles = cycles;        
    }
    
    public void setSpanSize(int size)
    {
        this.size = size;
    }

    public void setVars(int vars)
    {
        this.vars = vars;
    }
    
    public void setCycles(int cycles)
    {
        this.cycles = cycles;
    }
    
    public String getType()
    {
        return TYPE_STOCHASTIC;
    }

    @Override
    public void add(double t, double[] y)
    {
        for( int i = 0; i < y.length; i++ )
            fullValues[index][i][cycle] = y[i];
        times[index] = t;
        index++;
    }


    @Override
    public void startCycle()
    {
        cycle++;
        index = 0;
    }

    @Override
    public void start(Object model)
    {
        cycle = 0;
        index = 0;
        times = new double[size];
        fullValues = new double[size][vars][cycles];
        median = new double[size][vars];
        sd = new double[size][vars];
        values = new double[size][vars];
        q1 = new double[size][vars];
        q3 = new double[size][vars];
    }

    @Override
    public void finish()
    {
        double time = System.currentTimeMillis();
        for( int i = 0; i < size; i++ ) //time points
        {
            for( int j = 0; j < vars; j++ ) //variables
            {
                double[] vals = fullValues[i][j];
                Arrays.parallelSort( vals );
                values[i][j] = mean( vals );
                if( vals.length >= 3 )
                {
                    q1[i][j] = quartile1( vals );
                    median[i][j] = median( vals );
                    q3[i][j] = quartile3( vals );
                    sd[i][j] = sd( values[i][j], vals );
                }
            }
        }
        System.out.println( "Finish:" + ( System.currentTimeMillis() - time ) / 1000 );
    }
    
    @Override
    public double[] getTimes()
    {
        return times;
    }
    
    @Override
    public double[][] getValues()
    {
        return values;
    }
    
    public void setMedian(double[][] median)
    {
        this.median = median;
    }
    
    public double[][][] getFullValues()
    {
        return fullValues;
    }

    public void setQ1(double[][] q1)
    {
        this.q1 = q1;
    }
    
    public void setQ3(double[][] q3)
    {
        this.q3 = q3;
    }

    public double[][] getMedian()
    {
        return median;
    }
    
    public double[][] getSD()
    {
        return sd;
    }

    public double[][] getQ1()
    {
        return q1;
    }
    
    public double[][] getQ3()
    {
        return q3;
    }
    
    public double[] getMedian(int point)
    {
        return median[point];
    }
    
    public double[] getQ1(int point)
    {
        return q1[point];
    }
    
    public double[] getQ3(int point)
    {
        return q3[point];
    }

    public static double quartile1(double[] array)
    {
        final int size = array.length;
        return size % 2 == 1 ? median( array, 0, ( size - 1 ) / 2 ) : median( array, 0, size / 2 );
    }

    public static double mean(double[] array)
    {
        double sum = 0;
        for( double s : array )
            sum += s;
        return sum / array.length;
    }
    
    public static double sd(double mean, double[] array)
    {
        double sum = 0;
        for( double s : array )
        {
            double val = s - mean;
            sum += val * val;
        }
        return Math.sqrt( sum / ( array.length - 1 ) );
    }

    public static double median(double[] array)
    {
        return median( array, 0, array.length );
    }

    public static double median(double[] array, int start, int end)
    {
        int size = end - start;
        return size % 2 == 1 ? array[start + ( size - 1 ) / 2] : ( array[start + size / 2] + array[start + size / 2 - 1] ) * 0.5;
    }

    public static double quartile3(double[] array)
    {
        final int size = array.length;
        return size % 2 == 1 ? median( array, ( size + 1 ) / 2, size ) : median( array, size / 2, size );
    }

    @Override
    public void addAsFirst(double t, double[] y)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void update(double t, double[] y)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public StochasticSimulationResult clone(DataCollection origin, String name)
    {
        StochasticSimulationResult simulationResult = new StochasticSimulationResult(origin, name, cycles, size );
        simulationResult.setVars( vars );
        simulationResult.setTimes(getTimes());
        simulationResult.setValues(getValues());
        simulationResult.q1 = q1;
        simulationResult.q3 = q3;
        simulationResult.median = median;
        simulationResult.fullValues = fullValues;
        simulationResult.values = values;
        simulationResult.setDiagramPath(getDiagramPath());
        simulationResult.setDescription(getDescription());
        simulationResult.setCompletionTime(getCompletionTime());
        simulationResult.setInitialTime(getInitialTime());
        simulationResult.setSimulatorName(getSimulatorName());
        simulationResult.setTitle(getTitle());
        simulationResult.setVariableMap(getVariableMap());
        simulationResult.setVariablePathMap(getVariablePathMap());
        return simulationResult;
    }
    
    @Override
    public void setValues(String v, double[] values)
    {
        int index = variablePathMap.get( v );
        for( int i = 0; i < times.length; i++ )
            this.fullValues[i][index][cycle] = values[i];        
    }
}