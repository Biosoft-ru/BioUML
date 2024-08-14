package biouml.plugins.research;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.util.bean.BeanInfoEx2;

public class AnalysisMethodRefBeanInfo extends BeanInfoEx2<AnalysisMethodRef>
{
    public AnalysisMethodRefBeanInfo()
    {
        super(AnalysisMethodRef.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        addWithTags( "analysisMethod", bean -> AnalysisMethodRegistry.getAnalysisNamesWithGroup());
        add("selectManually");        
        addHidden( DataElementPathEditor.registerInput("analysisElement", beanClass, AnalysisMethodInfo.class), "isSelectFromList" );
    }
}