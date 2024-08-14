package biouml.standard.type;

public class CellBeanInfo extends CompartmentBeanInfo
{
    public CellBeanInfo()
    {
        super(Cell.class);
    }

    protected CellBeanInfo(Class<? extends Cell> beanClass)
    {
        super(beanClass);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property("species").htmlDisplayName("OS").add(2);
    }
}
