package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class InteractionPropertiesBeanInfo extends BeanInfoEx2<InteractionProperties>
{
    public InteractionPropertiesBeanInfo()
    {
        super( InteractionProperties.class );
    }

    @Override
    public void initProperties()
    {
        addReadOnly( "cellType" );
        add( "attackRate" );
        add( "fuseRate" );
        add( "phagocytosisRate" );
    }
}