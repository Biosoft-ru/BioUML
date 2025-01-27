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
        property("type").tags( Backbone.types ).readOnly(  "isCreated" ).add();
        addReadOnly( "name", "isCreated" );
        addHidden( "title", "isCreated" );
        addWithTags( "role", SbolUtil.getFeatureRoles() );
        add("private");
    }
}