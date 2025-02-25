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
        property( "type" ).tags( Backbone.types ).readOnly( "isCreated" ).add();
        property( "role" ).tags( SbolUtil.getFeatureRoles() ).readOnly( "isCreated" ).add();
        addReadOnly( "name", "isCreated" );
        add( "title" );
        add( "private" );
    }
}