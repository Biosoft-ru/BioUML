package biouml.plugins.sbml._test;


public interface TestLogger
{
    public String getCurrentCategory();
    public String getCurrentTest();

    public void categoryStarted(String name);
    public void testStarted(String name);
    public void testCompleted();

    public void warn(String message);
    public void error(Exception e);
    public void error(int status, String message);

    public int getStatus();
    public String getMessages();
    public Exception getException();

    public void simulationStarted();
    public void simulationCompleted();
    public long getSimulationTime();

    public void complete();
}


