package biouml.plugins.modelreduction;

import ru.biosoft.access.core.FolderCollection;
import biouml.model.Diagram;

public class MetabolicControlAnalysisParametersBeanInfo extends SteadyStateAnalysisParametersBeanInfo
{
    public MetabolicControlAnalysisParametersBeanInfo()
    {
        super(MetabolicControlAnalysisParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("input").inputElement(Diagram.class).add();
        property("output").outputElement(FolderCollection.class).add();
        super.initMethodProperties();
    }
}