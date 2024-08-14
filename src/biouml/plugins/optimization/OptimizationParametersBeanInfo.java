package biouml.plugins.optimization;

import com.developmentontheedge.beans.BeanInfoEx;

public class OptimizationParametersBeanInfo extends BeanInfoEx
{
    public OptimizationParametersBeanInfo()
    {
        super( OptimizationParameters.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        addHidden("fittingParameters");
        addHidden("optimizationExperiments");
        addHidden("optimizationConstraints");
    }
}