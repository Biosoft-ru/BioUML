package biouml.plugins.modelreduction;


import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@PropertyName ( "Parameters" )
public class StoichiometricAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath input;

    @PropertyName("Input diagram")
    @PropertyDescription("Diagram to find a stoichiometric matrix.")
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
    @PropertyDescription ("A path to save the result table containing information about stoichiometric coefficients.")
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
}
