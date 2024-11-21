package biouml.plugins.virtualcell.simulation;

import ru.biosoft.util.bean.BeanInfoEx2;

public class VirtualCellSimulationEngineBeanInfo extends BeanInfoEx2<VirtualCellSimulationEngine>
{
    public VirtualCellSimulationEngineBeanInfo()
    {
        super( VirtualCellSimulationEngine.class );
    }

    @Override
    public void initProperties()
    {
        add( "timeIncrement" );
        add( "timeCompletion" );
        add( "resultPath" );
    }
}