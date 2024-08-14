package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@PropertyName ("Parameters")
public class QuasiSteadyStateAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath input;

    @PropertyName("Input diagram")
    @PropertyDescription("Diagram to be analyzed.")
    public DataElementPath getInput()
    {
        return input;
    }
    public void setInput(DataElementPath input)
    {
        Object oldValue = this.input;
        this.input = input;
        firePropertyChange("input", oldValue, input);
    }

    private DataElementPath output;

    @PropertyName ("Result table")
    @PropertyDescription ("A path to save the result table of analysis.")
    public DataElementPath getOutput()
    {
        return output;
    }
    public void setOutput(DataElementPath output)
    {
    	Object oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, output);
    }

    private double initialTime = 0.0;

    @PropertyName ("Initial time")
    @PropertyDescription ("Time point to start simulation of the diagram.")
    public double getInitialTime()
    {
        return initialTime;
    }
    public void setInitialTime(double initialTime)
    {
        this.initialTime = initialTime;
    }

    private double completionTime = 100.0;

    @PropertyName ("Completion time")
    @PropertyDescription ("Time period to simulate the diagram.")
    public double getCompletionTime()
    {
        return completionTime;
    }
    public void setCompletionTime(double completionTime)
    {
        this.completionTime = completionTime;
    }

    private double timeIncrement = 1.0;

    @PropertyName ("Time increment")
    @PropertyDescription ("A delta for time.")
    public double getTimeIncrement()
    {
        return timeIncrement;
    }
    public void setTimeIncrement(double timeIncrement)
    {
        this.timeIncrement = timeIncrement;
    }

    private double dEpsilon = 1E-7;

    @PropertyName ("\u03b5_d")
    @PropertyDescription ("Denominator epsilon")
    public double getDEpsilon()
    {
        return dEpsilon;
    }
    public void setDEpsilon(double dEpsilon)
    {
        this.dEpsilon = dEpsilon;
    }

    private double timeEpsilon = 0.001;

    @PropertyName ("\u03b5_t")
    @PropertyDescription ("Time epsilon")
    public double getTimeEpsilon()
    {
        return timeEpsilon;
    }
    public void setTimeEpsilon(double timeEpsilon)
    {
        this.timeEpsilon = timeEpsilon;
    }

    private double ratioEpsilon = 0.001;

    @PropertyName ("\u03b5_r")
    @PropertyDescription ("Ratio epsilon")
    public double getRatioEpsilon()
    {
        return ratioEpsilon;
    }
    public void setRatioEpsilon(double ratioEpsilon)
    {
        this.ratioEpsilon = ratioEpsilon;
    }
}