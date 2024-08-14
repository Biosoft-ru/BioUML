package biouml.standard.type;

public class CompartmentBeanInfo extends ConceptBeanInfo
{
    public CompartmentBeanInfo()
    {
        super(Compartment.class);
    }

    protected CompartmentBeanInfo(Class<? extends Compartment> beanClass)
    {
        super(beanClass);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add("spatialDimension");
    }
}
