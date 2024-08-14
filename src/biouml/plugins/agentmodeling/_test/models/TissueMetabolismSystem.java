package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class TissueMetabolismSystem extends JavaBaseModel
{
    protected double Oxygen_UtilizationSpeed;
    protected double Oxygen_Delivery;
    protected double Debt_Venous;
    protected double Oxygen_Arterial;
    protected double BloodFlow_Capillary;
    protected double time;
    protected double Oxygen_Need;

    @Override
    public double[] dy_dt(double time, double[] x)
    {
        this.time = time;
        final double[] dydt = new double[2];
        Oxygen_Delivery = BloodFlow_Capillary * ( Oxygen_Arterial - x[0] );
        dydt[0] = +Debt_Venous * ( Oxygen_Delivery - Oxygen_Need );
        dydt[1] = -Oxygen_UtilizationSpeed * ( Oxygen_Delivery - Oxygen_Need );
        return dydt;
    }

    @Override
    public void init()
    {
        Oxygen_UtilizationSpeed = 0.5; // initial value of Oxygen_UtilizationSpeed
        Oxygen_Delivery = 0.0; // initial value of Oxygen_Delivery
        Debt_Venous = 8.0E-4; // initial value of Debt_Venous
        Oxygen_Arterial = 0.17; // initial value of Oxygen_Arterial
        BloodFlow_Capillary = 0.0; // initial value of BloodFlow_Capillary
        time = 0.0; // initial value of time
        Oxygen_Need = 4.0; // initial value of Oxygen_Need
        initialValues = getInitialValues();
        this.isInit = true;
    }

    @Override
    public double[] getInitialValues()
    {
        double[] x = new double[2];
        this.time = 0.0;
        if( !this.isInit )
        {
            x[0] = 0.14; //  initial value of Oxygen_Venous
            x[1] = 3.0E-4; //  initial value of Oxygen_Debt

            Oxygen_Delivery = BloodFlow_Capillary * ( Oxygen_Arterial - x[0] );

            return x;
        }
        else
            return initialValues;
    }

    @Override
    public double[] extendResult(double time, double[] x)
    {
        this.time = time;


        Oxygen_Delivery = BloodFlow_Capillary * ( Oxygen_Arterial - x[0] );
        double[] yv70 = new double[9];
        yv70[0] = Oxygen_UtilizationSpeed;
        yv70[1] = x[0];
        yv70[2] = Oxygen_Delivery;
        yv70[3] = Debt_Venous;
        yv70[4] = Oxygen_Arterial;
        yv70[5] = x[1];
        yv70[6] = BloodFlow_Capillary;
        yv70[7] = time;
        yv70[8] = Oxygen_Need;
        return yv70;
    }

}