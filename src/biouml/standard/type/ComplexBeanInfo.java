package biouml.standard.type;

public class ComplexBeanInfo extends ConceptBeanInfo
{
    public ComplexBeanInfo()
    {
        super(Complex.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        int index = findPropertyIndex("comment")+1;
        property("components").htmlDisplayName("CP").add(index);
    }
}
