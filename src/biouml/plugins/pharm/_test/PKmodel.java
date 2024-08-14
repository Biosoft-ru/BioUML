package biouml.plugins.pharm._test;
import biouml.plugins.simulation.java.JavaBaseModel;
public class PKmodel extends JavaBaseModel
{
    public double doseTime;
    public double doseVal;
    public double unknown;
    public double ka;
    public double rate_reaction;
    public double Cc;
    public double ke;
    public double CL;
    public double rate_reaction_1;


    private void calculateParameters() throws Exception
    {
        Cc = x_values[0]/CL*ke;
    }


    private void calculateReactionRates()
    {
        rate_reaction = ka*x_values[1];
        rate_reaction_1 = ke*x_values[0];
    }
 

    @Override
    public double[] dy_dt(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[2];  
        calculateParameters();
        calculateReactionRates();
        dydt[0] = +rate_reaction-rate_reaction_1;
        dydt[1] = -rate_reaction;
        return dydt;
    }


    @Override
    public void init() throws Exception
    {
        doseTime = 0.0; // initial value of doseTime
        doseVal = 0.0; // initial value of doseVal
        unknown = 0.0; // initial value of unknown
        ka = 0.0; // initial value of ka
        rate_reaction = 0.0; // initial value of $$rate_reaction
        Cc = Double.NaN; // initial value of Cc
        ke = 0.0; // initial value of ke
        CL = 0.0; // initial value of CL
        time = 0.0; // initial value of time
        rate_reaction_1 = 0.0; // initial value of $$rate_reaction_1
        initialValues = getInitialValues();
        this.isInit = true; 
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
        calculateParameters();
            return x_values;
        }
        else return initialValues;
    }  


    @Override
    public double[] extendResult(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        calculateParameters();
        double[] y = new double[10];
        y[0] = doseTime;
        y[1] = doseVal;
        y[2] = unknown;
        y[3] = ka;
        y[4] = Cc;
        y[5] = ke;
        y[6] = x_values[0];
        y[7] = CL;
        y[8] = time;
        y[9] = x_values[1];
        return y;
    }


    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        doseTime = values[0];
        doseVal = values[1];
        unknown = values[2];
        ka = values[3];
        rate_reaction = values[3];
        Cc = values[4];
        ke = values[5];
        x_values[0] = values[6];
        CL = values[7];
        time = values[8];
        x_values[1] = values[9];
        rate_reaction_1 = values[9];
        calculateParameters();
    }


    //events temporal variables 
    public double eventAssignment1 = 0;
    public double eventAssignment = 0;


    @Override
    public double[] checkEvent(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        calculateParameters();
        double[] z  = new double[2];
        z[0] = (time > doseTime) ? 1 : -1; // math-event part1;
        z[1] = (time > doseTime) ? 1 : -1; // math-event part2;
        return z;
    }


    @Override
    public void processEvent(int i)
    {
        if (i == 0) // math-event part1
        {  
            eventAssignment = 0;
            eventAssignment1 = doseVal;
        }
        else if (i == 1) // math-event part2
        {  
            unknown = eventAssignment;
            x_values[1] = eventAssignment1;
        }
   }


   @Override
public double[] getEventsPriority(double time, double[] x) throws Exception
   {
       double[] priority = new double[2];
        calculateParameters();
       priority[0] = Double.POSITIVE_INFINITY; //math-event part1
       priority[1] = Double.NEGATIVE_INFINITY; //math-event part2
       return priority;       
   }


   @Override
public boolean getEventsInitialValue(int i) throws IndexOutOfBoundsException
   {
      if (i == 0) // math-event part1  
         return false;
      else if (i == 1) // math-event part2
         return false;
      else
         throw new IndexOutOfBoundsException("Event index out of bounds: "+i+" (2) ");   
   }


   @Override
public boolean isEventTriggerPersistent(int i) throws IndexOutOfBoundsException
   {
      if (i == 0) // math-event part1
         return true;
      else if (i == 1) // math-event part2  
         return true;
      else
         throw new IndexOutOfBoundsException("Event index out of bounds: "+i+" (2) ");     
    }    
 }