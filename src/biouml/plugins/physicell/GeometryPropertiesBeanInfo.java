package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class GeometryPropertiesBeanInfo extends BeanInfoEx2<GeometryProperties>
{
    public GeometryPropertiesBeanInfo()
    {
        super( GeometryProperties.class );
    }

    @Override
    public void initProperties()
    {
        addReadOnly( "radius" );
        addReadOnly( "nuclearRadius" );
        //        add( "surfaceArea" );
        add( "polarity" );
    }
}