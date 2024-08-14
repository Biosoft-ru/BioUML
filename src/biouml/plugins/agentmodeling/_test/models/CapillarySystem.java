package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class CapillarySystem extends JavaBaseModel
{
    protected double Debt_Capillary;
    protected double Humoral_Capillary;
    protected double BloodFlow_Capillary;
    protected double Pressure_Arterial;
    protected double Humoral;
    protected double Conductivity_Capillary;
    protected double Conductivity_Capillary_0;
    protected double time;
    protected double Oxygen_Debt;
    protected double Pressure_Venous;


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
        Debt_Capillary = 0.00665; // initial value of Debt_Capillary
        Humoral_Capillary = 0.626; // initial value of Humoral_Capillary
        BloodFlow_Capillary = 0.0; // initial value of BloodFlow_Capillary
        Pressure_Arterial = 0.0; // initial value of Pressure_Arterial
        Humoral = 1.1; // initial value of Humoral
        Conductivity_Capillary = 0.0; // initial value of Conductivity_Capillary
        Conductivity_Capillary_0 = 0.0733; // initial value of Conductivity_Capillary_0
        time = 0.0; // initial value of time
        Oxygen_Debt = 3.0E-4; // initial value of Oxygen_Debt
        Pressure_Venous = 0.0; // initial value of Pressure_Venous
        initialValues = getInitialValues();
        this.isInit = true;
    }

    @Override
    public double[] getInitialValues()
    {
        //Degenerate case, when no rate rules specified.

        Conductivity_Capillary = Conductivity_Capillary_0 + Debt_Capillary * Oxygen_Debt + Humoral_Capillary * Humoral;
        BloodFlow_Capillary = Conductivity_Capillary * ( Pressure_Arterial - Pressure_Venous );
        double[] x = new double[] {0};
        return x;
    }

    @Override
    public double[] extendResult(double time, double[] x)
    {
        this.time = time;


        Conductivity_Capillary = Conductivity_Capillary_0 + Debt_Capillary * Oxygen_Debt + Humoral_Capillary * Humoral;
        BloodFlow_Capillary = Conductivity_Capillary * ( Pressure_Arterial - Pressure_Venous );
        double[] yv63 = new double[10];
        yv63[0] = Debt_Capillary;
        yv63[1] = Humoral_Capillary;
        yv63[2] = BloodFlow_Capillary;
        yv63[3] = Pressure_Arterial;
        yv63[4] = Humoral;
        yv63[5] = Conductivity_Capillary;
        yv63[6] = Conductivity_Capillary_0;
        yv63[7] = time;
        yv63[8] = Oxygen_Debt;
        yv63[9] = Pressure_Venous;
        return yv63;
    }

}