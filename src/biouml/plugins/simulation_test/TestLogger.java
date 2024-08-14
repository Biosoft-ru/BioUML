package biouml.plugins.simulation_test;

import biouml.standard.simulation.SimulationResult;

public interface TestLogger
{
    public void testStarted(String testName);
    public void testCompleted();

    public void warn(String message);
    public void error(int status, String message);

    public void simulationStarted();
    public void simulationCompleted();

    public void setStatistics(TestDescription statistics);
    public void setSimulationResult(SimulationResult simulationResult);

    public void complete();
    public int getStatus();
    
    public void setTimes(double[] times);
    public void setScriptName(String scriptname);
    public String getOutputPath();
}
