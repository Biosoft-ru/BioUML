package biouml.plugins.sbol;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ParticipationPropertiesBeanInfo extends BeanInfoEx2<ParticipationProperties>
{
    public ParticipationPropertiesBeanInfo()
    {
        super( ParticipationProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "name");
        addWithTags( "type", SbolConstants.participationTypes);
    }
}