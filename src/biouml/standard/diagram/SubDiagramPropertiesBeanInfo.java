package biouml.standard.diagram;

import one.util.streamex.StreamEx;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;

public class SubDiagramPropertiesBeanInfo extends BeanInfoEx2<SubDiagramProperties>
{
    public SubDiagramPropertiesBeanInfo()
    {
        super(SubDiagramProperties.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("external");
        property( DataElementPathEditor.registerInput( "diagramPath", beanClass, Diagram.class, false ) ).hidden( "isInternal" ).add();
        property( "modelDefinitionName" ).hidden( "isExternal" ).tags( bean -> StreamEx.of( bean.getAvailableModelDefinitions() ) ).add();
    }
}
