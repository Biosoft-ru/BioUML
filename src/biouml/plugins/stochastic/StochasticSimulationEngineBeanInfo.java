package biouml.plugins.stochastic;

import biouml.plugins.simulation.SimulationEngineBeanInfo;
import biouml.plugins.simulation.java.JavaSimulationEngine;

/**
 * Definition of common properties for simulation engine
 */
public class StochasticSimulationEngineBeanInfo extends SimulationEngineBeanInfo
{
    public StochasticSimulationEngineBeanInfo()
    {
        super( StochasticSimulationEngine.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add( "simulationNumber" );
        add( "transformRates" );
//        property( "resultType" ).tags( StochasticSimulationEngine.RESULT_TYPE_MEAN, StochasticSimulationEngine.RESULT_TYPE_REPEATS ).add();
        property( "templateType" ).tags( JavaSimulationEngine.TEMPLATE_AUTO, JavaSimulationEngine.TEMPLATE_NORMAL_ONLY,
                JavaSimulationEngine.TEMPLATE_LARGE_ONLY ).add();
        add( "outputMolecules" );
        add( "customSeed" );
        addHidden( "seed", "isAutoSeed" );
        add( "averageRegime" );
    }
}
