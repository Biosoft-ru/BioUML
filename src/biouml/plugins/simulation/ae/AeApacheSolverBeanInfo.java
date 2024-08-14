package biouml.plugins.simulation.ae;

import ru.biosoft.util.bean.BeanInfoEx2;

public class AeApacheSolverBeanInfo extends BeanInfoEx2<AeApacheSolver>
{
    public AeApacheSolverBeanInfo()
    {
        super(AeApacheSolver.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("ftol");
        add("maxIter");
        add("maxEval");
    }
}
