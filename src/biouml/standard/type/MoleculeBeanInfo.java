package biouml.standard.type;

public class MoleculeBeanInfo extends ConceptBeanInfo
{   
    protected MoleculeBeanInfo(Class<? extends Molecule> beanClass)
    {
        super( beanClass );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        int index = findPropertyIndex("databaseReferences");
        property("structureReferences").htmlDisplayName("ST").add(index);
    }
}
