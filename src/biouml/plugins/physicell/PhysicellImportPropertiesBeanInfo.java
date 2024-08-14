package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PhysicellImportPropertiesBeanInfo extends BeanInfoEx2<PhysicellImportProperties>
{
    public PhysicellImportPropertiesBeanInfo()
    {
        super( PhysicellImportProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "diagramName" );
        add( "importDefaultDefinition" );
    }
}