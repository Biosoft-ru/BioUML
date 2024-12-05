package biouml.plugins.virtualcell.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SingleConnectionPropertiesBeanInfo extends BeanInfoEx2<SingleConnectionProperties>
{
    public SingleConnectionPropertiesBeanInfo()
    {
        super( SingleConnectionProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "nameFrom" );
        add( "nameTo" );
    }
}