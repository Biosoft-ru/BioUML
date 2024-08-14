package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;
import java.util.Map;

public class ShorttermModel extends JavaBaseModel
{
    public double RATE_OF_counter;
    public double RATE_OF_x;
    public double assignment;
    public double center;
    public double delta;
    public double target;
    public double up;
    public double xMax;
    public double xMin;

    
    public double[] getY()
    {
        return x_values;
    }


    private void calculateParameters() throws Exception
    {
        double[] x_values = this.x_values;
        RATE_OF_counter = 1;
        RATE_OF_x = target - x_values[1];
    }


    private void calculateInitialParameters()
    {
        double[] x_values = this.x_values;
        RATE_OF_counter = 1;
        RATE_OF_x = target - x_values[1];
    }


    public final double[] dy_dt_slow(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[2];
        calculateParameters();
        dydt[0] = +1; //  rate rule for of counter
        dydt[1] = +target - x_values[1]; //  rate rule for of x
        return dydt;
    }



    @Override
    public final void init() throws Exception
    {
        CONSTRAINTS__VIOLATED = 0;
        RATE_OF_counter = 0.0; // initial value of RATE_OF_counter
        RATE_OF_x = 0.0; // initial value of RATE_OF_x
        assignment = 0.0; // initial value of assignment
        center = 0.0; // initial value of center
        delta = 10.0; // initial value of delta
        target = 10.0; // initial value of target
        time = 0.0; // initial value of time
        up = 1.0; // initial value of up
        xMax = 0.0; // initial value of xMax
        xMin = 0.0; // initial value of xMin
        calculateInitialValues();
        this.isInit = true;
    }


    @Override
    public final void init(double[] initialValues, Map<String, Double> parameters) throws Exception
    {
        super.init( initialValues, parameters );
        this.initialValues = x_values.clone();
    }


    private final void calculateInitialValues() throws Exception
    {
        double[] x_values = this.x_values = new double[2];
        this.time = 0.0;
        calculateInitialParameters();
        this.initialValues = x_values;
    }



    public final double[] extendResult(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        calculateParameters();
        return getCurrentState();
    }


    public final double[] getCurrentState()
    {
        return new double[] {center, x_values[0], delta, target, time, up, x_values[1], xMax, xMin,};
    }
    
    public String[] getVariables()
    {
        return new String[] {"center", "counter", "delta", "target", "time", "up", "x", "xMax", "xMin"};
    }

    @Override
    public final void setCurrentValues(double[] values) throws Exception
    {
        CONSTRAINTS__VIOLATED = 0;
        center = values[0];
        x_values[0] = values[1];
        delta = values[2];
        target = values[3];
        time = values[4];
        up = values[5];
        x_values[1] = values[6];
        xMax = values[7];
        xMin = values[8];
        if( time == 0 )
        {
            initialValues[0] = values[1];
            initialValues[1] = values[6];
            calculateInitialParameters();
        }
        else
            calculateParameters();
    }



    public final double[] checkEvent(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        calculateParameters();
        double[] z = new double[2];
        z[0] = ( NUMERIC_GT( x_values[0], 1 ) != 0 ) ? 1 : -1; // switch part1;
        z[1] = ( NUMERIC_GT( x_values[0], 1 ) != 0 ) ? 1 : -1; // switch part2;
        return z;
    }


    public final void processEvent(int i)
    {
        double[] assignments;
        double executionTime;
        switch( i )
        {
            case ( 0 ): //switch part1
            {
                executionTime = time;
                addDelayedEvent( 0 + 1, executionTime, null );
                break;
            }
            case ( 1 ): //switch part2
            {
                assignments = new double[5];
                assignments[0] = -up;
                double piecewise_63 = 0;
                if( NUMERIC_LT( up, 0 ) != 0.0 )
                {
                    piecewise_63 = x_values[1];
                }
                else
                {
                    piecewise_63 = xMin;
                }


                assignments[1] = piecewise_63;
                double piecewise_64 = 0;
                if( NUMERIC_GT( up, 0 ) != 0.0 )
                {
                    piecewise_64 = x_values[1];
                }
                else
                {
                    piecewise_64 = xMax;
                }


                assignments[2] = piecewise_64;
                double piecewise_65 = 0;
                if( NUMERIC_GT( up, 0 ) != 0.0 )
                {
                    piecewise_65 = center - delta;
                }
                else
                {
                    piecewise_65 = center + delta;
                }


                assignments[3] = piecewise_65;
                assignments[4] = 0;
                up = assignments[0];
                xMin = assignments[1];
                xMax = assignments[2];
                target = assignments[3];
                x_values[0] = assignments[4];
                removeDelayedEvent( 1 );
                break;
            }
            default:
                break;
        }
    }


    public final double[] getEventsPriority(double time, double[] x_values) throws Exception
    {
        calculateParameters();
        return new double[] {Double.POSITIVE_INFINITY, //switch part1
                Double.NEGATIVE_INFINITY, //switch part2
        };
    }


    public final boolean getEventsInitialValue(int i) throws IndexOutOfBoundsException
    {
        return true;
    }


    public final boolean isEventTriggerPersistent(int i) throws IndexOutOfBoundsException
    {
        return true;
    }


    public final String getEventMessage(int i) throws IndexOutOfBoundsException
    {
        return null;
    }
}

