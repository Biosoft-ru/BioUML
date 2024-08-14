package biouml.plugins.simulation;

import java.util.logging.Level;

import biouml.standard.simulation.ResultListener;

import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * General definiton of model simulator.
 */
public interface Simulator
{
    /** Returns simulator description. */
    public SimulatorInfo getInfo();

    /** Returns default options for the simulator. */
    public Object getDefaultOptions();

    /**
     * Initialize simulation options
     */
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception;
       
    /**
     * set initial values for simulation which will be used instead of model preset initial values
     */
    public void setInitialValues(double[] x0) throws Exception;

    
    /**
     * Process one step of simulation
     * It is generally expected that solver will proceed calculations only to next point of span and not beyond it
     * @return false is simulation complete, true otherwise
     */
    public boolean doStep() throws Exception;

    /**
     * Start model simulation
     */
    public void start(Model model, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl)
            throws Exception;

    /**
     * Break current simulation
     */
    public void stop();

    public SimulatorProfile getProfile();

    public Options getOptions();
    public void setOptions(Options options);
    
    public void setLogLevel(Level level);
}