package biouml.standard.diagram.properties;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ConstantElementPropertiesBeanInfo extends BeanInfoEx2<ConstantElementProperties>
{
    public ConstantElementPropertiesBeanInfo()
    {
        super( ConstantElementProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
        add( "value" );
    }
}