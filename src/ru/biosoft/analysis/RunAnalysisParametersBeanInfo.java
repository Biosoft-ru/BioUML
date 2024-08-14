package ru.biosoft.analysis;

import ru.biosoft.util.bean.BeanInfoEx2;

public class RunAnalysisParametersBeanInfo extends BeanInfoEx2<RunAnalysisParameters>
{
    public RunAnalysisParametersBeanInfo()
    {
        super(RunAnalysisParameters.class);
    }
    @Override
    public void initProperties() throws Exception
    {
        add("analysis");
        add("parametersTable");  
        addWithTags( "row", bean -> bean.getAvailableRows() );
    }
}