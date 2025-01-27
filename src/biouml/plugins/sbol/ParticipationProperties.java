package biouml.plugins.sbol;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.Participation;

import com.developmentontheedge.beans.annot.PropertyName;

public class ParticipationProperties extends SbolBase
{
    private String role = SbolConstants.STIMULATION;

    public ParticipationProperties()
    {
        super( null );
    }

    public ParticipationProperties(Identified so)
    {
        super( so );
        if ( so != null )
        {
            if ( so instanceof Participation )
            {
                Set<URI> roles = ((Participation) so).getRoles();
                for ( URI r : roles )
                {
                    String type = SbolUtil.getParticipationStringType(r);
                    if ( type != null )
                    {
                        role = type;
                        break;
                    }
                }
            }
        }
    }

    @PropertyName ( "Role" )
    public String getType()
    {
        return role;
    }

    public void setType(String role)
    {
        Object oldValue = this.role;
        this.role = role;
        try
        {
            Participation participation = (Participation)getSbolObject();
            if( participation != null )
            {
                Set<URI> roles = new HashSet<URI>();
                roles.add( SbolUtil.getParticipationURIByType( role ) );
                participation.setRoles( roles );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        firePropertyChange( "type", oldValue, role );
    }
}