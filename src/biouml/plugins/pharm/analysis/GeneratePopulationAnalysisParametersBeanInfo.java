package biouml.plugins.pharm.analysis;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class GeneratePopulationAnalysisParametersBeanInfo extends BeanInfoEx2<GeneratePopulationAnalysisParameters>
{
    public GeneratePopulationAnalysisParametersBeanInfo()
    {
        super( GeneratePopulationAnalysisParameters.class );
    }
    
    @Override
    public void initProperties() throws Exception
    {
        property("outputTablePath").outputElement(TableDataCollection.class).add();
    }
}
