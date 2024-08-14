package ru.biosoft.analysiscore;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon ( "resources/analysis.gif" )
public class AnalysisMethodElement extends DataElementSupport
{
    private AnalysisMethod method;

    public AnalysisMethodElement(String name, DataCollection<?> origin)
    {
        super( name, origin );
    }

    public AnalysisMethod getAnalysisMethod()
    {
        return method;
    }

    public void setAnalysisMethod(AnalysisMethod method)
    {
        this.method = method;
    }
}
