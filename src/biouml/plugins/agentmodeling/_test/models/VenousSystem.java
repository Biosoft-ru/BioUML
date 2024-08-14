package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class VenousSystem extends JavaBaseModel
{
    protected double Pressure_0;
    protected double Elasticity_Venous_0;
    protected double A_10;
    protected double Pressure_Venous;
    protected double Volume_Venous;
    protected double Volume_Venous_N;
    protected double Conductivity_Venous_0;
    protected double Conductivity_Venous;
    protected double Volume_Arterial;
    protected double time;
    protected double A_18;
    protected double A_7;
    protected double Volume_Ventricular;
    protected double Tone_Venous;
    protected double Humoral;
    protected double Volume_Full;
    protected double Elasticity_Venous;
    protected double A_6;
    protected double Oxygen_Need;


    @Override
    public double[] getTimes()
    {
        return new double[] {0.0};
    }

    @Override
    public double[] getResults(double time)
    {
        this.time = time;
        return extendResult(0.0, getInitialValues());
    }

    @Override
    public boolean isStatic()
    {
        return true;
    }

    @Override
    public void init()
    {
        Pressure_0 = 70.0; // initial value of Pressure_0
        Elasticity_Venous_0 = 6.69E-4; // initial value of Elasticity_Venous_0
        A_10 = 0.8; // initial value of A_10
        Pressure_Venous = 0.0; // initial value of Pressure_Venous
        Volume_Venous = 0.0; // initial value of Volume_Venous
        Volume_Venous_N = 0.2; // initial value of Volume_Venous_N
        Conductivity_Venous_0 = 0.0; // initial value of Conductivity_Venous_0
        Conductivity_Venous = 0.0; // initial value of Conductivity_Venous
        Volume_Arterial = 360.0; // initial value of Volume_Arterial
        time = 0.0; // initial value of time
        A_18 = 120.0; // initial value of A_18
        A_7 = 4.0; // initial value of A_7
        Volume_Ventricular = 156.0; // initial value of Volume_Ventricular
        Tone_Venous = 1.85E-6; // initial value of Tone_Venous
        Humoral = 1.1; // initial value of Humoral
        Volume_Full = 3500.0; // initial value of Volume_Full
        Elasticity_Venous = 0.0; // initial value of Elasticity_Venous
        A_6 = 3.3; // initial value of A_6
        Oxygen_Need = 4.0; // initial value of Oxygen_Need
        initialValues = getInitialValues();
        this.isInit = true;
    }

    @Override
    public double[] getInitialValues()
    {
        //Degenerate case, when no rate rules specified.

        Elasticity_Venous = Elasticity_Venous_0 + Tone_Venous * Humoral;
        Volume_Venous = Volume_Full - Volume_Ventricular - Volume_Arterial;
        Pressure_Venous = Elasticity_Venous * ( Volume_Venous - Volume_Venous_N );
        Conductivity_Venous_0 = A_18 - A_10 * Pressure_0;
        Conductivity_Venous = Conductivity_Venous_0 + A_6 * Pressure_Venous + A_7 * Oxygen_Need;
        double[] x = new double[] {0};
        return x;
    }

    @Override
    public double[] extendResult(double time, double[] x)
    {
        this.time = time;


        Elasticity_Venous = Elasticity_Venous_0 + Tone_Venous * Humoral;
        Volume_Venous = Volume_Full - Volume_Ventricular - Volume_Arterial;
        Pressure_Venous = Elasticity_Venous * ( Volume_Venous - Volume_Venous_N );
        Conductivity_Venous_0 = A_18 - A_10 * Pressure_0;
        Conductivity_Venous = Conductivity_Venous_0 + A_6 * Pressure_Venous + A_7 * Oxygen_Need;
        double[] yv8 = new double[19];
        yv8[0] = Pressure_0;
        yv8[1] = Elasticity_Venous_0;
        yv8[2] = A_10;
        yv8[3] = Pressure_Venous;
        yv8[4] = Volume_Venous;
        yv8[5] = Volume_Venous_N;
        yv8[6] = Conductivity_Venous_0;
        yv8[7] = Conductivity_Venous;
        yv8[8] = Volume_Arterial;
        yv8[9] = time;
        yv8[10] = A_18;
        yv8[11] = A_7;
        yv8[12] = Volume_Ventricular;
        yv8[13] = Tone_Venous;
        yv8[14] = Humoral;
        yv8[15] = Volume_Full;
        yv8[16] = Elasticity_Venous;
        yv8[17] = A_6;
        yv8[18] = Oxygen_Need;
        return yv8;
    }

}