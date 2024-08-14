package biouml.plugins.optimization;

import ru.biosoft.util.bean.BeanInfoEx2;

public class OptimizationExperimentBeanInfo extends BeanInfoEx2<OptimizationExperiment>
{
    public OptimizationExperimentBeanInfo()
    {
        super(OptimizationExperiment.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("diagramStateName");
        add("filePath");
        add("cellLine");
        add("weightMethod");
        add("experimentType");
    }
}
