package biouml.plugins.sbol;

import ru.biosoft.util.bean.BeanInfoEx2;

public class MolecularSpeciesBeanInfo extends BeanInfoEx2<MolecularSpecies>
{
    public MolecularSpeciesBeanInfo()
    {
        super( MolecularSpecies.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "name", "isCreated" );
        add( "title" );
        addWithTags( "type", MolecularSpecies.types);
//        addWithTags( "role", new String[] {});
//        add("private");
    }
}