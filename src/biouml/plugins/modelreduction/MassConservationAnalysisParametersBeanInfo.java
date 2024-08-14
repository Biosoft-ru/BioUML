package biouml.plugins.modelreduction;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;

public class MassConservationAnalysisParametersBeanInfo extends BeanInfoEx2<MassConservationAnalysisParameters>
{
    public MassConservationAnalysisParametersBeanInfo()
    {
        super(MassConservationAnalysisParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("input").inputElement(Diagram.class).add();
        property("output").outputElement( FolderCollection.class).add();
    }
}
