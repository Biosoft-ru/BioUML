package biouml.plugins.modelreduction;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;


public class StoichiometricAnalysisParametersBeanInfo extends BeanInfoEx2<StoichiometricAnalysisParameters>
{
    public StoichiometricAnalysisParametersBeanInfo()
    {
        super(StoichiometricAnalysisParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {        
        property("input").inputElement(Diagram.class).add();
        property("output").outputElement( TableDataCollection.class).add();
    }
}
