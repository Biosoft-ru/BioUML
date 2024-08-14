package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class ArterialSystem extends JavaBaseModel
{
    protected double Volume_Arterial_N;
    protected double BloodFlow_VentricularToArterial;
    protected double Elasticity_Arterial;
    protected double Tone_Arterial;
    protected double Pressure_Arterial;
    protected double Elasticity_Arterial_0;
    protected double time;
    protected double Humoral_Arterial;
    protected double Volume_Arterial_N0;
    protected double Humoral;
    protected double BloodFlow_Capillary;

    @Override
    public double[] dy_dt(double time, double[] x)
    {
        this.time = time;
        final double[] dydt = new double[1];
        Volume_Arterial_N = Volume_Arterial_N0 + Tone_Arterial * Humoral;
        Elasticity_Arterial = Elasticity_Arterial_0 + Humoral_Arterial * Humoral;
        Pressure_Arterial = Elasticity_Arterial * ( x[0] - Volume_Arterial_N );
        dydt[0] = +BloodFlow_VentricularToArterial - BloodFlow_Capillary;
        return dydt;
    }

    @Override
    public void init()
    {
        Volume_Arterial_N = 0.0; // initial value of Volume_Arterial_N
        BloodFlow_VentricularToArterial = 0.0; // initial value of BloodFlow_VentricularToArterial
        Elasticity_Arterial = 0.0; // initial value of Elasticity_Arterial
        Tone_Arterial = 120.0; // initial value of Tone_Arterial
        Pressure_Arterial = 0.0; // initial value of Pressure_Arterial
        Elasticity_Arterial_0 = 0.35; // initial value of Elasticity_Arterial_0
        time = 0.0; // initial value of time
        Humoral_Arterial = 0.3; // initial value of Humoral_Arterial
        Volume_Arterial_N0 = 80.0; // initial value of Volume_Arterial_N0
        Humoral = 1.1; // initial value of Humoral
        BloodFlow_Capillary = 0.0; // initial value of BloodFlow_Capillary
        initialValues = getInitialValues();
        this.isInit = true;
    }

    @Override
    public double[] getInitialValues()
    {
        double[] x = new double[1];
        this.time = 0.0;
        if( !this.isInit )
        {
            x[0] = 360.0; //  initial value of Volume_Arterial
            Volume_Arterial_N = Volume_Arterial_N0 + Tone_Arterial * Humoral;
            Elasticity_Arterial = Elasticity_Arterial_0 + Humoral_Arterial * Humoral;
            Pressure_Arterial = Elasticity_Arterial * ( x[0] - Volume_Arterial_N );

            return x;
        }
        else
            return initialValues;
    }

    @Override
    public double[] extendResult(double time, double[] x)
    {
        this.time = time;
        Volume_Arterial_N = Volume_Arterial_N0 + Tone_Arterial * Humoral;
        Elasticity_Arterial = Elasticity_Arterial_0 + Humoral_Arterial * Humoral;
        Pressure_Arterial = Elasticity_Arterial * ( x[0] - Volume_Arterial_N );
        double[] yv7 = new double[12];
        yv7[0] = Volume_Arterial_N;
        yv7[1] = BloodFlow_VentricularToArterial;
        yv7[2] = Elasticity_Arterial;
        yv7[3] = Tone_Arterial;
        yv7[4] = Pressure_Arterial;
        yv7[5] = Elasticity_Arterial_0;
        yv7[6] = x[0];
        yv7[7] = time;
        yv7[8] = Humoral_Arterial;
        yv7[9] = Volume_Arterial_N0;
        yv7[10] = Humoral;
        yv7[11] = BloodFlow_Capillary;
        return yv7;
    }

}