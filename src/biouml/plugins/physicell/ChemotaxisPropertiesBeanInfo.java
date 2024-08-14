package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ChemotaxisPropertiesBeanInfo extends BeanInfoEx2<ChemotaxisProperties>
{
    public ChemotaxisPropertiesBeanInfo()
    {
        super( ChemotaxisProperties.class );
    }

    @Override
    public void initProperties()
    {
        addReadOnly( "title" );
        add( "sensitivity" );
    }
}