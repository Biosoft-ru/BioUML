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
        add( "title" );
        addWithTags( "type", Backbone.types );
        addWithTags( "role", Backbone.roles );
        addWithTags( "strandType", Backbone.strandTypes );
        addWithTags( "topologyType", Backbone.topologyTypes );
    }
}