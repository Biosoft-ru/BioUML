package ru.biosoft.analysiscore;

import ru.biosoft.access.CacheableBeanProvider;

public class AnalysisParametersBeanProvider implements CacheableBeanProvider
{
    public static final String PREFIX = "properties/method/parameters/";

    @Override
    public Object getBean(String path)
    {
        AnalysisMethod method = AnalysisMethodRegistry.getAnalysisMethod(path);
        return method == null ? null : method.getParameters();
    }

}
