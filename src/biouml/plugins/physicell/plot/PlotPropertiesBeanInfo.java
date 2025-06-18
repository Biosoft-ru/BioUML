package biouml.plugins.physicell.plot;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PlotPropertiesBeanInfo extends BeanInfoEx2<PlotProperties>
{
    public PlotPropertiesBeanInfo()
    {
        super( PlotProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "properties" );
    }
}