package biouml.plugins.simulation;

/**
 * Formal description of simulator.
 */
public class SimulatorInfo
{
    public String name;
    public String description;
    public boolean eventsSupport;
    public boolean delaySupport;
    public boolean boundaryConditionSupport;

    // Some classification - ODE, STOCHASTIC, ...
    String type;
}
