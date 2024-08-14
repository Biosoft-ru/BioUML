package biouml.plugins.simulation.ae;

import ru.biosoft.util.bean.BeanInfoEx2;

public class NewtonSolverWrapperExBeanInfo extends BeanInfoEx2<NewtonSolverWrapperEx>
{
    public NewtonSolverWrapperExBeanInfo()
    {
        super(NewtonSolverWrapperEx.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("maxIts");
        add("tolF");
        add("tolMin");
        add("tolX");
    }
}
