package biouml.plugins.simulation.java;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.ae.AeModel;
import biouml.plugins.simulation.ae.AeSolver;
import biouml.plugins.simulation.ae.NewtonSolverWrapper;
import biouml.plugins.simulation.ode.OdeModel;
import one.util.streamex.EntryStream;

/**
 * WARNING: READ BEFORE EDITING THIS CLASS!
 * 
 * This class and some others constitutes separate jar which is used during model simulation in BioUML,
 * therefore all class on which it depends should also added to this jar
 * jar file used for simulation is specified by SimulationEngine
 * 
 * Before adding any new dependencies here - please think twice.
 * 
 * If you add dependency - add this class (and all classes from which it depends) to build_bdk.xml
 * (see biouml.plugins.simulation building)
 * @see SimulationEngine
 */


abstract public class JavaBaseModel implements OdeModel, AeModel
{
    public boolean fastRegime;
    public double[] x_values;
    public double time;
    public static double pi = Math.PI;
    public static double exponentiale = Math.E;
    public static double avogadro = 6.02214179E23;
    public double[] initialValues;
    protected double CONSTRAINTS__VIOLATED = 0;

    protected AeSolver aeSolver = new NewtonSolverWrapper();

    @Override
    public double[] dy_dt(double time, double[] x) throws Exception
    {
        return fastRegime? dy_dt_fast(time, x): dy_dt_slow(time, x);
    }
    
    public double[] dy_dt_slow(double time, double[] x) throws Exception
    {
        return null;
    }
    
    public double[] dy_dt_fast(double time, double[] x) throws Exception
    {
        return null;
    }
    
    @Override
    public double[] getY()
    {
        return x_values;
    }
    
    @Override
    public double getTime()
    {
        return time;
    }

    public void setAeSolver(AeSolver aeSolver)
    {
        this.aeSolver = aeSolver;
    }

    @Override
    public double[] solveAlgebraic(double[] z)
    {
        return null;
    }

    @Override
    public double[] getConstraints()
    {
        return null;
    }

    @Override
    public double[] checkEvent(double time, double[] x) throws Exception
    {
        return new double[0];
    }

    @Override
    public void processEvent(int i)
    {
    }

    @Override
    public boolean getEventsInitialValue(int i) throws IndexOutOfBoundsException
    {
        return true;
    }

    @Override
    public boolean isEventTriggerPersistent(int i) throws IndexOutOfBoundsException
    {
        return true;
    }

    @Override
    public double[] getEventsPriority(double time, double[] x) throws Exception
    {
        return null;
    }

    @Override
    public double[] getPrehistory(double time)
    {
        return null;
    }

    @Override
    public double getPrehistory(double time, int i)
    {
        double[] result = getPrehistory(time);
        if( result != null && i < result.length - 1 )
            return result[i];

        return 0;
    }

    @Override
    public double[] getCurrentHistory()
    {
        return null;
    }

    @Override
    public double[] extendResult(double time, double[] x) throws Exception
    {
        return x;
    }

    @Override
    public double[] getCurrentValues() throws Exception
    {
        return extendResult(time, x_values);
    }
    
   
    @Override
    public double[] getCurrentState()
    {
        return null;
    }

    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        //do nothing -- this is for compatibility with old models
    }

    protected List<double[]> simulationResultHistory = new ArrayList<double[]>();
    protected List<Double> simulationResultTimes = new ArrayList<Double>();

    // Primitive mirror of simulationResultTimes, kept in sync by updateHistory()/clear()/init().
    // delay() hot path reads this instead of unboxing a List<Double> on every probe.
    private double[] timeCache = new double[0];

    // Monotonic cursor for the delay() fast path: the largest index known to hold a
    // time <= the last requested t, and the t that produced it. History times only
    // increase, so as long as the requested t is non-decreasing the cursor never has
    // to move backwards; it starts at the previous insertion point and only scans
    // history entries between the previous and current requested times.
    //
    // delay() is NOT guaranteed to be called with non-decreasing t (generated models
    // evaluate several delay(..., time - d_i) expressions at the same simulation time
    // with different offsets, and solvers may re-evaluate at earlier t), so the cursor
    // is only used when t >= delayCursorTime; otherwise we fall back to the binary
    // search (firstPart) which is correct for any ordering.
    private int delayCursor = 0;
    private double delayCursorTime = Double.NEGATIVE_INFINITY;

    private double timeAt(int i)
    {
        if( i < timeCache.length )
            return timeCache[i];
        return simulationResultTimes.get(i).doubleValue();
    }

    private void addHistory(double[] z, double t)
    {
        simulationResultHistory.add(z);
        simulationResultTimes.add(t);
        int n = simulationResultTimes.size();
        if( timeCache.length < n )
        {
            double[] grown = new double[Math.max(16, n * 2)];
            System.arraycopy(timeCache, 0, grown, 0, timeCache.length);
            timeCache = grown;
        }
        timeCache[n - 1] = t;
    }

    private void clearHistory()
    {
        simulationResultHistory.clear();
        simulationResultTimes.clear();
        timeCache = new double[0];
        delayCursor = 0;
        delayCursorTime = Double.NEGATIVE_INFINITY;
    }

    @Override
    public void updateHistory(double t)
    {
        double[] z = getCurrentHistory();
        if( z != null )
        {
            addHistory(z, t);
        }
    }

    public void clear()
    {
        clearHistory();
    }

    public double[] getTimes()
    {
        double[] times = new double[simulationResultTimes.size()];
        for( int i = 0; i < times.length; i++ )
        {
            times[i] = simulationResultTimes.get(i).doubleValue();
        }
        return times;
    }

    public double[] getResults(double time) throws Exception
    {
        return EntryStream.zip(simulationResultTimes, simulationResultHistory)
            .filterKeys( t -> t == time ).values().findFirst().orElse( null );
    }
    
    private int firstPart(double t)
    {
        int low = 0;
        int high = simulationResultTimes.size();
        while (low < high)
        {
            int middle = low + (high - low) / 2;
            double middleTime = timeAt(middle);

            if (middleTime < t)
            {
                low = middle + 1;
            }
            else
            {
                high = middle;
            }
        }
        return low;
    }

    // Fast path for delay(): when the requested t is non-decreasing relative to the
    // previous call, advance the monotonic cursor to the first index whose time >= t.
    // The cursor starts at the previous insertion point, so it only scans history
    // entries between the previous and current requested times instead of searching
    // the whole history.
    //
    // delay() is NOT guaranteed to be called with non-decreasing t (generated models
    // evaluate several delay(..., time - d_i) at the same simulation time with
    // different offsets, and solvers may re-evaluate at earlier t). If t moves
    // backwards relative to the last cursor query, the cursor is not a valid start
    // point, so fall back to the binary search, which is correct for any ordering.
    // The cursor state is deliberately left unchanged on the fallback: it remains a
    // valid lower bound for all future t >= delayCursorTime.
    private int firstPartMonotonic(double t)
    {
        if( t < delayCursorTime )
            return firstPart(t);

        int i = delayCursor;
        int n = simulationResultTimes.size();
        while( i < n && timeAt(i) < t )
            i++;
        delayCursor = i;
        delayCursorTime = t;
        return i;
    }

    private double interpolate(double t1, double t2, double x1, double x2, double t)
    {
        return ( ( x2 - x1 ) / ( t2 - t1 ) ) * ( t - t1 ) + x1;
    }
    
    public double delay(int index, double t)
    {
        if( simulationResultTimes.size() == 0 || t < timeAt(0) )
            return getPrehistory(t, index);

        int i = firstPartMonotonic(t);

        if( i == 0 )
            return simulationResultHistory.get(0)[index];


        double x1;
        double x2;
        double t1;
        double t2;

        if( i == simulationResultHistory.size() )
        {
            //get current values
            x1 = this.getCurrentHistory()[index];
            t1 = this.time;
        }
        else
        {
            x1 = simulationResultHistory.get(i)[index];
            t1 = timeAt(i);
        }

        x2 = simulationResultHistory.get(i - 1)[index];
        t2 = timeAt(i - 1);
        return interpolate(t1, t2, x1, x2, t);
    }

    @Override
    public void init() throws Exception
    {
        clearHistory();
        CONSTRAINTS__VIOLATED = 0;
        initialValues = getInitialValues();
        isInit = true;
    }

    @Override
    public double[] getInitialValues() throws Exception
    {
        return initialValues;
    }
    /**
     * Alternative to init(). It forcefully sets initial values and parameters to concrete values and sets isInit true.
     * It must include initial assignments to parameters because getInitialValues() will not cause this assignments
     * @param values
     */
    @Override
    public void init(double[] initialValues, Map<String, Double> parameters) throws Exception
    {
        this.initialValues = Arrays.copyOf(initialValues, initialValues.length);
        clearHistory();
        this.x_values = Arrays.copyOf(initialValues, initialValues.length);
        Field[] fields = this.getClass().getDeclaredFields();

        for( Field field : fields )
        {
            String name = field.getName();
            double value = 0.0;
            boolean isChanged = false;
            if( parameters.containsKey(name) )
            {
                value = parameters.get(name);
                isChanged = true;
            }
            else if( name.startsWith("rate") )
                isChanged = true;

            if( isChanged )
            {
                try
                {
                    field.setDouble(this, value);
                }
                catch( IllegalArgumentException | IllegalAccessException e )
                {
                    throw new Exception( "Error during parameter " + name + "value setting to " + value );
                }
            }
        }
        isInit = true;
    }

    protected boolean isInit = false;
    @Override
    public boolean isInit()
    {
        return isInit;
    }

    @Override
    public boolean isStatic()
    {
        return false;
    }

    //event issues
    protected static class DelayedEvent implements Comparable<DelayedEvent>
    {
        public Double executionTime;
        public double[] assignments;
        public int eventIndex;

        protected DelayedEvent(int eventIndex, double executionTime, double ... assignments)
        {
            this.executionTime = executionTime;
            this.assignments = assignments;
            this.eventIndex = eventIndex;
        }

        @Override
        public int compareTo(DelayedEvent o)
        {
            return executionTime.compareTo(o.executionTime);
        }
    }

    public static class DelayedEventPriorityQueue extends PriorityQueue<DelayedEvent>
    {
    }

    protected HashMap<Integer, DelayedEventPriorityQueue> delayedEvents = new HashMap<>();

    protected void addDelayedEvent(int i, double executionTime, double ... assignment)
    {
        DelayedEventPriorityQueue queue = ( delayedEvents.containsKey(i) ) ? delayedEvents.get(i) : new DelayedEventPriorityQueue();
        queue.add(new DelayedEvent(i, executionTime, assignment));
        delayedEvents.put(i, queue);
    }

    protected DelayedEvent getNextDelayedEvent(int i)
    {
        DelayedEventPriorityQueue queue = delayedEvents.get(i);
        return ( queue != null ) ? queue.peek() : null;
    }

    protected Double getNextExecutionTime(int i)
    {
        DelayedEvent de = getNextDelayedEvent(i);
        return ( de != null ) ? de.executionTime : Double.POSITIVE_INFINITY;
    }

    protected double[] getNextAssignments(int i)
    {
        DelayedEvent de = getNextDelayedEvent(i);
        return ( de != null ) ? de.assignments : null;
    }


    protected void removeDelayedEvent(int i)
    {
        DelayedEventPriorityQueue queue = delayedEvents.get( i );
        if( queue != null )
            queue.poll();
    }
    
    @Override
    public String getEventMessage(int i)
    {
        return null;
    }

    //TODO: do more thorough cloning
    @Override
    public Model clone()
    {
        try
        {
            JavaBaseModel result = (JavaBaseModel)super.clone();
            result.simulationResultHistory = new ArrayList<>();
            for( double[] val : simulationResultHistory )
                result.simulationResultHistory.add(val.clone());
            result.delayedEvents = new HashMap<>();
            result.initialValues = this.initialValues.clone();
            result.x_values = this.x_values.clone();
            result.simulationResultTimes = new ArrayList<>(simulationResultTimes);
            // Copy the primitive time cache (preserves allocated capacity, avoids an
            // O(n) List<Double> unboxing pass). Must be a copy -- not a shared array --
            // because each model extends its own history independently.
            result.timeCache = this.timeCache.clone();
            result.delayCursor = 0;
            result.delayCursorTime = Double.NEGATIVE_INFINITY;
            return result;
        }
        catch( Exception ex )
        {
            throw new InternalError(ex.getMessage());
        }
    }
    
    public void setFastRegime(boolean fastRegime)
    {
        this.fastRegime = fastRegime;
    }
    
    public static double NUMERIC_AND(double a, double b)
    {
        return a != 0 && b != 0 ? 1 : 0;
    }

    public static double NUMERIC_OR(double a, double b)
    {
        return a != 0 || b != 0 ? 1 : 0;
    }
    
    public static double NUMERIC_NOT(double a)
    {
        return a == 0? 1: 0;
    }

    public static double NUMERIC_XOR(double a, double b)
    {
        return a != 0 ^ b != 0 ? 1 : 0;
    }

    public static double implies(double a, double b)
    {
        return b != 0 || a == 0 ? 1 : 0;
    }

    public static double NUMERIC_LT(double a, double b)
    {
        return a < b ? 1 : 0;
    }

    public static double NUMERIC_GT(double a, double b)
    {
        return a > b ? 1 : 0;
    }

    public static double NUMERIC_EQ(double a, double b)
    {
        return a == b ? 1 : 0;
    }

    public static double NUMERIC_LEQ(double a, double b)
    {
        return a <= b ? 1 : 0;
    }

    public static double NUMERIC_GEQ(double a, double b)
    {
        return a >= b ? 1 : 0;
    }

    public static double NUMERIC_NEQ(double a, double b)
    {
        return a != b ? 1 : 0;
    }
    
    protected static double max(double ... args)
    {
        double result =  args[0];
        for (int i=1; i<args.length; i++)
            result = Math.max(result, args[i]);
        return result;
    }
    
    protected static double min(double ... args)
    {
        double result =  args[0];
        for (int i=1; i<args.length; i++)
            result = Math.min(result, args[i]);
        return result;
    }
    
    @Override
    public boolean isConstraintViolated()
    {
        return CONSTRAINTS__VIOLATED != 0;
    }
    
    @Override
    public boolean hasFastOde()
    {
        return false;
    }
    
    public double getInitialValue(int i)
    {
        return this.initialValues[i];
    }
}