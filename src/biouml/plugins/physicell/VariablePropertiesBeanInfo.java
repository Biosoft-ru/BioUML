package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class VariablePropertiesBeanInfo extends BeanInfoEx2<VariableProperties>
{
    public VariablePropertiesBeanInfo()
    {
        super( VariableProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "name" );
        add( "value" );
        add( "units" );
        add( "conserved" );
    }
}