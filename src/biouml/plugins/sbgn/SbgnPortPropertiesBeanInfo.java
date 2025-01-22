package biouml.plugins.sbgn;

import biouml.standard.diagram.PortProperties;
import one.util.streamex.StreamEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SbgnPortPropertiesBeanInfo extends BeanInfoEx2<SbgnPortProperties>
{
    public SbgnPortPropertiesBeanInfo()
    {
        super( SbgnPortProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("title");
        property("portType").hidden("isPortTypeFixed").tags(PortProperties.getAvailablePortTypes()).add();
        property("accessType").hidden("isDiagramFlat").tags(PortProperties.getAvailableAccessTypes()).add();
        property( "moduleName" ).hidden( "isPublicPort" ).tags( bean -> StreamEx.of( bean.getAvailableModules() ) ).add();
        property( "basePortName" ).hidden( "isPublicPort" ).tags( bean -> StreamEx.of( bean.getAvailablePorts() ) ).add();
        property( "varName" ).hidden( "isPropagatedPort" ).tags( bean -> StreamEx.of( bean.getAvailableParameters() ) ).add();
    }
}
