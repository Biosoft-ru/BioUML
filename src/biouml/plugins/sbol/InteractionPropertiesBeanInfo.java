package biouml.plugins.sbol;

import ru.biosoft.util.bean.BeanInfoEx2;

public class InteractionPropertiesBeanInfo extends BeanInfoEx2<InteractionProperties>
{
    public InteractionPropertiesBeanInfo()
    {
        super( InteractionProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "name", "isCreated" );
        addWithTags( "type", SbolConstants.interactionTypes);
    }
}