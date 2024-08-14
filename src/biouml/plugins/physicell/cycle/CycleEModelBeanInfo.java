package biouml.plugins.physicell.cycle;

import ru.biosoft.util.bean.BeanInfoEx2;

public class CycleEModelBeanInfo extends BeanInfoEx2<CycleEModel>
{
    public CycleEModelBeanInfo()
    {
        super( CycleEModel.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "deathModel" );
    }
}