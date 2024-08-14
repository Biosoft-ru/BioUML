package biouml.plugins.simulation.java._test;

import biouml.plugins.simulation.java.JavaBaseModel;
import java.util.Map;

public class DelayModel extends JavaBaseModel
{
    public double rate_Reaction_4;
    public double rate_Reaction_5;
    public double RATE_OF_x;
    public double RATE_OF_y;
    public double assignment;
    public double d;
    public double delayed2;
    public double k_1;
    public double unknown2;
     public double[] getY()
     {
         return x_values;
     }


    private void calculateParameters() throws Exception
    {
        double[] x_values = this.x_values;
        unknown2 = k_1*x_values[1];
        RATE_OF_y = -unknown2;
        delayed2 = delay(0, time - (d));
        RATE_OF_x = unknown2 - delayed2;
    }


    private void calculateInitialParameters()
    {
        double[] x_values = this.x_values;
        unknown2 = k_1*x_values[1];
        RATE_OF_y = -unknown2;
        delayed2 = delay(0, time - (d));
        RATE_OF_x = unknown2 - delayed2;
    }
 

    public final double[] dy_dt_slow(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[2];
        calculateParameters();
        dydt[0] = +unknown2 - delayed2;  //  rate rule for of x
        dydt[1] = -unknown2;  //  rate rule for of y
        return dydt;
    }




    @Override
    public final void init() throws Exception
    {
        CONSTRAINTS__VIOLATED = 0;
        this.simulationResultHistory.clear();
        this.simulationResultTimes.clear();
        rate_Reaction_4 = 0.0; // initial value of $$rate_Reaction_4
        rate_Reaction_5 = 0.0; // initial value of $$rate_Reaction_5
        RATE_OF_x = 0.0; // initial value of RATE_OF_x
        RATE_OF_y = 0.0; // initial value of RATE_OF_y
        assignment = 0.0; // initial value of assignment
        d = 100.0; // initial value of d
        delayed2 = 0.0; // initial value of delayed2
        k_1 = 0.01; // initial value of k_1
        time = 0.0; // initial value of time
        unknown2 = 0.0; // initial value of unknown2
        calculateInitialValues();
        this.isInit = true;
    }


    @Override
    public final void init(double[] initialValues, Map<String, Double> parameters) throws Exception
    {
        super.init(initialValues, parameters);
        this.initialValues = x_values.clone();
    }


    private final void calculateInitialValues() throws Exception
    {
        double[] x_values = this.x_values = new double[2];
        this.time = 0.0;
this.initialValues = x_values;
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
        return new double[] {
            rate_Reaction_4,
            rate_Reaction_5,
            d,
            delayed2,
            k_1,
            time,
            unknown2,
            x_values[0],
            x_values[1],
        };
    }


    @Override
    public final void setCurrentValues(double[] values) throws Exception
    {
        CONSTRAINTS__VIOLATED = 0;
        rate_Reaction_4 = values[0];        
        rate_Reaction_5 = values[1];        
        d = values[2];        
        delayed2 = values[3];        
        k_1 = values[4];        
        time = values[5];        
        unknown2 = values[6];        
        x_values[0] = values[7];        
        x_values[1] = values[8];        
        if ( time == 0 )
        {
           initialValues[0] = values[7];        
           initialValues[1] = values[8];        
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
        double[] z  = new double[2];
        z[0] = (NUMERIC_GEQ(time, 1) != 0) ? 1 : -1; // event_1 part1;
        z[1] = (NUMERIC_GEQ(time, 1) != 0) ? 1 : -1; // event_1 part2;
        return z;
    }


    public final void processEvent(int i)
    {
        double[] assignments;
        double executionTime;
        switch ( i )
        {
            case ( 0 ): //event_1 part1
            {
                assignments = new double[2 - 1];
                executionTime = time;
                assignments[1 - 1] = 1000;
                addDelayedEvent(0 + 1, executionTime, assignments);
                break;
            }
            case ( 1 ): //event_1 part2
            {
                assignments = getNextAssignments(1);
                x_values[1] = assignments[0] * 1.0;
                removeDelayedEvent(1);
                break;
            }
            default:
                break;
        }
    }


    public final double[] getEventsPriority(double time, double[] x_values) throws Exception
    {
        calculateParameters();
    return new double[] {
        Double.POSITIVE_INFINITY, //event_1 part1
        Double.NEGATIVE_INFINITY, //event_1 part2
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


    public final double getPrehistory(double time, int i)
    {
        switch (i)
        {
            case 0:
            {
                return k_1*x_values[1];
            }
        }
        return 0;
    }


    public final double[] getCurrentHistory()
    {
        double[] z  = new double[1];
        z[0] = unknown2;
        return z;
    }
}
