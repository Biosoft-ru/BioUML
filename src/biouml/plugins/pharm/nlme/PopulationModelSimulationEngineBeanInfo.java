package biouml.plugins.pharm.nlme;

import static biouml.plugins.pharm.nlme.PopulationModelSimulationEngine.*;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PopulationModelSimulationEngineBeanInfo extends BeanInfoEx2<PopulationModelSimulationEngine>
{

    public PopulationModelSimulationEngineBeanInfo()
    {
        super( PopulationModelSimulationEngine.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addWithTags( "method", METHOD_ML, METHOD_REML );
        addWithTags( "randomEffectsType", RANDOM_DIAG, RANDOM_DENSE );
        add("needToShowPlot");
        add( "aTolSteadyState" );
        add( "rTolSteadyState" );
        add( "engine" );
    }
}