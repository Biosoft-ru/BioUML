package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class Solodyannikov extends JavaBaseModel
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
    protected double CardiacOutput;
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
    protected double S_2;
    protected double Volume_Arterial_N0;
    protected double Stage_Sistole;
    protected double Volume_Full;
    protected double BloodFlow_VentricularToArterial;
    protected double A_10;
    protected double Elasticity_Arterial;
    protected double Pressure_Venous;
    protected double BR_HeartCenter;
    protected double Conductivity_Venous_0;
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
    protected double CardiacOutput_Minute;
    protected double A_6;
    protected double Pulse;
    protected double Pulse_Minute;

    private void calculateParameters() throws Exception
    {
        Conductivity_Capillary = Conductivity_Capillary_0 + Debt_Capillary * x_values[1] + Humoral_Capillary * x_values[6];
        Elasticity_Venous = Elasticity_Venous_0 + Tone_Venous * x_values[6];
        Volume_Venous = Volume_Full - x_values[4] - x_values[2];
        Pressure_Venous = Elasticity_Venous * ( Volume_Venous - Volume_Venous_N );
        Volume_Arterial_N = Volume_Arterial_N0 + Tone_Arterial * x_values[6];
        Elasticity_Arterial = Elasticity_Arterial_0 + Humoral_Arterial * x_values[6];
        Pressure_Arterial = Elasticity_Arterial * ( x_values[2] - Volume_Arterial_N );
        BloodFlow_Capillary = Conductivity_Capillary * ( Pressure_Arterial - Pressure_Venous );
        Oxygen_Delivery = BloodFlow_Capillary * ( Oxygen_Arterial - x_values[3] );
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
        final double[] dydt = new double[7];
        calculateParameters();
        dydt[0] = +1;
        dydt[1] = -Oxygen_UtilizationSpeed * ( Oxygen_Delivery - Oxygen_Need );
        dydt[2] = +BloodFlow_VentricularToArterial - BloodFlow_Capillary;
        dydt[3] = +Debt_Venous * ( Oxygen_Delivery - Oxygen_Need );
        dydt[4] = +BloodFlow_VenousToVentricular - BloodFlow_VentricularToArterial;
        dydt[5] = +1;
        dydt[6] = +Reactivity_HeartCenter
                * ( BR_HeartCenter * BloodFlow_Capillary / x_values[2] - Stress_HeartCenter * ( Pressure_Arterial - Pressure_0 ) - x_values[6] );
        return dydt;
    }


    @Override
    public void init() throws Exception
    {
        super.init();
        Pressure_Diastole = 0.096516; // initial value of $varName
        Reactivity_HeartCenter = 2.0; // initial value of $varName
        Oxygen_UtilizationSpeed = 0.5; // initial value of $varName
        Volume_Arterial_N = 212.0; // initial value of $varName
        Volume_Venous_N = 0.2; // initial value of $varName
        Conductivity_Venous = 86.6113115849; // initial value of $varName
        K_2 = 20.0; // initial value of $varName
        Conductivity_Capillary_0 = 0.0733; // initial value of $varName
        Debt_Capillary = 0.00665; // initial value of $varName
        A_7 = 4.0; // initial value of $varName
        Stress_HeartCenter = 0.0015; // initial value of $varName
        Duration_Sistole = 0.0; // initial value of $varName
        S_3 = 0.2; // initial value of $varName
        Tone_Venous = 1.85E-6; // initial value of $varName
        Humoral_Diastole = 1.1; // initial value of $varName
        CardiacOutput = 0.0; // initial value of $varName
        Conductivity_Capillary = 0.7619019950000001; // initial value of $varName
        Elasticity_Venous = 6.71435E-4; // initial value of $varName
        Oxygen_Need = 4.0; // initial value of $varName
        Pressure_0 = 70.0; // initial value of $varName
        Elasticity_Venous_0 = 6.694E-4; // initial value of $varName
        Elasticity_Myocard = 0.023; // initial value of $varName
        Tone_Arterial = 120.0; // initial value of $varName
        Volume_Ventricular_Diastole = 156.0; // initial value of $varName
        Oxygen_Arterial = 0.17; // initial value of $varName
        K_1 = 0.6; // initial value of $varName
        Elasticity_Arterial_0 = 0.35; // initial value of $varName
        Conductivity_Arterial = 11.7; // initial value of $varName
        S_2 = 0.25; // initial value of $varName
        Volume_Arterial_N0 = 80.0; // initial value of $varName
        Stage_Sistole = 1.0; // initial value of $varName
        Volume_Full = 3500.0; // initial value of $varName
        BloodFlow_VentricularToArterial = 285.0120000000001; // initial value of $varName
        A_10 = 0.8; // initial value of $varName
        Elasticity_Arterial = 0.6799999999999999; // initial value of $varName
        Pressure_Venous = 2.003427753; // initial value of $varName
        BR_HeartCenter = 6.0; // initial value of $varName
        Conductivity_Venous_0 = 64.0; // initial value of $varName
        time = 0.0; // initial value of $varName
        Humoral_Arterial = 0.3; // initial value of $varName
        S_1 = 22.0; // initial value of $varName
        Humoral_Capillary = 0.626; // initial value of $varName
        Oxygen_Delivery = 2.254542035248528; // initial value of $varName
        A_24 = 0.01; // initial value of $varName
        BloodFlow_Capillary = 75.15140117495093; // initial value of $varName
        Debt_Venous = 8.0E-4; // initial value of $varName
        Volume_Venous = 2984.0; // initial value of $varName
        BloodFlow_VenousToVentricular = 0.0; // initial value of $varName
        Volume_Ventricular_N = 120.0; // initial value of $varName
        Pressure_Arterial = 100.63999999999999; // initial value of $varName
        A_18 = 120.0; // initial value of $varName
        A_29 = 0.0070; // initial value of $varName
        Pressure_Sistole = 125.0; // initial value of $varName
        CardiacOutput_Minute = 0.0; // initial value of $varName
        A_6 = 3.3; // initial value of $varName
        Pulse = 0;
        Pulse_Minute = 90;
        initialValues = getInitialValues();
        this.isInit = true;
    }


    @Override
    public double[] getInitialValues() throws Exception
    {
        if( !this.isInit )
        {
            this.x_values = new double[7];
            this.time = 0.0;
            x_values[0] = 0.0; //  initial value of Duration_Current
            x_values[1] = 3.0E-4; //  initial value of Oxygen_Debt
            x_values[2] = 360.0; //  initial value of Volume_Arterial
            x_values[3] = 0.14; //  initial value of Oxygen_Venous
            x_values[4] = 156.0; //  initial value of Volume_Ventricular
            x_values[5] = 0.0; //  initial value of minute
            x_values[6] = 1.1; //  initial value of Humoral
            calculateParameters();
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
        calculateParameters();
        double[] y = new double[62];
        y[0] = Pressure_Diastole;
        y[1] = Reactivity_HeartCenter;
        y[2] = Oxygen_UtilizationSpeed;
        y[3] = Volume_Arterial_N;
        y[4] = x_values[0];
        y[5] = Volume_Venous_N;
        y[6] = x_values[1];
        y[7] = Conductivity_Venous;
        y[8] = K_2;
        y[9] = x_values[2];
        y[10] = Conductivity_Capillary_0;
        y[11] = Debt_Capillary;
        y[12] = A_7;
        y[13] = Stress_HeartCenter;
        y[14] = Duration_Sistole;
        y[15] = S_3;
        y[16] = Tone_Venous;
        y[17] = Humoral_Diastole;
        y[18] = CardiacOutput;
        y[19] = Conductivity_Capillary;
        y[20] = Elasticity_Venous;
        y[21] = Oxygen_Need;
        y[22] = x_values[3];
        y[23] = Pressure_0;
        y[24] = Elasticity_Venous_0;
        y[25] = Elasticity_Myocard;
        y[26] = Tone_Arterial;
        y[27] = Volume_Ventricular_Diastole;
        y[28] = Oxygen_Arterial;
        y[29] = K_1;
        y[30] = x_values[4];
        y[31] = Elasticity_Arterial_0;
        y[32] = Conductivity_Arterial;
        y[33] = S_2;
        y[34] = x_values[5];
        y[35] = Volume_Arterial_N0;
        y[36] = Stage_Sistole;
        y[37] = Volume_Full;
        y[38] = x_values[6];
        y[39] = BloodFlow_VentricularToArterial;
        y[40] = A_10;
        y[41] = Elasticity_Arterial;
        y[42] = Pressure_Venous;
        y[43] = BR_HeartCenter;
        y[44] = Conductivity_Venous_0;
        y[45] = time;
        y[46] = Humoral_Arterial;
        y[47] = S_1;
        y[48] = Humoral_Capillary;
        y[49] = Oxygen_Delivery;
        y[50] = A_24;
        y[51] = BloodFlow_Capillary;
        y[52] = Debt_Venous;
        y[53] = Volume_Venous;
        y[54] = BloodFlow_VenousToVentricular;
        y[55] = Volume_Ventricular_N;
        y[56] = Pressure_Arterial;
        y[57] = A_18;
        y[58] = A_29;
        y[59] = Pressure_Sistole;
        y[60] = CardiacOutput_Minute;
        y[61] = A_6;
        return y;
    }


    //events temporal variables
    private double eventAssignment9 = 0;
    private double eventAssignment8 = 0;
    private double eventAssignment7 = 0;
    private double eventAssignment12 = 0;
    private double eventAssignment10 = 0;
    private double eventAssignment11 = 0;
    private double eventAssignment = 0;
    private double eventAssignment1 = 0;
    private double eventAssignment2 = 0;
    private double eventAssignment5 = 0;
    private double eventAssignment6 = 0;
    private double eventAssignment3 = 0;
    private double eventAssignment4 = 0;


    @Override
    public double[] checkEvent(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        calculateParameters();
        double[] z = new double[6];
        z[0] = ( x_values[4] < ( 1 - K_1 ) * Volume_Ventricular_Diastole + K_2 && Stage_Sistole == 1) ? 1 : -1; // math-event_0 part1;
        z[1] = ( x_values[4] < ( 1 - K_1 ) * Volume_Ventricular_Diastole + K_2 && Stage_Sistole == 1) ? 1 : -1; // math-event_0 part2;
        z[2] = ( x_values[0] > 1.0 / x_values[6] - Duration_Sistole && Stage_Sistole != 1) ? 1 : -1; // math-event_2 part1;
        z[3] = ( x_values[0] > 1.0 / x_values[6] - Duration_Sistole && Stage_Sistole != 1) ? 1 : -1; // math-event_2 part2;
        z[4] = ( x_values[5] >= 60 ) ? 1 : -1; // math-event_3 part1;
        z[5] = ( x_values[5] >= 60 ) ? 1 : -1; // math-event_3 part2;
        return z;
    }


    @Override
    public void processEvent(int i)
    {
        if( i == 0 ) // math-event_0 part1
        {
            eventAssignment = 0;
            eventAssignment1 = x_values[0];
            eventAssignment2 = 0;
            eventAssignment3 = CardiacOutput + ( Volume_Ventricular_Diastole - x_values[4] );
        }
        else if( i == 1 ) // math-event_0 part2
        {
            Stage_Sistole = eventAssignment;
            Duration_Sistole = eventAssignment1;
            x_values[0] = eventAssignment2;
            CardiacOutput = eventAssignment3;
        }
        else if( i == 2 ) // math-event_2 part1
        {
            eventAssignment4 = Pressure_Sistole + S_1 * ( Duration_Sistole - S_2 / Humoral_Diastole - S_3 * ( 1 - K_1 ) );
            eventAssignment5 = x_values[4];
            eventAssignment6 = x_values[6];
            eventAssignment7 = 1;
            eventAssignment8 = 0;
            eventAssignment9 = 0;
            Pulse += 1;
        }
        else if( i == 3 ) // math-event_2 part2
        {
            Pressure_Sistole = eventAssignment4;
            Volume_Ventricular_Diastole = eventAssignment5;
            Humoral_Diastole = eventAssignment6;
            Stage_Sistole = eventAssignment7;
            Duration_Sistole = eventAssignment8;
            x_values[0] = eventAssignment9;
        }
        else if( i == 4 ) // math-event_3 part1
        {
            eventAssignment10 = CardiacOutput;
            eventAssignment11 = 0;
            eventAssignment12 = 0;
        }
        else if( i == 5 ) // math-event_3 part2
        {
            CardiacOutput_Minute = eventAssignment10 / 1000;
            Pulse_Minute = Pulse;
            x_values[5] = eventAssignment11;
            CardiacOutput = eventAssignment12;
            Pulse = 0;
        }
    }


    @Override
    public double[] getEventsPriority(double time, double[] x) throws Exception
    {
        double[] priority = new double[6];
        calculateParameters();
        priority[0] = Double.POSITIVE_INFINITY; //math-event_0 part1
        priority[1] = Double.NEGATIVE_INFINITY; //math-event_0 part2
        priority[2] = Double.POSITIVE_INFINITY; //math-event_2 part1
        priority[3] = Double.NEGATIVE_INFINITY; //math-event_2 part2
        priority[4] = Double.POSITIVE_INFINITY; //math-event_3 part1
        priority[5] = Double.NEGATIVE_INFINITY; //math-event_3 part2
        return priority;
    }


    @Override
    public boolean getEventsInitialValue(int i) throws IndexOutOfBoundsException
    {
        if( i == 0 ) // math-event_0 part1
            return true;
        else if( i == 1 ) // math-event_0 part2
            return true;
        else if( i == 2 ) // math-event_2 part1
            return true;
        else if( i == 3 ) // math-event_2 part2
            return true;
        else if( i == 4 ) // math-event_3 part1
            return true;
        else if( i == 5 ) // math-event_3 part2
            return true;
        else
            throw new IndexOutOfBoundsException("Event index out of bounds: " + i + " (6) ");
    }


    @Override
    public boolean isEventTriggerPersistent(int i) throws IndexOutOfBoundsException
    {
        if( i == 0 ) // math-event_0 part1
            return true;
        else if( i == 1 ) // math-event_0 part2
            return true;
        else if( i == 2 ) // math-event_2 part1
            return true;
        else if( i == 3 ) // math-event_2 part2
            return true;
        else if( i == 4 ) // math-event_3 part1
            return true;
        else if( i == 5 ) // math-event_3 part2
            return true;
        else
            throw new IndexOutOfBoundsException("Event index out of bounds: " + i + " (6) ");
    }
}