package biouml.standard.type;


public class StateBeanInfo extends ConceptBeanInfo
{
    protected StateBeanInfo(Class beanClass, String key)
    {
        super(beanClass, key);
    }

    public StateBeanInfo()
    {
        super(State.class, "STATE");
    }
}
