package ru.biosoft.plugins.docker;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CWLDockeredAnalysisParametersBeanInfo extends BeanInfoEx2<CWLDockeredAnalysisParameters>
{
    public CWLDockeredAnalysisParametersBeanInfo()
    {
        super( CWLDockeredAnalysisParameters.class );
        this.setSubstituteByChild( true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerOutput( "outputFolder", beanClass, DataCollection.class ));

        property("cwlFile").structureChanging().expert().add();
        property("dockerImage").structureChanging().expert().add();
        property("cwlPath").structureChanging().expert().add();

        property( "parameters" ).structureChanging().add();
        property( "outputs" ).structureChanging().add();
    }
}