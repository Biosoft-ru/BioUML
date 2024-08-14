package biouml.standard.diagram;

import biouml.model.dynamics.VariableRole;
import one.util.streamex.StreamEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class BusPropertiesBeanInfo extends BeanInfoEx2<BusProperties>
{
    public BusPropertiesBeanInfo()
    {
        super( BusProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property("name").add();
        property("variable").tags(
                bean -> StreamEx.of( bean.model.getVariableRoles().stream() ).map( VariableRole::getName )
                        .prepend( BusProperties.NEW_VARIABLE_CONSTANT ) )
                .add();
    }
}