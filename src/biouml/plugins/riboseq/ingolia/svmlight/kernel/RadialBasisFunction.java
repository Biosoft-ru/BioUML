package biouml.plugins.riboseq.ingolia.svmlight.kernel;

import java.util.List;

public class RadialBasisFunction extends KernelOptions
{

    private Double gamma;
    public Double getGamma()
    {
        return gamma;
    }
    public void setGamma(Double gamma)
    {
        this.gamma = gamma;
    }

    @Override
    protected int getKernelTypeCode()
    {
        return 2;
    }
    
    @Override
    public List<String> getOptions()
    {
        List<String> options = super.getOptions();
        if(gamma != null)
        {
            options.add( "-g" );
            options.add( String.valueOf( gamma ) );
        }
        return options;
    }

}
