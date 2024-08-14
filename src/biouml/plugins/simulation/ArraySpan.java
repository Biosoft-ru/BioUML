package biouml.plugins.simulation;

import java.util.Arrays;
import java.util.stream.Stream;

import one.util.streamex.DoubleStreamEx;

/**
 * Array implementation of span: stores array with all time points
 */
public class ArraySpan implements Span
{

    /**
     *  Constructor initializes the two doubles to the starting and stopping
     *  points of the span (span is defined such that there will be integration
     *  between these 2 points but not interpolation)
     */
    public ArraySpan(double a, double b)
    {
        this(a, b, b - a);
    }

    /**
       Constructor initializes the 2 doubles to the starting and stopping
       points of the span of the integration interval and then creates
       an array of evenly separated points, starting at with the initial time
       point for an interval of integration with interpolation at these points
       (since the endpoint is also included in this array, it is usually the
       only point that doesn't have the same separation as the rest of the
       points).  The separation of these points is specified by the 3rd
       parameter.  note: only solvers with continuous interpolation will
       interpolate the solution, given a span defined in this way<br>
     */
    public ArraySpan(double a, double b, double inc)
    {

        this.proper = ( b > a || inc < 0 );

        if( inc == 0 || !proper )
            return;

        /*
         * find out how many of these evenly spaced points lie on interval
         * to figure out length of array
         * account for initial and final time points in array
         */
        int length = (int)Math.ceil( ( b - a ) / inc) + 1;

        this.times = new double[length]; // put evenly spaced points in array

        for( int i = 0; i < length - 1; i++ )
            times[i] = a + i * inc;

        times[length - 1] = b;

        checkProper();
    }
    /**
     *  Constructor gets array and stores it in the span, so that interpolation
     *  can be done according to the points in that array
     *  note: only solvers with continuous interpolation will
     *  interpolate the solution, given a span defined in this way
     */
    public ArraySpan(double[] times)
    {
        this.times = Arrays.copyOf(times, times.length); // fill array
        //        this.t0 = times[0]; // get initial and final times
        //        this.tf = times[times.length - 1];

        checkProper();
    }

    private void checkProper()
    {
        this.proper = DoubleStreamEx.of( times ).pairMap( (a, b) -> a >= b ? 1 : 0 ).allMatch( x -> x == 0 );
    }

    @Override
    public double getTimeStart()
    {
        return times[0];
    }
    @Override
    public double getTimeFinal()
    {
        return times[times.length - 1];
    }

    public double[] getTimes()
    {
        return times;
    }


    @Override
    public double getTime(int i)
    {
        if( i >= times.length )
            throw new IndexOutOfBoundsException("ArraySpan index" + i + "> " + getLength());
        return times[i];
    }


    @Override
    public int getLength()
    {
        if( times == null )
        {
            return 0;
        }
        return ( times.length );
    }

    @Override
    public boolean isProper()
    {
        return ( proper );
    }

    @Override
    public Span getRestrictedSpan(double a, double b)
    {
        if( times == null )
        {
            return null;
        }
        int n = 0;
        for( double time : times )
        {
            if( time > a && time < b )
                n++;
        }
        double[] newTimes = new double[n + 2];

        newTimes[0] = a;

        n = 1;
        for( double time : times )
        {
            if( time > a && time < b )
                newTimes[n++] = time;
        }

        newTimes[n] = b;
        return new ArraySpan(newTimes);
    }

    // instance variables
    private double[] times; // array of times for interpolation
    private boolean proper; // whether the span is out of order or not


    @Override
    public void addPoints(double[] points)
    {
        times = Stream.of(points, times).flatMapToDouble( Arrays::stream ).sorted().distinct().toArray();
        checkProper();
    }
    
    @Override
    public ArraySpan clone()
    {
        return new ArraySpan(times.clone());
    }
}