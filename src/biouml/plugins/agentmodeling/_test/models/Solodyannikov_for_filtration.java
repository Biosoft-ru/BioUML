package biouml.plugins.agentmodeling._test.models;
import biouml.plugins.simulation.java.JavaBaseModel;
public class Solodyannikov_for_filtration extends JavaBaseModel
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
    protected double Pressure_Arterial;
    protected double Stage_Sistole;
    protected double Volume_Full;
    protected double BloodFlow_VentricularToArterial;
    protected double A_10;
    protected double Pressure_Venous;
    protected double BR_HeartCenter;
    protected double Conductivity_Venous_0;
    protected double time;
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
        
        Oxygen_Delivery = BloodFlow_Capillary*(Oxygen_Arterial - x_values[2]);
        BloodFlow_VentricularToArterial = Stage_Sistole*(Conductivity_Arterial*(Pressure_Sistole - Pressure_Arterial));
        Elasticity_Venous = Elasticity_Venous_0 + Tone_Venous*x_values[4];
        Volume_Venous = Volume_Full - x_values[3] - Volume_Arterial;
        Pressure_Venous = Elasticity_Venous*(Volume_Venous - Volume_Venous_N);
        Conductivity_Venous_0 = A_18 - A_10*Pressure_0;
        Conductivity_Venous = Conductivity_Venous_0 + A_6*Pressure_Venous + A_7*Oxygen_Need;
        Pressure_Diastole = A_29*((x_values[3] - Volume_Ventricular_N)*(A_24*(x_values[3] - Volume_Ventricular_N) + Elasticity_Myocard));
        BloodFlow_VenousToVentricular = (1 - Stage_Sistole)*Conductivity_Venous*(Pressure_Venous - Pressure_Diastole);
        Conductivity_Capillary = Conductivity_Capillary_0 + Debt_Capillary*x_values[1] + Humoral_Capillary*x_values[4];
    }
      @Override
    public double[] dy_dt(double time, double[] x_values)
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[5];
        
        calculateScalar();
        
        dydt[0] = +1;
        dydt[1] = -Oxygen_UtilizationSpeed*(Oxygen_Delivery - Oxygen_Need);
        dydt[2] = +Debt_Venous*(Oxygen_Delivery - Oxygen_Need);
        dydt[3] = +BloodFlow_VenousToVentricular - BloodFlow_VentricularToArterial;
        dydt[4] = +Reactivity_HeartCenter*(BR_HeartCenter*BloodFlow_Capillary/Volume_Arterial - Stress_HeartCenter*(Pressure_Arterial - Pressure_0) - x_values[4]);
        return dydt;
    }
    @Override
    public void init()
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
        Pressure_0 = 85.0; // initial value of Pressure_0
        Elasticity_Venous_0 = 6.694E-4; // initial value of Elasticity_Venous_0
        Elasticity_Myocard = 0.023; // initial value of Elasticity_Myocard
        Volume_Ventricular_Diastole = 156.0; // initial value of Volume_Ventricular_Diastole
        Oxygen_Arterial = 0.17; // initial value of Oxygen_Arterial
        K_1 = 0.6; // initial value of K_1
        Conductivity_Arterial = 11.7; // initial value of Conductivity_Arterial
        S_2 = 0.25; // initial value of S_2
        Pressure_Arterial = 0.0; // initial value of Pressure_Arterial
        Stage_Sistole = 1.0; // initial value of Stage_Sistole
        Volume_Full = 3500.0; // initial value of Volume_Full
        BloodFlow_VentricularToArterial = 0.0; // initial value of BloodFlow_VentricularToArterial
        A_10 = 0.8; // initial value of A_10
        Pressure_Venous = 0.0; // initial value of Pressure_Venous
        BR_HeartCenter = 6.0; // initial value of BR_HeartCenter
        Conductivity_Venous_0 = 0.0; // initial value of Conductivity_Venous_0
        time = 0.0; // initial value of time
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
    public double[] getInitialValues()
    {
       if (!this.isInit)
       {
            this.x_values = new double[5];
            this.time = 0.0;
            x_values[0] = 0.0; //  initial value of Duration_Current
            x_values[1] = 3.0E-4; //  initial value of Oxygen_Debt
            x_values[2] = 0.14; //  initial value of Oxygen_Venous
            x_values[3] = 156.0; //  initial value of Volume_Ventricular
            x_values[4] = 1.1; //  initial value of Humoral
            calculateScalar();
            
            return x_values;
        }
        else return initialValues;
    }
    @Override
    public double[] extendResult(double time, double[] x_values)
    {
        this.time = time;
        this.x_values = x_values;
        
        calculateScalar();
        double[] yv8 = new double[53];
        yv8[0] = Pressure_Diastole;
        yv8[1] = Reactivity_HeartCenter;
        yv8[2] = Oxygen_UtilizationSpeed;
        yv8[3] = x_values[0];
        yv8[4] = Volume_Venous_N;
        yv8[5] = x_values[1];
        yv8[6] = Conductivity_Venous;
        yv8[7] = K_2;
        yv8[8] = Volume_Arterial;
        yv8[9] = Conductivity_Capillary_0;
        yv8[10] = Debt_Capillary;
        yv8[11] = A_7;
        yv8[12] = Stress_HeartCenter;
        yv8[13] = Duration_Sistole;
        yv8[14] = S_3;
        yv8[15] = Tone_Venous;
        yv8[16] = Humoral_Diastole;
        yv8[17] = Conductivity_Capillary;
        yv8[18] = Elasticity_Venous;
        yv8[19] = Oxygen_Need;
        yv8[20] = x_values[2];
        yv8[21] = Pressure_0;
        yv8[22] = Elasticity_Venous_0;
        yv8[23] = Elasticity_Myocard;
        yv8[24] = Volume_Ventricular_Diastole;
        yv8[25] = Oxygen_Arterial;
        yv8[26] = K_1;
        yv8[27] = x_values[3];
        yv8[28] = Conductivity_Arterial;
        yv8[29] = S_2;
        yv8[30] = Pressure_Arterial;
        yv8[31] = Stage_Sistole;
        yv8[32] = Volume_Full;
        yv8[33] = x_values[4];
        yv8[34] = BloodFlow_VentricularToArterial;
        yv8[35] = A_10;
        yv8[36] = Pressure_Venous;
        yv8[37] = BR_HeartCenter;
        yv8[38] = Conductivity_Venous_0;
        yv8[39] = time;
        yv8[40] = S_1;
        yv8[41] = BloodFlow_Capillary;
        yv8[42] = Humoral_Capillary;
        yv8[43] = Oxygen_Delivery;
        yv8[44] = A_24;
        yv8[45] = Debt_Venous;
        yv8[46] = Volume_Venous;
        yv8[47] = BloodFlow_VenousToVentricular;
        yv8[48] = Volume_Ventricular_N;
        yv8[49] = A_18;
        yv8[50] = A_29;
        yv8[51] = Pressure_Sistole;
        yv8[52] = A_6;
        return yv8;
    }
    private double eventTempVariablev6 = 0;
    private double eventTempVariablev7 = 0;
    @Override
    public double[] checkEvent(double time, double[] x_values)
    {
       this.time = time;
       this.x_values = x_values;
       
       calculateScalar();
       double [] flagsv9  = new double[2];
        flagsv9[0] = (x_values[3] < (1 - K_1)*Volume_Ventricular_Diastole + K_2) ? +1 : -1; // event: event0;
        flagsv9[1] = (x_values[0] > 1.0/x_values[4] - Duration_Sistole) ? +1 : -1; // event: event1;
        return flagsv9;
    }
    @Override
    public void processEvent(int v10)
    {
        if (v10 == 0)
        {  // event0
            eventTempVariablev6 = x_values[0];
            Stage_Sistole = 0;
            Duration_Sistole = eventTempVariablev6;
            x_values[0] = 0;
        }
        else if (v10 == 1)
        {  // event1
            eventTempVariablev6 = Duration_Sistole;
            Pressure_Sistole = Pressure_Sistole + S_1*(eventTempVariablev6 - S_2/Humoral_Diastole - S_3*(1 - K_1));
            Volume_Ventricular_Diastole = x_values[3];
            Humoral_Diastole = x_values[4];
            Stage_Sistole = 1;
            Duration_Sistole = 0;
            x_values[0] = 0;
        }
   }   }