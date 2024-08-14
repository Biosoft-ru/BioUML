package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;
import java.util.Map;

public class LongtermModel extends JavaBaseModel
{
    public double RATE_OF_k;
    public double assignment;
     public double[] getY()
     {
         return x_values;
     }


    private void calculateParameters() throws Exception
    {
        double[] x_values = this.x_values;
        RATE_OF_k = 1.0;
    }


    private void calculateInitialParameters()
    {
        double[] x_values = this.x_values;
        RATE_OF_k = 1.0;
    }
 

    public final double[] dy_dt_slow(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[1];
        calculateParameters();
        dydt[0] = +1.0;  //  rate rule for of k
        return dydt;
    }




    @Override
    public final void init() throws Exception
    {
        CONSTRAINTS__VIOLATED = 0;
        RATE_OF_k = 0.0; // initial value of RATE_OF_k
        assignment = 0.0; // initial value of assignment
        time = 0.0; // initial value of time
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
        double[] x_values = this.x_values = new double[1];
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
        return new double[] {
            x_values[0],
            time,
        };
    }
    
    public String[] getVariables()
    {
        return new String[] {"k", "time"};
    }

    @Override
    public final void setCurrentValues(double[] values) throws Exception
    {
        CONSTRAINTS__VIOLATED = 0;
        x_values[0] = values[0];        
        time = values[1];        
        if ( time == 0 )
        {
           initialValues[0] = values[0];        
           calculateInitialParameters();  
        }
        else
           calculateParameters();
    }
}
