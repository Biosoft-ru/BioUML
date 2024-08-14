package biouml.plugins.modelreduction;

import ru.biosoft.access.core.FolderCollection;

import biouml.model.Diagram;

public class SensitivityAnalysisParametersBeanInfo extends SteadyStateAnalysisParametersBeanInfo
{
    public SensitivityAnalysisParametersBeanInfo()
    {
        super(SensitivityAnalysisParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {        
        property("input").inputElement(Diagram.class).add();
        property("output").outputElement( FolderCollection.class).add();
        addExpert("variableNames");
        addExpert("targetVariables");
        addExpert("inputVariables");
        add("relativeStep");
        add("absoluteStep");
        super.initMethodProperties();
    }
}
