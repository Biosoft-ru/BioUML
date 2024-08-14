package biouml.plugins.riboseq.ingolia.svmlight.kernel;

import java.util.List;

public class PolynomialKernel extends KernelOptions
{
    private Integer d;

    public Integer getD()
    {
        return d;
    }
    public void setD(Integer d)
    {
        this.d = d;
    }


    @Override
    protected int getKernelTypeCode()
    {
        return 1;
    }

    @Override
    public List<String> getOptions()
    {
        List<String> options = super.getOptions();
        if(d != null)
        {
            options.add( "-d" );
            options.add( String.valueOf( d ) );
        }
        return options;
    }
}
