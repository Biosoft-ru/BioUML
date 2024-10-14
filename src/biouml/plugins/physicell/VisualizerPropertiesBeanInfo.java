package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class VisualizerPropertiesBeanInfo extends BeanInfoEx2<VisualizerProperties>
{
    public VisualizerPropertiesBeanInfo()
    {
        super(VisualizerProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "properties" );
    }
}