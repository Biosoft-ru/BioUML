package biouml.plugins.brain.model;

public interface BrainModelProperties
{
    // parameters for each type of brain model should be specified.
	
    public boolean getPortsFlag(); // if true than ports will be generated in equation diagram for further use in composite diagram
    public void setPortsFlag(boolean portsFlag);
}
