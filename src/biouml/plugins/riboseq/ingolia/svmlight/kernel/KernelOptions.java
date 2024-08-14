package biouml.plugins.riboseq.ingolia.svmlight.kernel;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.riboseq.ingolia.svmlight.Options;

public abstract class KernelOptions implements Options
{
    protected abstract int getKernelTypeCode();
    
    @Override
    public List<String> getOptions()
    {
        List<String> options = new ArrayList<>();
        options.add( "-t" );
        options.add( String.valueOf( getKernelTypeCode() ) );
        return options;
    }
}
