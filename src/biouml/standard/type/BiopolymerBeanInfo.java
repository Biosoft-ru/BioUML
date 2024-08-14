package biouml.standard.type;

public class BiopolymerBeanInfo extends MoleculeBeanInfo
{
    protected BiopolymerBeanInfo(Class<? extends Biopolymer> beanClass)
    {
        super(beanClass);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property("species").htmlDisplayName("OS").add(2);
        int index = findPropertyIndex("comment");
        property("source").htmlDisplayName("SO").add(index + 1);
        property("regulation").htmlDisplayName("RE").add(index + 2);
    }
}
