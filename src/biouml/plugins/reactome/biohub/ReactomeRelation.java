package biouml.plugins.reactome.biohub;

import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.HubEdge;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.access.exception.Assert;

public class ReactomeRelation extends ReactomeElement implements HubEdge
{
    private final String type;

    public ReactomeRelation(String acc, String type)
    {
        super( acc );
        Assert.notNull( "type", type );
        this.type = type;
    }

    @Override
    public int hashCode()
    {
        return super.hashCode() * 31 + type.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( !super.equals( obj ) )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        return type.equals( ( (ReactomeRelation)obj ).type );
    }

    @Override
    public Element createElement(KeyNodesHub<?> hub)
    {
        return new Element( ( (ReactomeSqlBioHub)hub ).getCompleteElementPath( this.toString() ) );
    }

    @Override
    public String getRelationType(boolean upStream)
    {
        if( upStream )
        {
            switch( type )
            {
                case RelationType.MODIFIER:
                    return RelationType.MODIFIER;
                default:
                    return RelationType.REACTANT;
            }
        }
        else
        {
            return RelationType.PRODUCT;
        }
    }
}
