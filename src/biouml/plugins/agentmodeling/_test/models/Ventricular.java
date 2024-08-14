package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class Ventricular extends JavaBaseModel
{
    protected double Pressure_Diastole;
    protected double BloodFlow_VentricularToArterial;
    protected double K_2;
    protected double time;
    protected double Duration_Sistole;
    protected double S_3;
    protected double Conductivity_Venous;
    protected double Humoral_Diastole;
    protected double Humoral;
    protected double S_1;
    protected double A_24;
    protected double Elasticity_Myocard;
    protected double Volume_Ventricular_Diastole;
    protected double K_1;
    protected double BloodFlow_VenousToVentricular;
    protected double Volume_Ventricular_N;
    protected double A_29;
    protected double Conductivity_Arterial;
    protected double S_2;
    protected double Pressure_Sistole;
    protected double Pressure_Arterial;
    protected double Stage_Sistole;
    protected double Pressure_Venous;



    @Override
    public double[] dy_dt(double time, double[] x)
    {
        this.time = time;
        final double[] dydt = new double[2];


        BloodFlow_VentricularToArterial = Stage_Sistole * ( Conductivity_Arterial * ( Pressure_Sistole - Pressure_Arterial ) );
        Pressure_Diastole = A_29 * ( ( x[1] - Volume_Ventricular_N ) * ( A_24 * ( x[1] - Volume_Ventricular_N ) + Elasticity_Myocard ) );
        BloodFlow_VenousToVentricular = ( 1 - Stage_Sistole ) * Conductivity_Venous * ( Pressure_Venous - Pressure_Diastole );

        dydt[0] = +1;
        dydt[1] = +BloodFlow_VenousToVentricular - BloodFlow_VentricularToArterial;
        return dydt;
    }

    @Override
    public void init()
    {
        Pressure_Diastole = 0.0; // initial value of Pressure_Diastole
        BloodFlow_VentricularToArterial = 0.0; // initial value of BloodFlow_VentricularToArterial
        K_2 = 20.0; // initial value of K_2
        time = 0.0; // initial value of time
        Duration_Sistole = 0.0; // initial value of Duration_Sistole
        S_3 = 0.2; // initial value of S_3
        Conductivity_Venous = 0.0; // initial value of Conductivity_Venous
        Humoral_Diastole = 1.1; // initial value of Humoral_Diastole
        Humoral = 1.1; // initial value of Humoral
        S_1 = 22.0; // initial value of S_1
        A_24 = 0.01; // initial value of A_24
        Elasticity_Myocard = 0.023; // initial value of Elasticity_Myocard
        Volume_Ventricular_Diastole = 156.0; // initial value of Volume_Ventricular_Diastole
        K_1 = 0.6; // initial value of K_1
        BloodFlow_VenousToVentricular = 0.0; // initial value of BloodFlow_VenousToVentricular
        Volume_Ventricular_N = 120.0; // initial value of Volume_Ventricular_N
        A_29 = 0.0070; // initial value of A_29
        Conductivity_Arterial = 11.7; // initial value of Conductivity_Arterial
        S_2 = 0.25; // initial value of S_2
        Pressure_Sistole = 125.0; // initial value of Pressure_Sistole
        Pressure_Arterial = 0.0; // initial value of Pressure_Arterial
        Stage_Sistole = 1.0; // initial value of Stage_Sistole
        Pressure_Venous = 0.0; // initial value of Pressure_Venous
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
            x[0] = 0.0; //  initial value of Duration_Current
            x[1] = 156.0; //  initial value of Volume_Ventricular

            BloodFlow_VentricularToArterial = Stage_Sistole * ( Conductivity_Arterial * ( Pressure_Sistole - Pressure_Arterial ) );
            Pressure_Diastole = A_29 * ( ( x[1] - Volume_Ventricular_N ) * ( A_24 * ( x[1] - Volume_Ventricular_N ) + Elasticity_Myocard ) );
            BloodFlow_VenousToVentricular = ( 1 - Stage_Sistole ) * Conductivity_Venous * ( Pressure_Venous - Pressure_Diastole );

            return x;
        }
        else
            return initialValues;
    }

    @Override
    public double[] extendResult(double time, double[] x)
    {
        this.time = time;


        BloodFlow_VentricularToArterial = Stage_Sistole * ( Conductivity_Arterial * ( Pressure_Sistole - Pressure_Arterial ) );
        Pressure_Diastole = A_29 * ( ( x[1] - Volume_Ventricular_N ) * ( A_24 * ( x[1] - Volume_Ventricular_N ) + Elasticity_Myocard ) );
        BloodFlow_VenousToVentricular = ( 1 - Stage_Sistole ) * Conductivity_Venous * ( Pressure_Venous - Pressure_Diastole );
        double[] yv74 = new double[25];
        yv74[0] = Pressure_Diastole;
        yv74[1] = BloodFlow_VentricularToArterial;
        yv74[2] = x[0];
        yv74[3] = K_2;
        yv74[4] = time;
        yv74[5] = Duration_Sistole;
        yv74[6] = S_3;
        yv74[7] = Conductivity_Venous;
        yv74[8] = Humoral_Diastole;
        yv74[9] = Humoral;
        yv74[10] = S_1;
        yv74[11] = A_24;
        yv74[12] = Elasticity_Myocard;
        yv74[13] = Volume_Ventricular_Diastole;
        yv74[14] = K_1;
        yv74[15] = BloodFlow_VenousToVentricular;
        yv74[16] = Volume_Ventricular_N;
        yv74[17] = x[1];
        yv74[18] = A_29;
        yv74[19] = Conductivity_Arterial;
        yv74[20] = S_2;
        yv74[21] = Pressure_Sistole;
        yv74[22] = Pressure_Arterial;
        yv74[23] = Stage_Sistole;
        yv74[24] = Pressure_Venous;
        return yv74;
    }

    private double eventTempVariablev72 = 0;
    private double eventTempVariablev73 = 0;

    @Override
    public double[] checkEvent(double time, double[] x)
    {
        this.time = time;


        BloodFlow_VentricularToArterial = Stage_Sistole * ( Conductivity_Arterial * ( Pressure_Sistole - Pressure_Arterial ) );
        Pressure_Diastole = A_29 * ( ( x[1] - Volume_Ventricular_N ) * ( A_24 * ( x[1] - Volume_Ventricular_N ) + Elasticity_Myocard ) );
        BloodFlow_VenousToVentricular = ( 1 - Stage_Sistole ) * Conductivity_Venous * ( Pressure_Venous - Pressure_Diastole );
        double[] flagsv75 = new double[2];
        flagsv75[0] = ( x[1] < ( 1 - K_1 ) * Volume_Ventricular_Diastole + K_2 ) ? +1 : -1; // event: event0;
        flagsv75[1] = ( x[0] > 1.0 / Humoral - Duration_Sistole ) ? +1 : -1; // event: event1;
        return flagsv75;
    }

    public void processEvent(int v76, double time, double[] x)
    {
        this.time = time;
        if( v76 == 0 )
        { // event0
            eventTempVariablev72 = x[0];
            Stage_Sistole = 0;
            Duration_Sistole = eventTempVariablev72;
            x[0] = 0;
        }
        else if( v76 == 1 )
        { // event1
            eventTempVariablev72 = Duration_Sistole;
            Pressure_Sistole = Pressure_Sistole + S_1 * ( eventTempVariablev72 - S_2 / Humoral_Diastole - S_3 * ( 1 - K_1 ) );
            Volume_Ventricular_Diastole = x[1];
            Humoral_Diastole = Humoral;
            Stage_Sistole = 1;
            Duration_Sistole = 0;
            x[0] = 0;
        }
    }
}