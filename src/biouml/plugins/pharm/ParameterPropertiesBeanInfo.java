package biouml.plugins.pharm;

import one.util.streamex.StreamEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ParameterPropertiesBeanInfo extends BeanInfoEx2<ParameterProperties>
{
    public ParameterPropertiesBeanInfo()
    {
        super( ParameterProperties.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        addWithTags( "parameterName", bean -> StreamEx.of( bean.getAvailableNames() ) );
        addWithTags( "type", bean -> StreamEx.of( bean.getAvailableTypes() ) );
    }
}