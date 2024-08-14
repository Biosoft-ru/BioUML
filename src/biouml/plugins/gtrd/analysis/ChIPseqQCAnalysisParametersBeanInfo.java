package biouml.plugins.gtrd.analysis;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ChIPseqQCAnalysisParametersBeanInfo extends BeanInfoEx2<ChIPseqQCAnalysisParameters>
{
    public ChIPseqQCAnalysisParametersBeanInfo()
    {
        super(ChIPseqQCAnalysisParameters.class);
    }
    
    @Override
    protected void initProperties() throws Exception
    {
    	property("dataType").tags( ChIPseqQCAnalysisParameters.SEVERAL_FOLDERS, ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA ).add();
    	property("inputDataParameters").hidden("areInputParametersHidden").add();
    	property("pathsToDataSets").hidden("isPathsToDataSetsHidden").add();
    	property("qCAnalysisParameters").add();
    	property("pathToOutputFolder").inputElement( FolderCollection.class ).add();
    }
    
}