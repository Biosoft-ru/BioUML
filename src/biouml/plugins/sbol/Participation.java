package biouml.plugins.sbol;

import org.sbolstandard.core2.Identified;

import com.developmentontheedge.beans.annot.PropertyName;

public class Participation extends SbolBase
{
//    public static String[] roles = new String[] {"Association", "Dissociation", "Process", "Unspecified"};
//
//    public static String[] inhibitionRoles = new String[] {"Inhibitor", "Inhibited"};
//    public static String[] simulationRoles = new String[] {""}
//    public static String[] roles
//    public static String[] roles
    
    private String name = "Participation";
    private String title = "Participation";
    private String type = SbolUtil.TYPE_STIMULATION;

    public Participation()
    {
        super( null );
    }

    public Participation(Identified so)
    {
        super( so );
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange( "name", oldValue, name );
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        Object oldValue = this.title;
        this.title = title;
        firePropertyChange( "title", oldValue, title );
    }

    @PropertyName ( "Type" )
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        Object oldValue = this.type;
        this.type = type;
        firePropertyChange( "type", oldValue, type );
    }
}