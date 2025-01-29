package biouml.plugins.sbol;

import ru.biosoft.util.bean.BeanInfoEx2;

public class BackboneBeanInfo extends BeanInfoEx2<Backbone>
{
    public BackboneBeanInfo()
    {
        super( Backbone.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "name", "isCreated" );
        addWithTags( "type", Backbone.types );
        addWithTags( "role", Backbone.roles );
        addWithTags( "strandType", SbolConstants.strandTypes );
        addWithTags( "topologyType", SbolConstants.topologyTypes );
    }
}