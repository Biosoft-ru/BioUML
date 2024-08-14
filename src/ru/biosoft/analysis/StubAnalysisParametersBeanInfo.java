package ru.biosoft.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class StubAnalysisParametersBeanInfo extends BeanInfoEx2<StubAnalysisParameters>
{
    public StubAnalysisParametersBeanInfo()
    {
        super(StubAnalysisParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput( "input", beanClass, FileDataElement.class ));
        addWithTags("launchType", StubAnalysisParameters.LAUNCH_LOCAL, StubAnalysisParameters.LAUNCH_SLURM);
        add(DataElementPathEditor.registerOutput( "outputFolder", beanClass, DataCollection.class ));
    }
}