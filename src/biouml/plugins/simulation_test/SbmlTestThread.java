package biouml.plugins.simulation_test;

import biouml.plugins.simulation.OdeSimulationEngine;

public class SbmlTestThread extends Thread
{
    protected OdeSimulationEngine simulationEngine;
    protected TestLogger testLogger;
    protected String testName;
    protected SimulatorTest test;

    public SbmlTestThread(OdeSimulationEngine simulationEngine, TestLogger testLogger, String testName, SimulatorTest test)
    {
        this.simulationEngine = simulationEngine;
        this.testLogger = testLogger;
        this.testName = testName;
        this.test = test;
    }

    @Override
    public void run()
    {
        test.executeTest(simulationEngine, testLogger, testName);
    }
}
