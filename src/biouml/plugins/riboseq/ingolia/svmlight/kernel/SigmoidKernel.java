package biouml.plugins.riboseq.ingolia.svmlight.kernel;

import java.util.List;

public class SigmoidKernel extends KernelOptions
{
    private Double s;
    public Double getS()
    {
        return s;
    }
    public void setS(Double s)
    {
        this.s = s;
    }
    
    private Double c;
    public Double getC()
    {
        return c;
    }
    public void setC(Double c)
    {
        this.c = c;
    }
    
    @Override
    protected int getKernelTypeCode()
    {
        // TODO Auto-generated method stub
        return 3;
    }

    @Override
    public List<String> getOptions()
    {
        List<String> options = super.getOptions();
        if(s != null)
        {
            options.add( "-s" );
            options.add( String.valueOf( s ) );
        }
        if(c != null)
        {
            options.add( "-r" );
            options.add( String.valueOf( c ) );
        }
        return options;
    }
}
