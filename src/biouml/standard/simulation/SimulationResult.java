package biouml.standard.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biouml.model.dynamics.Variable;
import biouml.standard.diagram.Util;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class SimulationResult extends SimulationResultSupport
{
    public static final String TYPE_SIMPLE = "Simple";
    
    private List<ResultListener> listeners = new ArrayList<>();
    protected double times[] = new double[10];
    protected double values[][] = new double[10][];
    protected int size = 0; //number of time points
    
    public String getType()
    {
        return TYPE_SIMPLE;
    }
    
    public SimulationResult(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }
       
    public void addResultListener(ResultListener listener)
    {
        listeners.add(listener);
    }
    
    public void removeResultListener(ResultListener listener)
    {
        listeners.remove(listener);
    }
    
    public void removeAllListeners()
    {
        listeners.clear();
    }

    @Override
    public SimulationResult clone(DataCollection origin, String name)
    {
        SimulationResult simulationResult = new SimulationResult(origin, name);
        simulationResult.setTimes(getTimes());
        simulationResult.setValues(getValues());
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
    
    private void compactify()
    {
        if(times.length > size)
        {
            double[] newTimes = new double[size];
            System.arraycopy(times, 0, newTimes, 0, size);
            times = newTimes;
        }
        if(values.length > size)
        {
            double[][] newValues = new double[size][];
            System.arraycopy(values, 0, newValues, 0, size);
            values = newValues;
        }
    }
    
    private void realloc()
    {
        if(times.length == size)
        {
            int newAllocSize = Math.max(size*3/2, 10);
            double[] newTimes = new double[newAllocSize];
            System.arraycopy(times, 0, newTimes, 0, size);
            times = newTimes;
            double[][] newValues = new double[newAllocSize][];
            System.arraycopy(values, 0, newValues, 0, size);
            values = newValues;
        }
    }

    public double[] getTimes()
    {
        compactify();
        return times;
    }
    
    public double getTime(int point)
    {
        return times[point];
    }

    public void setTimes(double[] times)
    {
        this.times = times;
        this.size = this.times.length;
    }

    public double[][] getValues()
    {
        compactify();
        return values;
    }
    
    public double[] getValue(int point)
    {
        return values[point];
    }

    public void setValues(double[][] values)
    {
        this.values = values;
        this.size = this.values.length;
    }
    
    public int getCount()
    {
        return size;
    }
    
    public void add(double t, double[] v)
    {
        realloc();
        times[size] = t;
        values[size] = v;
        size++;
        int count = listeners.size();
        for(int i=0; i<count; i++)
        {
            try
            {
                listeners.get(i).add(t, v);
            }
            catch( Exception e )
            {
            }
        }
    }

    @Override
    public boolean isNotFilled()
    {
        return size == 1;
    }

    public SimulationResult approximate(double initialTime, double completionTime, double timeIncrement)
    {
        if( timeIncrement == 0 )
            return null;

        // one needs to have at least two points to
        // create linear interpolation
        if( times == null || times.length < 2 )
            return null;

        // nothing to approximate if there are no variables at all
        if( values == null || values.length == 0 )
            return null;

        SimulationResult result = new SimulationResult(getOrigin(), getName());

        result.diagramName = diagramName;
        result.simulatorName = simulatorName;
        result.variableMap = new HashMap<>(variableMap);
        result.variablePathMap = variablePathMap == null ? null : new HashMap<>( variablePathMap );
        result.description = description + "\r\n Approximated for time interval: " + initialTime + " - " + completionTime + "with step:"
                + timeIncrement;

        int varCount = values[0].length;

        int n = (int) ( ( completionTime - initialTime ) / timeIncrement );

        if( initialTime + timeIncrement * n < completionTime )
            n += 2;
        else
            n++;

        result.size = n;
        result.initialValues = (ArrayList<Variable>)initialValues.clone();
        result.times = new double[n];
        result.values = new double[n][varCount];

        int pos = 0;
        for( int i = 0; i < n; i++ )
        {
            result.times[i] = initialTime + i * timeIncrement;

            if( result.times[i] > completionTime )
                result.times[i] = completionTime;

            // find appropriate point in the former set
            while( pos < times.length && result.times[i] > times[pos] )
                pos++;

            int pos1 = ( pos == 0 ) ? 0 : ( pos == times.length ? times.length - 2 : pos - 1 );

            double t1 = times[pos1];
            double t2 = times[pos1 + 1];
            for( int var = 0; var < varCount; var++ )
            {
                double x1 = values[pos1][var];
                double x2 = values[pos1 + 1][var];
                result.values[i][var] = ( ( x2 - x1 ) / ( t2 - t1 ) ) * ( result.times[i] - t1 ) + x1;
            }
        }
        return result;
    }

    /**
     * @param controlPoints
     * @return
     * @throws Exception
     */
    public double[][] interpolateLinear(double[] controlPoints) throws Exception
    {
        compactify();
        
        if( values.length == 0 )
            return null;

        int m = values[0].length;
        double[][] interpolatedValues = new double[controlPoints.length][m];
        int[] indexes = MathUtils.multiBinarySearch(times, controlPoints);
        for( int j = 0; j < m; j++ )
        {
            // MathUtils.interpolateLinear was inlined here as it's much faster
            for( int i = 0; i < controlPoints.length; i++ )
            {
                int k = indexes[i];
                if( k < 0 )
                {
                    int index = -k - 1;
                    if( index == 0 )
                    {
                        interpolatedValues[i][j] = values[0][j];
                    }
                    else if( index == times.length )
                    {
                        interpolatedValues[i][j] = values[values.length - 1][j];
                    }
                    else
                    {
                        double x0 = times[index - 1];
                        double f0 = values[index - 1][j];
            
                        //Axec: added for correct infinite simulation values handling
                        if( Double.isInfinite(f0) && Double.isInfinite(values[index][j]) )
                            interpolatedValues[i][j] = f0;
                        else
                            interpolatedValues[i][j] = f0 + ( values[index][j] - f0 ) * ( controlPoints[i] - x0 ) / ( times[index] - x0 );
                    }
                }
                else
                {
                    interpolatedValues[i][j] = values[k][j];
                }
            }
        }
        return interpolatedValues;
    }

    /**
     * Returns values for given variable at all time points
     */
    public double[] getValues(String variableName)
    {
        return getValues(new String[] {variableName})[0];
    }
    
    /**
     * Returns final value for given variable
     */
    public double getFinal(String variableName)
    {
        return getValues(new String[] {variableName})[0][size-1];
    }    
    
    /**
     * Returns initial value for given variable
     */
    public double getInitial(String variableName)
    {
        return getValues(new String[] {variableName})[0][0];
    }  
    
    public String[] getVariables()
    {
        String[] result = new String[variableShortMap.size()];
        variableShortMap.entrySet().forEach( e -> {
            result[e.getValue()] = e.getKey();
        } );
        return result;
    }

    /**
     * Returns values for given variables at all time points
     */
    public double[][] getValues(String[] variableNames)
    {
        compactify();
        
        if( variableNames == null )
            return null;

        double[][] varValues = new double[variableNames.length][times.length];

        for( int i = 0; i < variableNames.length; ++i )
        {
            if( variableMap.containsKey(variableNames[i]) )
            {
                int ind = variableMap.get(variableNames[i]);
                for( int j = 0; j < times.length; ++j )
                {
                    varValues[i][j] = values[j][ind];
                }
            }
            else if( variableShortMap.containsKey( variableNames[i] ) ) //sometimes we want to find values by short name
            {
                int ind = variableShortMap.get( variableNames[i] );
                for( int j = 0; j < times.length; ++j )
                {
                    varValues[i][j] = values[j][ind];
                }
            }
            else
            {
                varValues[i] = null;
            }
        }
        return varValues;
    }
    /**
     * Returns values for given variables at all time points
     */
    public double[][] getValuesTransposed(String[] variableNames)
    {
        compactify();

        if( variableNames == null )
            return null;

        int[] indexes = StreamEx.of( variableNames ).mapToInt( s -> variableShortMap.get( s ) ).toArray();
        double[][] varValues = new double[times.length][variableNames.length];

        for( int i = 0; i < times.length; ++i )
        {
            for( int j = 0; j < variableNames.length; j++ )
                varValues[i][j] = values[i][indexes[j]];
        }
        return varValues;
    }

    public Set<String> getPaths()
    {
        Set<String> paths = new HashSet<>();
        for( String name : getVariablePathMap().keySet() )
        {
            String[] components = Util.getMainPathComponents( name );
            paths.add( components[0] );
        }
        return paths;
    }

    @Override
    public void setValues(String v, double[] values)
    {
        int index = variablePathMap.get( v );
        for( int i = 0; i < times.length; i++ )
            this.values[i][index] = values[i];        
    }
}