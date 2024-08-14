package biouml.standard.diagram;

import one.util.streamex.StreamEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PortPropertiesBeanInfo extends BeanInfoEx2<PortProperties>
{
    public PortPropertiesBeanInfo()
    {
        super( PortProperties.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("title");
        property("portType").hidden("isPortTypeFixed").tags(PortProperties.getAvailablePortTypes()).add();
        property("accessType").hidden("isDiagramFlat").tags(PortProperties.getAvailableAccessTypes()).add();
        property("moduleName").hidden("isPublicPort").tags(bean -> StreamEx.of(bean.availableModules)).add();
        property("basePortName").hidden("isPublicPort").tags(bean -> StreamEx.of(bean.availablePorts)).add();
        property( "varName" ).hidden( "isPropagatedPort" ).tags( bean -> StreamEx.of( bean.availableParameters ) ).add();
    }
}
