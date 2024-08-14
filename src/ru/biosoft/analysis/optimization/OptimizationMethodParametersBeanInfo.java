package ru.biosoft.analysis.optimization;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class OptimizationMethodParametersBeanInfo extends BeanInfoEx2<OptimizationMethodParameters>
{
    public OptimizationMethodParametersBeanInfo()
    {
        super( OptimizationMethodParameters.class);
    }

    public OptimizationMethodParametersBeanInfo(Class<? extends OptimizationMethodParameters> type)
    {
        super(type);
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "diagramPath" ).readOnly().add(); 
        property( "resultPath" ).outputElement( FolderCollection.class ).add();
        property( "applyState" ).add();
        property( "randomSeedHidden" ).hidden( "isNotStochastic" ).add();
        property( "randomSeed" ).hidden( "isRandomSeedHidden" ).add();
        property( "useStartingParameters" ).add();
        property( "startingParameters" ).hidden( "isStartingParametersHidden" ).add();
    }
}
