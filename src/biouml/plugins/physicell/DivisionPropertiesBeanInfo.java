package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class DivisionPropertiesBeanInfo extends BeanInfoEx2<DivisionProperties>
{
    public DivisionPropertiesBeanInfo()
    {
        super( DivisionProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "asymmetric" );
        property( "probabilities").hidden("isDefault").add();
    }
}