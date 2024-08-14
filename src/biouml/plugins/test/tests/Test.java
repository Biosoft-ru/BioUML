package biouml.plugins.test.tests;

import com.developmentontheedge.beans.Option;

import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.test.Status;
import biouml.plugins.test.TestModel;
import biouml.standard.simulation.SimulationResult;

public abstract class Test extends Option
{
    //test status
    protected Status status;

    //short error message
    protected String error;

    public Status getStatus()
    {
        return status;
    }

    public void setError(String error)
    {
        this.error = error;
    }

    public String getError()
    {
        return error;
    }

    public abstract String test(SimulationResult simulationResult, SimulationEngine simulationEngine);

    //short info about criterion
    public abstract String getInfo();

    // generates JavaScript to demonstrate results
    public abstract String generateJavaScript(TestModel model);

    //
    // Serialization
    //

    protected static final String DELIMITER = ";";

    // restore test attributes from string
    public abstract void loadFromString(String string);
}
