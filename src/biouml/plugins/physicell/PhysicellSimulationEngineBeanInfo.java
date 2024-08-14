package biouml.plugins.physicell;

import one.util.streamex.StreamEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PhysicellSimulationEngineBeanInfo extends BeanInfoEx2<PhysicellSimulationEngine>
{
    public PhysicellSimulationEngineBeanInfo()
    {
        super( PhysicellSimulationEngine.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "solverName" ).structureChanging().tags( bean -> StreamEx.of( bean.getAvailableSolvers() ) ).add();
        add( "logReport" );
        add( "simulatorOptions" );
    }
}
