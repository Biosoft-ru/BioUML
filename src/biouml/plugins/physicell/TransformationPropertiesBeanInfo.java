package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class TransformationPropertiesBeanInfo extends BeanInfoEx2<TransformationProperties>
{
    public TransformationPropertiesBeanInfo()
    {
        super( TransformationProperties.class );
    }

    @Override
    public void initProperties()
    {
        addReadOnly( "cellType" );
        add( "rate" );
    }
}