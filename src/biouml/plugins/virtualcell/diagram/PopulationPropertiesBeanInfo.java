package biouml.plugins.virtualcell.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PopulationPropertiesBeanInfo extends BeanInfoEx2<PopulationProperties>
{
    public PopulationPropertiesBeanInfo()
    {
        super( PopulationProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
    }
}