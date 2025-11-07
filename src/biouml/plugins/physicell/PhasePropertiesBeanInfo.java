package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PhasePropertiesBeanInfo extends BeanInfoEx2<PhaseProperties>
{
    public PhasePropertiesBeanInfo()
    {
        super( PhaseProperties.class );
    }

    @Override
    public void initProperties()
    {
        property( "name" ).hidden( "isCompleted" ).add();
        property( "title" ).hidden().implicit().add();
        property( "divisionAtExit" ).hidden( "isDeathPhase" ).add();
        property( "removalAtExit" ).hidden( "isLivePhase" ).add();
        property( "isDeathPhase" ).hidden().add();
    }
}