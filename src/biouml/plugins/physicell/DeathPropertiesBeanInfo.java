package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class DeathPropertiesBeanInfo extends BeanInfoEx2<DeathProperties>
{
    public DeathPropertiesBeanInfo()
    {
        super( DeathProperties.class );
    }

    @Override
    public void initProperties()
    {
        try
        {
            property( "deathModels" )
                    .childDisplayName(beanClass.getMethod("getDeathModelName", new Class[] { Integer.class, Object.class })).fixedLength().add();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}