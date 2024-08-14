package ru.biosoft.galaxy;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisParameters;

public class DataSourceStubMethod extends GalaxyMethod
{
    DataSourceMethodParameters parameters;
    
    public DataSourceStubMethod(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @Override
    public AnalysisParameters getParameters()
    {
        if( parameters == null )
        {
            parameters = new DataSourceMethodParameters((DataSourceMethodInfo)methodInfo);
        }
        return parameters;
    }

    @Override
    public void setParameters(AnalysisParameters parameters)
    {
        if(!(parameters instanceof DataSourceMethodParameters))
            throw new IllegalArgumentException();
        this.parameters = (DataSourceMethodParameters)parameters;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        throw new IllegalArgumentException("This analysis cannot be started");
    }

    @Override
    public String generateJavaScript(Object parameters)
    {
        return "/* No JavaScript available for this analysis */";
    }
}