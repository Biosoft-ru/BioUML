package biouml.plugins.sedml.analyses;

import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author axec
 *
 */
public class GenerateReportParametersBeanInfo<T extends GenerateReportParameters> extends BeanInfoEx2<T>
{
    protected GenerateReportParametersBeanInfo(Class<T> beanClass)
    {
        super(beanClass);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( DataElementPathEditor.registerInputMulti( "simulationResultPath", beanClass, SimulationResult.class ) );
    }
    
}
