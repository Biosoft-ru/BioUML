package biouml.plugins.pharm._test;

import biouml.plugins.simulation.java.JavaBaseModel;
import java.util.Map;
public class IndomethPK extends JavaBaseModel
{
    public double start;
    public double unknown;
    public double rate_reaction_2;
    public double k12;
    public double k21;
    public double rate_reaction;
    public double k10;
    public double rate_reaction_1;


    private void calculateReactionRates()
    {
        rate_reaction = k12*x_values[1];
        rate_reaction_1 = k10*x_values[1];
        rate_reaction_2 = k21*x_values[0];
    }
 

    @Override
    public double[] dy_dt(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[2];  
        calculateReactionRates();
        dydt[0] = +rate_reaction-rate_reaction_2;
        dydt[1] = -rate_reaction-rate_reaction_1+rate_reaction_2;
        return dydt;
    }


    @Override
    public void init() throws Exception
    {
        start = 0.0; // initial value of start
        unknown = 0.0; // initial value of unknown
        rate_reaction_2 = 0.0; // initial value of $$rate_reaction_2
        k12 = 0.951; // initial value of k12
        k21 = 0.0; // initial value of k21
        rate_reaction = 0.0; // initial value of $$rate_reaction
        time = 0.0; // initial value of time
        k10 = 0.904; // initial value of k10
        rate_reaction_1 = 0.0; // initial value of $$rate_reaction_1
        initialValues = getInitialValues();
        this.isInit = true; 
    }


    @Override
    public void init(double[] initialValues, Map<String, Double> parameters) throws Exception
    {
         super.init(initialValues, parameters);
         x_values[1] = start;
        this.initialValues = x_values.clone();
    }


    @Override
    public double[] getInitialValues() throws Exception
    {
       if (!this.isInit)
       {
            this.x_values = new double[2];
            this.time = 0.0;
            x_values[0] = 0.0; //  initial value of $y2
            x_values[1] = 0.0; //  initial value of $y1
      x_values[1] = start;
            return x_values;
        }
        else return initialValues;
    }  


    @Override
    public double[] extendResult(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        double[] y = new double[8];
        y[0] = start;
        y[1] = unknown;
        y[2] = k12;
        y[3] = k21;
        y[4] = x_values[0];
        y[5] = time;
        y[6] = x_values[1];
        y[7] = k10;
        return y;
    }


    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        start = values[0];
        unknown = values[1];
        rate_reaction_2 = values[1];
        k12 = values[2];
        k21 = values[3];
        rate_reaction = values[3];
        x_values[0] = values[4];
        time = values[5];
        x_values[1] = values[6];
        k10 = values[7];
        rate_reaction_1 = values[7];
    }
 }