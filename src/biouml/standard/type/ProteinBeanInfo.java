package biouml.standard.type;

public class ProteinBeanInfo extends BiopolymerBeanInfo
{
    public ProteinBeanInfo()
    {
        super(Protein.class);
    }

    protected ProteinBeanInfo(Class<? extends Protein> beanClass)
    {
        super(beanClass);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        int index = findPropertyIndex("comment");
        property("gene").htmlDisplayName("GN").add(index);
        property("functionalState").tags(Protein.proteinFunctionalStates).htmlDisplayName("FN").add(index + 1);
        property("structure").tags(Protein.proteinStructures).htmlDisplayName("MM").add(index + 2);
        property("modification").tags(Protein.proteinModifications).htmlDisplayName("MD").add(index + 3);
    }
}
