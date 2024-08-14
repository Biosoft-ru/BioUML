package biouml.plugins.simulation;

import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Simple span implementation: successive times are equally separated from each other
 * Span do not store double[] array in memory, instead all values are calculated on fly
 * However method getTimes() returns corresponding array (it is not recommended to use it)
 * 
 * @author axec
 * @author lan
 *
 */
public class UniformSpan implements Span
{
    private final TreeMap<Integer, Double> additionalPoints;
    private final TreeMap<Integer, Integer> smallerPointsNumber;


    /**
     *  Constructor initializes the 2 doubles to the starting and stopping
     *  points of the span of the integration interval and then creates
     *  span with evenly separated points, starting at with the initial time
     *  point for an interval of integration with interpolation at these points.
     *  The separation of these points is specified by the 3rd
     *  parameter.  note: only solvers with continuous interpolation will
     *  interpolate the solution, given a span defined in this way
     */
    public UniformSpan(double a, double b, double inc)
    {
        this(a, b, inc, 0);
    }
    
    public UniformSpan(double a, double b, double incN, double incD)
    {
        if(incD == 0)
        {
            double length = ( b - a ) / incN;
            if((length-Math.floor(length+0.5))/length < 0.00001)
            { // User intended to set inc = 1/length
                incN = b-a;
                incD = Math.floor(length+0.5);
            } else
            {
                incD = 1;
            }
        }
        this.t0 = a;
        this.tf = b;
        this.incN = incN;
        this.incD = incD;

        this.proper = ( b >= a && incN/incD >= 0 );
        //        if( !proper )
        //            return;
        /*
         * find out how many of these evenly spaced points lie on interval
         * to figure out length of array
         * account for initial and final time points in array
         */
        length = (int)Math.floor( ( b - a ) * incD / incN) + 1;

        additionalPoints = new TreeMap<>();
        smallerPointsNumber = new TreeMap<>();
        if( t0+this.incN*(length - 1)/this.incD != tf )
            length++;

        length = Math.max(length, 0);
        uniformLength = length;
    }
    
    @Override
    public double getTimeStart()
    {
        return getTime(0);
    }

    @Override
    public double getTimeFinal()
    {
        return getTime(length - 1);
    }

    public double getTimeIncrement()
    {
        return incN/incD;
    }

    @Override
    public double getTime(int i) throws IndexOutOfBoundsException
    {
        if( i >= length )
            throw new IndexOutOfBoundsException("ArraySpan index " + i + "> " + getLength());

        if( additionalPoints.containsKey(i) )
            return additionalPoints.get(i);

        if( i == 0 )
            return t0;
        if( i == ( length - 1 ) )
            return tf;

        int smallerAdditionalPoints = 0;

        Integer key = smallerPointsNumber.floorKey(i);
        if( key != null )
            smallerAdditionalPoints = smallerPointsNumber.get(key);

        return t0+incN*(i - smallerAdditionalPoints)/incD;
    }

    private double getUniformPoint(int i)
    {
        return t0+incN*i/incD;
    }

    @Override
    public int getLength()
    {
        return length;
    }

    @Override
    public boolean isProper()
    {
        return proper;
    }

    @Override
    public Span getRestrictedSpan(double a, double b)
    {
        if( b < a )
            return new UniformSpan(a, b, incN, incD);

        double startValue;
        double endValue;

        if( b >= tf )
            endValue = tf;
        else
        {
            endValue = getUniformPoint((int) Math.floor( (b-t0)*incD/incN ));
        }

        if( a < t0 )
            startValue = t0;
        else
        {
            startValue = getUniformPoint((int) Math.ceil( (a-t0)*incD/incN ));
        }
        
        boolean startFromA = ( startValue == a );
        boolean endWithB = ( endValue == b );
        UniformSpan result = new UniformSpan(startValue, endValue, getTimeIncrement());


        //additional span points
        HashSet<Double> newPoints = new HashSet<>();
        //if a or b are not included in uniform part of span, add to additional points
        if( !startFromA )
            newPoints.add(a);

        if( !endWithB )
            newPoints.add(b);

        for( Double additionalValue : additionalPoints.values() )
        {
            if( additionalValue > a && additionalValue < b )
                newPoints.add(additionalValue);
        }

        result.addPoints(newPoints);

        return result;
    }

    @Override
    public void addPoints(double[] points)
    {
        HashSet<Double> PointsSet = new HashSet<>();
        for( double point : points )
            PointsSet.add(point);
        addPoints(PointsSet);
    }

    public void addPoints(HashSet<Double> points)
    {
        TreeSet<Double> newPoints = new TreeSet<>(points);

        if( !additionalPoints.isEmpty() )
        {
            newPoints.addAll(additionalPoints.values());
            additionalPoints.clear();
        }

        int i = 0;
        for( Double point : newPoints )
        {
            int uniformSpanPoints;
            if( point > tf )
            {
                uniformSpanPoints = uniformLength;
            }
            else
            {
                uniformSpanPoints = Math.max(0, (int)Math.ceil( ( point - t0 ) * incD / incN));

                if( equals( getUniformPoint( uniformSpanPoints ), point, 1E-10 ) )
                    continue;
            }

            additionalPoints.put(uniformSpanPoints + i, point);
            smallerPointsNumber.put(uniformSpanPoints + i, i + 1);
            i++;
        }
        length += i;
    }

    private boolean equals(double a, double b, double accuracy)
    {
        return Math.abs(a - b) < accuracy * ( a + b );
    }
    
    @Override
    public UniformSpan clone()
    {
        return new UniformSpan(t0, tf, incN, incD);
    }
    
    public JsonObject toJson()
    {
        JsonObject result = new JsonObject();
        
        result.add( "t0", t0 );
        result.add( "tf", tf );
        result.add( "incN", incN );
        result.add( "incD", incD );
        
        JsonArray values = new JsonArray();
        for(Double value : additionalPoints.values())
            values.add( value );
        result.add( "additionalPoints", values );
        
        return result;
    }
    
    public void fromJson(JsonObject json)
    {
    }
    
    @Override
    public String toString()
    {
        return toJson().toString();
    }
    
    public static UniformSpan createInstance(String str)
    {
        JsonObject json = JsonObject.readFrom( str );
        double t0 = json.getDouble( "t0", 0 );
        double tf = json.getDouble( "tf", 1 );
        double incN = json.getDouble( "incN", 1 );
        double incD = json.getDouble( "incD", 1 );
        UniformSpan result = new UniformSpan( t0, tf, incN, incD );
        
        JsonValue values = json.get( "additionalPoints" );
        if(values != null && values instanceof JsonArray)
        {
            HashSet<Double> valueSet = new HashSet<>();
            JsonArray valuesArray = (JsonArray)values;
            for(JsonValue value : valuesArray)
                valueSet.add( value.asDouble() );
            result.addPoints( valueSet );
        }
        
        return result;
    }

    
    private final double t0; // starting time
    private final double tf; // stopping time
    private final double incN, incD; //time increment numerator and denominator
    private int length; //length of span
    private final boolean proper; // whether the span is out of order or not
    private final int uniformLength;
}