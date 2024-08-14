package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class Solodyannikov_for_karaaslan extends JavaBaseModel
{
    protected double Pressure_Diastole;
    protected double Reactivity_HeartCenter;
    protected double Oxygen_UtilizationSpeed;
    protected double Volume_Arterial_N;
    protected double Volume_Venous_N;
    protected double Conductivity_Venous;
    protected double K_2;
    protected double Conductivity_Capillary_0;
    protected double Debt_Capillary;
    protected double A_7;
    protected double Stress_HeartCenter;
    protected double Duration_Sistole;
    protected double S_3;
    protected double Tone_Venous;
    protected double Humoral_Diastole;
    protected double Conductivity_Capillary;
    protected double Elasticity_Venous;
    protected double Oxygen_Need;
    protected double Pressure_0;
    protected double Elasticity_Venous_0;
    protected double Elasticity_Myocard;
    protected double Tone_Arterial;
    protected double Volume_Ventricular_Diastole;
    protected double Oxygen_Arterial;
    protected double K_1;
    protected double Elasticity_Arterial_0;
    protected double Conductivity_Arterial;
    protected double Conductivity_Arterial_Base;
    protected double S_2;
    protected double Volume_Arterial_N0;
    protected double Stage_Sistole;
    protected double Volume_Full;
    protected double BloodFlow_VentricularToArterial;
    protected double A_10;
    protected double Elasticity_Arterial;
    protected double Pressure_Venous;
    protected double BR_HeartCenter;
    protected double Output_Minute;
    protected double Conductivity_Venous_0;
    protected double time;
    protected double Humoral_Arterial;
    protected double S_1;
    protected double Humoral_Capillary;
    protected double Oxygen_Delivery;
    protected double A_24;
    protected double BloodFlow_Capillary;
    protected double Debt_Venous;
    protected double Volume_Venous;
    protected double BloodFlow_VenousToVentricular;
    protected double Volume_Ventricular_N;
    protected double Pressure_Arterial;
    protected double A_18;
    protected double A_29;
    protected double Pressure_Sistole;
    protected double A_6;
    protected double vas;
    protected double eps_aum;
    protected double[] x_values;
    private void calculateScalar()
    {

        Conductivity_Capillary = Conductivity_Capillary_0 + Debt_Capillary * x_values[1] + Humoral_Capillary * x_values[5];
        Elasticity_Venous = Elasticity_Venous_0 + Tone_Venous * x_values[5];
        Volume_Venous = Volume_Full - x_values[4] - x_values[2];
        Pressure_Venous = Elasticity_Venous * ( Volume_Venous - Volume_Venous_N );
        Volume_Arterial_N = Volume_Arterial_N0 + Tone_Arterial * x_values[5];
        Elasticity_Arterial = Elasticity_Arterial_0 + Humoral_Arterial * x_values[5];
        Pressure_Arterial = Elasticity_Arterial * ( x_values[2] - Volume_Arterial_N );
        BloodFlow_Capillary = Conductivity_Capillary * ( Pressure_Arterial - Pressure_Venous );
        Oxygen_Delivery = BloodFlow_Capillary * ( Oxygen_Arterial - x_values[3] );
        Conductivity_Arterial = Conductivity_Arterial_Base * vas / eps_aum;
        BloodFlow_VentricularToArterial = Stage_Sistole * ( Conductivity_Arterial * ( Pressure_Sistole - Pressure_Arterial ) );
        Conductivity_Venous_0 = A_18 - A_10 * Pressure_0;
        Conductivity_Venous = Conductivity_Venous_0 + A_6 * Pressure_Venous + A_7 * Oxygen_Need;
        Pressure_Diastole = A_29
                * ( ( x_values[4] - Volume_Ventricular_N ) * ( A_24 * ( x_values[4] - Volume_Ventricular_N ) + Elasticity_Myocard ) );
        BloodFlow_VenousToVentricular = ( 1 - Stage_Sistole ) * Conductivity_Venous * ( Pressure_Venous - Pressure_Diastole );
    }
    @Override
    public double[] dy_dt(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[8];
        calculateScalar();


        dydt[0] = +1;
        dydt[1] = -Oxygen_UtilizationSpeed * ( Oxygen_Delivery - Oxygen_Need );
        dydt[2] = +BloodFlow_VentricularToArterial - BloodFlow_Capillary;
        dydt[3] = +Debt_Venous * ( Oxygen_Delivery - Oxygen_Need );
        dydt[4] = +BloodFlow_VenousToVentricular - BloodFlow_VentricularToArterial;
        dydt[5] = +Reactivity_HeartCenter
                * ( BR_HeartCenter * BloodFlow_Capillary / x_values[2] - Stress_HeartCenter * ( Pressure_Arterial - Pressure_0 ) - x_values[5] );
        dydt[6] = +BloodFlow_VentricularToArterial;
        dydt[7] = +1;
        return dydt;
    }
    @Override
    public void init() throws Exception
    {
        Pressure_Diastole = 0.0; // initial value of Pressure_Diastole
        Reactivity_HeartCenter = 2.0; // initial value of Reactivity_HeartCenter
        Oxygen_UtilizationSpeed = 0.5; // initial value of Oxygen_UtilizationSpeed
        Volume_Arterial_N = 0.0; // initial value of Volume_Arterial_N
        Volume_Venous_N = 0.2; // initial value of Volume_Venous_N
        Conductivity_Venous = 0.0; // initial value of Conductivity_Venous
        K_2 = 20.0; // initial value of K_2
        Conductivity_Capillary_0 = 0.0733; // initial value of Conductivity_Capillary_0
        Debt_Capillary = 0.00665; // initial value of Debt_Capillary
        A_7 = 4.0; // initial value of A_7
        Stress_HeartCenter = 0.0015; // initial value of Stress_HeartCenter
        Duration_Sistole = 0.0; // initial value of Duration_Sistole
        S_3 = 0.2; // initial value of S_3
        Tone_Venous = 1.85E-6; // initial value of Tone_Venous
        Humoral_Diastole = 1.1; // initial value of Humoral_Diastole
        Conductivity_Capillary = 0.0; // initial value of Conductivity_Capillary
        Elasticity_Venous = 0.0; // initial value of Elasticity_Venous
        Oxygen_Need = 4.0; // initial value of Oxygen_Need
        Pressure_0 = 70.0; // initial value of Pressure_0
        Elasticity_Venous_0 = 6.694E-4; // initial value of Elasticity_Venous_0
        Elasticity_Myocard = 0.023; // initial value of Elasticity_Myocard
        Tone_Arterial = 120.0; // initial value of Tone_Arterial
        Volume_Ventricular_Diastole = 156.0; // initial value of Volume_Ventricular_Diastole
        Oxygen_Arterial = 0.17; // initial value of Oxygen_Arterial
        K_1 = 0.6; // initial value of K_1
        Elasticity_Arterial_0 = 0.35; // initial value of Elasticity_Arterial_0
        Conductivity_Arterial_Base = 11.7; // initial value of Conductivity_Arterial
        Conductivity_Arterial = 11.7; // initial value of Conductivity_Arterial
        S_2 = 0.25; // initial value of S_2
        Volume_Arterial_N0 = 80.0; // initial value of Volume_Arterial_N0
        Stage_Sistole = 1.0; // initial value of Stage_Sistole
        Volume_Full = 3500.0; // initial value of Volume_Full
        BloodFlow_VentricularToArterial = 0.0; // initial value of BloodFlow_VentricularToArterial
        A_10 = 0.8; // initial value of A_10
        Elasticity_Arterial = 0.0; // initial value of Elasticity_Arterial
        Pressure_Venous = 0.0; // initial value of Pressure_Venous
        BR_HeartCenter = 6.0; // initial value of BR_HeartCenter
        Output_Minute = 0.0; // initial value of Output_Minute
        Conductivity_Venous_0 = 0.0; // initial value of Conductivity_Venous_0
        time = 0.0; // initial value of time
        Humoral_Arterial = 0.3; // initial value of Humoral_Arterial
        S_1 = 22.0; // initial value of S_1
        Humoral_Capillary = 0.626; // initial value of Humoral_Capillary
        Oxygen_Delivery = 0.0; // initial value of Oxygen_Delivery
        A_24 = 0.01; // initial value of A_24
        BloodFlow_Capillary = 0.0; // initial value of BloodFlow_Capillary
        Debt_Venous = 8.0E-4; // initial value of Debt_Venous
        Volume_Venous = 0.0; // initial value of Volume_Venous
        BloodFlow_VenousToVentricular = 0.0; // initial value of BloodFlow_VenousToVentricular
        Volume_Ventricular_N = 120.0; // initial value of Volume_Ventricular_N
        Pressure_Arterial = 0.0; // initial value of Pressure_Arterial
        A_18 = 120.0; // initial value of A_18
        A_29 = 0.0070; // initial value of A_29
        Pressure_Sistole = 125.0; // initial value of Pressure_Sistole
        A_6 = 3.3; // initial value of A_6
        vas = 1;
        eps_aum = 1;
        initialValues = getInitialValues();
        this.isInit = true;
    }
    @Override
    public double[] getInitialValues() throws Exception
    {
        if( !this.isInit )
        {
            this.x_values = new double[8];
            this.time = 0.0;
            x_values[0] = 0.0; //  initial value of Duration_Current
            x_values[1] = 3.0E-4; //  initial value of Oxygen_Debt
            x_values[2] = 360.0; //  initial value of Volume_Arterial
            x_values[3] = 0.14; //  initial value of Oxygen_Venous
            x_values[4] = 156.0; //  initial value of Volume_Ventricular
            x_values[5] = 1.1; //  initial value of Humoral
            x_values[6] = 0.0; //  initial value of Cardiac_Output
            x_values[7] = 1.0; //  initial value of Time_Minute
            calculateScalar();

            return x_values;
        }
        else
            return initialValues;
    }
    @Override
    public double[] extendResult(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        calculateScalar();

        double[] yv17 = new double[62];
        yv17[0] = Pressure_Diastole;
        yv17[1] = Reactivity_HeartCenter;
        yv17[2] = Oxygen_UtilizationSpeed;
        yv17[3] = Volume_Arterial_N;
        yv17[4] = x_values[0];
        yv17[5] = Volume_Venous_N;
        yv17[6] = x_values[1];
        yv17[7] = Conductivity_Venous;
        yv17[8] = K_2;
        yv17[9] = x_values[2];
        yv17[10] = Conductivity_Capillary_0;
        yv17[11] = Debt_Capillary;
        yv17[12] = A_7;
        yv17[13] = Stress_HeartCenter;
        yv17[14] = Duration_Sistole;
        yv17[15] = S_3;
        yv17[16] = Tone_Venous;
        yv17[17] = Humoral_Diastole;
        yv17[18] = Conductivity_Capillary;
        yv17[19] = Elasticity_Venous;
        yv17[20] = Oxygen_Need;
        yv17[21] = x_values[3];
        yv17[22] = Pressure_0;
        yv17[23] = Elasticity_Venous_0;
        yv17[24] = Elasticity_Myocard;
        yv17[25] = Tone_Arterial;
        yv17[26] = Volume_Ventricular_Diastole;
        yv17[27] = Oxygen_Arterial;
        yv17[28] = K_1;
        yv17[29] = x_values[4];
        yv17[30] = Elasticity_Arterial_0;
        yv17[31] = Conductivity_Arterial;
        yv17[32] = S_2;
        yv17[33] = Volume_Arterial_N0;
        yv17[34] = Stage_Sistole;
        yv17[35] = Volume_Full;
        yv17[36] = x_values[5];
        yv17[37] = BloodFlow_VentricularToArterial;
        yv17[38] = A_10;
        yv17[39] = Elasticity_Arterial;
        yv17[40] = Pressure_Venous;
        yv17[41] = BR_HeartCenter;
        yv17[42] = Output_Minute;
        yv17[43] = Conductivity_Venous_0;
        yv17[44] = time;
        yv17[45] = Humoral_Arterial;
        yv17[46] = S_1;
        yv17[47] = Humoral_Capillary;
        yv17[48] = Oxygen_Delivery;
        yv17[49] = A_24;
        yv17[50] = BloodFlow_Capillary;
        yv17[51] = Debt_Venous;
        yv17[52] = Volume_Venous;
        yv17[53] = BloodFlow_VenousToVentricular;
        yv17[54] = Volume_Ventricular_N;
        yv17[55] = Pressure_Arterial;
        yv17[56] = x_values[6];
        yv17[57] = A_18;
        yv17[58] = A_29;
        yv17[59] = Pressure_Sistole;
        yv17[60] = x_values[7];
        yv17[61] = A_6;
        return yv17;
    }
    private double eventTempVariablev0 = 0;
    private double eventTempVariablev1 = 0;
    private double eventTempVariablev5 = 0;
    private double eventTempVariablev6 = 0;
    private double eventTempVariablev10 = 0;
    private double eventTempVariablev11 = 0;
    private double eventTempVariablev15 = 0;
    private double eventTempVariablev16 = 0;
    @Override
    public double[] checkEvent(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        calculateScalar();

        double[] flagsv18 = new double[3];
        flagsv18[0] = ( x_values[4] < ( 1 - K_1 ) * Volume_Ventricular_Diastole + K_2 ) ? +1 : -1; // event: event0;
        flagsv18[1] = ( x_values[0] > 1.0 / x_values[5] - Duration_Sistole ) ? +1 : -1; // event: event1;
        flagsv18[2] = ( x_values[7] > 60 ) ? +1 : -1; // event: event2;
        return flagsv18;
    }
    @Override
    public void processEvent(int v19)
    {
        if( v19 == 0 )
        { // event0
            eventTempVariablev0 = x_values[0];
            Stage_Sistole = 0;
            Duration_Sistole = eventTempVariablev0;
            x_values[0] = 0;
        }
        else if( v19 == 1 )
        { // event1
            eventTempVariablev0 = Duration_Sistole;
            Pressure_Sistole = Pressure_Sistole + S_1 * ( eventTempVariablev0 - S_2 / Humoral_Diastole - S_3 * ( 1 - K_1 ) );
            Volume_Ventricular_Diastole = x_values[4];
            Humoral_Diastole = x_values[5];
            Stage_Sistole = 1;
            Duration_Sistole = 0;
            x_values[0] = 0;
        }
        else if( v19 == 2 )
        { // event2
            eventTempVariablev0 = x_values[6];
            x_values[7] = 1;
            Output_Minute = eventTempVariablev0 / 1000;
            x_values[6] = 0;
        }
    }
}