package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@PropertyName ("Parameters")
public class MassConservationAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath input;

    @PropertyName("Input diagram")
    @PropertyDescription("A diagram to find conservation laws.")
    public DataElementPath getInput()
    {
        return input;
    }
    public void setInput(DataElementPath input)
    {
        Object oldValue = this.input;
        this.input = input;
        this.firePropertyChange("input", oldValue, input);
    }

    private DataElementPath output;

    @PropertyName("Result path")
    @PropertyDescription("A folder to save results of the analysis.")
    public DataElementPath getOutput()
    {
        return output;
    }
    public void setOutput(DataElementPath output)
    {
        DataElementPath oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, output);
    }
}
