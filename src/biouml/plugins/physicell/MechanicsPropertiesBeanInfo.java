package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class MechanicsPropertiesBeanInfo extends BeanInfoEx2<MechanicsProperties>
{
    public MechanicsPropertiesBeanInfo()
    {
        super( MechanicsProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "cellCellAdhesionStrength" );
        add( "cellCellRepulsionStrength" );
        add( "cellBMAdhesionStrength" );
        add( "cellBMRepulsionStrength" );
        add( "relMaxAdhesionDistance" );
        add( "maxAttachments" );
        add( "attachmentElasticConstant" );
        add( "attachmentRate" );
        add( "detachmentRate" );
    }
}