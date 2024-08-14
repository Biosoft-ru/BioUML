package biouml.plugins.riboseq.ingolia.svmlight.kernel;

import java.util.List;

public class UserDefinedKernel extends KernelOptions
{
    private String parameters;
    public String getParameters()
    {
        return parameters;
    }
    public void setParameters(String parameters)
    {
        this.parameters = parameters;
    }

    @Override
    protected int getKernelTypeCode()
    {
        return 4;
    }

    @Override
    public List<String> getOptions()
    {
        List<String> options = super.getOptions();
        if(parameters != null)
        {
            options.add( "-u" );
            options.add( parameters );
        }
        return options;
    }
}
