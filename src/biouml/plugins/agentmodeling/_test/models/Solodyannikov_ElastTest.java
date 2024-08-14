package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class Solodyannikov_ElastTest extends JavaBaseModel
{
    protected double Pressure_Diastole;
    protected double Reactivity_HeartCenter;
    protected double Oxygen_UtilizationSpeed;
    protected double Volume_Venous_N;
    protected double Conductivity_Venous;
    protected double K_2;
    protected double Volume_Arterial;
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
    protected double Volume_Ventricular_Diastole;
    protected double Oxygen_Arterial;
    protected double K_1;
    protected double Conductivity_Arterial;
    protected double S_2;
    protected double Cardiac_Output;
    protected double Pressure_Arterial;
    protected double Stage_Sistole;
    protected double Volume_Full;
    protected double BloodFlow_VentricularToArterial;
    protected double A_10;
    protected double Elasticity_Arterial;
    protected double Pressure_Venous;
    protected double BR_HeartCenter;
    protected double kof_Elast;
    protected double Conductivity_Venous_0;
    protected double time;
    protected double Humoral_Arterial;
    protected double S_1;
    protected double BloodFlow_Capillary;
    protected double Humoral_Capillary;
    protected double Oxygen_Delivery;
    protected double A_24;
    protected double Debt_Venous;
    protected double Volume_Venous;
    protected double BloodFlow_VenousToVentricular;
    protected double Volume_Ventricular_N;
    protected double A_18;
    protected double A_29;
    protected double Pressure_Sistole;
    protected double A_6;
    protected double[] x_values;
    private void calculateScalar()
    {
        
        Oxygen_Delivery = BloodFlow_Capillary*(Oxygen_Arterial - x_values[3]);
        BloodFlow_VentricularToArterial = Stage_Sistole*(Conductivity_Arterial*(Pressure_Sistole - Pressure_Arterial));
        Elasticity_Venous = Elasticity_Venous_0 + Tone_Venous*x_values[5];
        Volume_Venous = Volume_Full - x_values[4] - Volume_Arterial;
        Pressure_Venous = Elasticity_Venous*(Volume_Venous - Volume_Venous_N);
        Conductivity_Venous_0 = A_18 - A_10*Pressure_0;
        Conductivity_Venous = Conductivity_Venous_0 + A_6*Pressure_Venous + A_7*Oxygen_Need;
        Pressure_Diastole = A_29*((x_values[4] - Volume_Ventricular_N)*(A_24*(x_values[4] - Volume_Ventricular_N) + Elasticity_Myocard));
        BloodFlow_VenousToVentricular = (1 - Stage_Sistole)*Conductivity_Venous*(Pressure_Venous - Pressure_Diastole);
        Conductivity_Capillary = Conductivity_Capillary_0 + Debt_Capillary*x_values[2] + Humoral_Capillary*x_values[5];
        Elasticity_Arterial = x_values[1] + Humoral_Arterial*x_values[5];
        double piecewise_1 = 0;
if (time < 9300) {
    piecewise_1 = 0.0;
}
else if (time >= 9300 && time < 9600) {
    piecewise_1 = 0.0020;
}
else {
    piecewise_1 = 0.0;
}


        kof_Elast = piecewise_1;
    }
      @Override
    public double[] dy_dt(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[8];
        calculateScalar();
        
        
        dydt[0] = +1;
        dydt[1] = +kof_Elast;
        dydt[2] = -Oxygen_UtilizationSpeed*(Oxygen_Delivery - Oxygen_Need);
        dydt[3] = +Debt_Venous*(Oxygen_Delivery - Oxygen_Need);
        dydt[4] = +BloodFlow_VenousToVentricular - BloodFlow_VentricularToArterial;
        dydt[5] = +Reactivity_HeartCenter*(BR_HeartCenter*BloodFlow_Capillary/Volume_Arterial - Stress_HeartCenter*(Pressure_Arterial - Pressure_0) - x_values[5]);
        dydt[6] = +1;
        dydt[7] = +BloodFlow_VentricularToArterial/1300;
        return dydt;
    }
    @Override
    public void init() throws Exception
    {
        Pressure_Diastole = 0.0; // initial value of Pressure_Diastole
        Reactivity_HeartCenter = 2.0; // initial value of Reactivity_HeartCenter
        Oxygen_UtilizationSpeed = 0.5; // initial value of Oxygen_UtilizationSpeed
        Volume_Venous_N = 0.2; // initial value of Volume_Venous_N
        Conductivity_Venous = 0.0; // initial value of Conductivity_Venous
        K_2 = 20.0; // initial value of K_2
        Volume_Arterial = 360.0; // initial value of Volume_Arterial
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
        Pressure_0 = 80.0; // initial value of Pressure_0
        Elasticity_Venous_0 = 6.694E-4; // initial value of Elasticity_Venous_0
        Elasticity_Myocard = 0.023; // initial value of Elasticity_Myocard
        Volume_Ventricular_Diastole = 156.0; // initial value of Volume_Ventricular_Diastole
        Oxygen_Arterial = 0.17; // initial value of Oxygen_Arterial
        K_1 = 0.6; // initial value of K_1
        Conductivity_Arterial = 11.7; // initial value of Conductivity_Arterial
        S_2 = 0.25; // initial value of S_2
        Cardiac_Output = 0.0; // initial value of Cardiac_Output
        Pressure_Arterial = 0.0; // initial value of Pressure_Arterial
        Stage_Sistole = 1.0; // initial value of Stage_Sistole
        Volume_Full = 3500.0; // initial value of Volume_Full
        BloodFlow_VentricularToArterial = 0.0; // initial value of BloodFlow_VentricularToArterial
        A_10 = 0.8; // initial value of A_10
        Elasticity_Arterial = 0.0; // initial value of Elasticity_Arterial
        Pressure_Venous = 0.0; // initial value of Pressure_Venous
        BR_HeartCenter = 6.0; // initial value of BR_HeartCenter
        kof_Elast = 0.0; // initial value of kof_Elast
        Conductivity_Venous_0 = 0.0; // initial value of Conductivity_Venous_0
        time = 0.0; // initial value of time
        Humoral_Arterial = 0.3; // initial value of Humoral_Arterial
        S_1 = 22.0; // initial value of S_1
        BloodFlow_Capillary = 0.0; // initial value of BloodFlow_Capillary
        Humoral_Capillary = 0.626; // initial value of Humoral_Capillary
        Oxygen_Delivery = 0.0; // initial value of Oxygen_Delivery
        A_24 = 0.01; // initial value of A_24
        Debt_Venous = 8.0E-4; // initial value of Debt_Venous
        Volume_Venous = 0.0; // initial value of Volume_Venous
        BloodFlow_VenousToVentricular = 0.0; // initial value of BloodFlow_VenousToVentricular
        Volume_Ventricular_N = 120.0; // initial value of Volume_Ventricular_N
        A_18 = 120.0; // initial value of A_18
        A_29 = 0.0070; // initial value of A_29
        Pressure_Sistole = 125.0; // initial value of Pressure_Sistole
        A_6 = 3.3; // initial value of A_6
        initialValues = getInitialValues();
        this.isInit = true;
    }
    @Override
    public double[] getInitialValues() throws Exception
    {
       if (!this.isInit)
       {
            this.x_values = new double[8];
            this.time = 0.0;
            x_values[0] = 0.0; //  initial value of Duration_Current
            x_values[1] = 0.35; //  initial value of Elasticity_Arterial_0
            x_values[2] = 3.0E-4; //  initial value of Oxygen_Debt
            x_values[3] = 0.14; //  initial value of Oxygen_Venous
            x_values[4] = 156.0; //  initial value of Volume_Ventricular
            x_values[5] = 1.1; //  initial value of Humoral
            x_values[6] = 0.0; //  initial value of time_min
            x_values[7] = 0.0; //  initial value of BloodFlowToArterial_min
            calculateScalar();
            
            return x_values;
        }
        else return initialValues;
    }
    @Override
    public double[] extendResult(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        calculateScalar();
        
        double[] yv7 = new double[60];
        yv7[0] = Pressure_Diastole;
        yv7[1] = Reactivity_HeartCenter;
        yv7[2] = Oxygen_UtilizationSpeed;
        yv7[3] = x_values[0];
        yv7[4] = x_values[1];
        yv7[5] = Volume_Venous_N;
        yv7[6] = x_values[2];
        yv7[7] = Conductivity_Venous;
        yv7[8] = K_2;
        yv7[9] = Volume_Arterial;
        yv7[10] = Conductivity_Capillary_0;
        yv7[11] = Debt_Capillary;
        yv7[12] = A_7;
        yv7[13] = Stress_HeartCenter;
        yv7[14] = Duration_Sistole;
        yv7[15] = S_3;
        yv7[16] = Tone_Venous;
        yv7[17] = Humoral_Diastole;
        yv7[18] = Conductivity_Capillary;
        yv7[19] = Elasticity_Venous;
        yv7[20] = Oxygen_Need;
        yv7[21] = x_values[3];
        yv7[22] = Pressure_0;
        yv7[23] = Elasticity_Venous_0;
        yv7[24] = Elasticity_Myocard;
        yv7[25] = Volume_Ventricular_Diastole;
        yv7[26] = Oxygen_Arterial;
        yv7[27] = K_1;
        yv7[28] = x_values[4];
        yv7[29] = Conductivity_Arterial;
        yv7[30] = S_2;
        yv7[31] = Cardiac_Output;
        yv7[32] = Pressure_Arterial;
        yv7[33] = Stage_Sistole;
        yv7[34] = Volume_Full;
        yv7[35] = x_values[5];
        yv7[36] = BloodFlow_VentricularToArterial;
        yv7[37] = A_10;
        yv7[38] = Elasticity_Arterial;
        yv7[39] = Pressure_Venous;
        yv7[40] = BR_HeartCenter;
        yv7[41] = kof_Elast;
        yv7[42] = Conductivity_Venous_0;
        yv7[43] = time;
        yv7[44] = Humoral_Arterial;
        yv7[45] = x_values[6];
        yv7[46] = S_1;
        yv7[47] = BloodFlow_Capillary;
        yv7[48] = Humoral_Capillary;
        yv7[49] = Oxygen_Delivery;
        yv7[50] = A_24;
        yv7[51] = Debt_Venous;
        yv7[52] = Volume_Venous;
        yv7[53] = x_values[7];
        yv7[54] = BloodFlow_VenousToVentricular;
        yv7[55] = Volume_Ventricular_N;
        yv7[56] = A_18;
        yv7[57] = A_29;
        yv7[58] = Pressure_Sistole;
        yv7[59] = A_6;
        return yv7;
    }
    private double eventTempVariablev5 = 0;
    private double eventTempVariablev6 = 0;
    @Override
    public double[] checkEvent(double time, double[] x_values) throws Exception
    {
       this.time = time;
       this.x_values = x_values;
       calculateScalar();
       
       double [] flagsv8  = new double[3];
        flagsv8[0] = (x_values[4] < (1 - K_1)*Volume_Ventricular_Diastole + K_2) ? +1 : -1; // event: event0;
        flagsv8[1] = (x_values[0] > 1.0/x_values[5] - Duration_Sistole) ? +1 : -1; // event: event1;
        flagsv8[2] = (x_values[6] > 60) ? +1 : -1; // event: event2;
        return flagsv8;
    }
    public void processEvent(int v9, double time, double[] x_values)
    {
        this.time = time;
        this.x_values = x_values;
        if (v9 == 0)
        {  // event0
            eventTempVariablev5 = x_values[0];
            Stage_Sistole = 0;
            Duration_Sistole = eventTempVariablev5;
            x_values[0] = 0;
        }
        else if (v9 == 1)
        {  // event1
            eventTempVariablev5 = Duration_Sistole;
            Pressure_Sistole = Pressure_Sistole + S_1*(eventTempVariablev5 - S_2/Humoral_Diastole - S_3*(1 - K_1));
            Volume_Ventricular_Diastole = x_values[4];
            Humoral_Diastole = x_values[5];
            Stage_Sistole = 1;
            Duration_Sistole = 0;
            x_values[0] = 0;
        }
        else if (v9 == 2)
        {  // event2
            eventTempVariablev5 = x_values[7];
            x_values[6] = 0;
            Cardiac_Output = eventTempVariablev5;
            x_values[7] = 0;
        }
   }   }
