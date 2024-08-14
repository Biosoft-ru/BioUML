package biouml.plugins.simulation.ode;

import biouml.plugins.simulation.Model;

/*
 * WARNING: READ BEFORE EDITING THIS CLASS!
 * 
 * This class and some others constitutes separate jar which is used during model simulation in BioUML,
 * therefore all class on which it depends should also added to this jar 
 * jar file used for simulation is specified by SimulationEngine
 * 
 * Before adding any new dependencies here - please think twice.
 * 
 * If you add dependency - add this class (and all classes from which it depends) to build_bdk.xml
 * (see biouml.plugins.simulation building)
 * @see SimualtionEngine
 */

/* Ordinary Differential Equation (ODE) Model */
public interface OdeModel extends Model
{
    public boolean isStatic();
    
    /** An ODE function declaration */
    public double[] dy_dt(double t, double[] x) throws Exception;

    public double[] getY();
    
    public double getTime();

    /** 
     * An event function declaration that is defined by the implementor(note that an event function is one that defines where an event
     * occurs in the ODE). Event functions are recognized by the event locator particular solvers
     */
    public double[] checkEvent(double t, double[] x) throws Exception;
    public void processEvent(int i);
    public String getEventMessage(int i);
    
    public boolean getEventsInitialValue(int i) throws IndexOutOfBoundsException;
    
    public boolean isEventTriggerPersistent(int i) throws IndexOutOfBoundsException;
    
    public double[] getEventsPriority(double time, double[] x) throws Exception;

    /** Update solution history */
    public void updateHistory(double time);
    
    public double[] getPrehistory(double time);
    
    public double getPrehistory(double time, int i);
    
    public double[] getCurrentHistory();


    /**Returns result with all the variables calculated.*/
    public double[] extendResult(double time, double[] x) throws Exception;

    public boolean isConstraintViolated();
    
    public boolean hasFastOde();
}
