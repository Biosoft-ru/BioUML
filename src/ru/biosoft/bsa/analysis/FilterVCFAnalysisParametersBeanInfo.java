package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;


public class FilterVCFAnalysisParametersBeanInfo extends BeanInfoEx2<FilterVCFAnalysisParameters>
{
    public FilterVCFAnalysisParametersBeanInfo()
    {
        super( FilterVCFAnalysisParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("inputTrack", beanClass, Track.class));
        property( "mode" ).tags( FilterVCFAnalysisParameters.MODES ).add();
        property( "outputPath" ).outputElement( FolderCollection.class ).auto( "$inputTrack$ filtered" ).add();
    }
}
