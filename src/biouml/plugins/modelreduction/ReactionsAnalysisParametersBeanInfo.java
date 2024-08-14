package biouml.plugins.modelreduction;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;

import com.developmentontheedge.beans.BeanInfoConstants;

public class ReactionsAnalysisParametersBeanInfo extends BeanInfoEx2<ReactionsAnalysisParameters>
{
    public ReactionsAnalysisParametersBeanInfo()
    {
    	super(ReactionsAnalysisParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("input").inputElement(Diagram.class).add();
        property("output").outputElement(FolderCollection.class).add();
        property("analysisTarget").add();
        property("threshold").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).hidden("isSimulationParametersHidden").add();
        property("initialTime").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).hidden("isSimulationParametersHidden").add();
        property("completionTime").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).hidden("isSimulationParametersHidden").add();
        property("timeIncrement").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).hidden("isSimulationParametersHidden").add();
    }
}
