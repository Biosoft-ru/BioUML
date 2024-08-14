package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;

@PropertyName ( "Parameters" )
public class MetabolicControlAnalysisParameters extends SteadyStateAnalysisParameters
{
    @Override
    @PropertyName ( "Result path" )
    @PropertyDescription ( "Generic data collection to save results of the analysis." )
    public DataElementPath getOutput()
    {
        return super.getOutput();
    }
}
