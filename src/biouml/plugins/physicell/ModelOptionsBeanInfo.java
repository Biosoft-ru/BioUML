package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ModelOptionsBeanInfo extends BeanInfoEx2<ModelOptions>
{
    public ModelOptionsBeanInfo()
    {
        super( ModelOptions.class );
    }

    @Override
    public void initProperties()
    {
            add( "disableAutomatedAdhesions" );   
    }
}