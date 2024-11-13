package biouml.plugins.sbol;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SequenceFeatureBeanInfo extends BeanInfoEx2<SequenceFeature>
{
    public SequenceFeatureBeanInfo()
    {
        super( SequenceFeature.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "name", "isCreated" );
        add( "title" );
        addWithTags( "type", Backbone.types);
        addWithTags( "role", SbolUtil.getFeatureRoles() );
        add("private");
    }
}