
package biouml.plugins.simulation;

import biouml.model.Diagram;
import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ModelAnalysisParametersBeanInfo extends BeanInfoEx2<ModelAnalysisParameters>
{
    public ModelAnalysisParametersBeanInfo()
    {
        super( ModelAnalysisParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {        
        add(DataElementPathEditor.registerInput("modelPath", beanClass, Diagram.class));
        add( "simulationEngine" );
        add( DataElementPathEditor.registerOutput( "reportPath", beanClass, HtmlDataElement.class ));
    }
}
