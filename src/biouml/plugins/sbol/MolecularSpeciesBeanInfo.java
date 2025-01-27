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
        property( "type").tags( MolecularSpecies.types).readOnly( "isCreated" ).add();
        addReadOnly( "name", "isCreated" );
        add( "title" );
    }
}