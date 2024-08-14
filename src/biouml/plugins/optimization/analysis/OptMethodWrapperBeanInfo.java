package biouml.plugins.optimization.analysis;

import ru.biosoft.util.bean.BeanInfoEx2;

public class OptMethodWrapperBeanInfo extends BeanInfoEx2<OptMethodWrapper>
{
    public OptMethodWrapperBeanInfo()
    {
        super( OptMethodWrapper.class );
    }
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property( "optMethodName" ).tags( OptMethodWrapper.getAvailableMethodNames() ).add();
        property( "optimizationIterations" ).hidden( "isOptItersHidden" ).add();
        property( "populationSize" ).hidden( "isPopulationSizeHidden" ).add();
        property( "delta" ).hidden( "isDeltaHidden" ).add();
        property( "gridLength" ).hidden( "isGridHidden" ).add();
        property( "gridWidth" ).hidden( "isGridHidden" ).add();
        property( "particleNumber" ).hidden( "isParticlesHidden" ).add();
    }
}
