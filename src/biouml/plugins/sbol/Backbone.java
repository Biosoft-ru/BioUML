package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;
import java.net.URI;

import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.SBOLDocument;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class Backbone extends SbolBase implements InitialElementProperties
{
    public static String[] strandTypes = new String[] {SbolConstants.STRAND_SINGLE, SbolConstants.STRAND_DOUBLE};
    public static String[] topologyTypes = new String[] {SbolConstants.TOPOLOGY_LINEAR, SbolConstants.TOPOLOGY_CIRCULAR};
    public static String[] types = new String[] {SbolConstants.TYPE_DNA, SbolConstants.TYPE_RNA};
    public static String[] roles = new String[] {SbolConstants.ROLE_SEQUENCE_FEATURE, SbolConstants.TYPE_RNA};

    private String strandType = SbolConstants.STRAND_SINGLE;
    private String topologyType = SbolConstants.TOPOLOGY_LINEAR;
    private String type = SbolConstants.TYPE_DNA;
    private String role = SbolConstants.ROLE_SEQUENCE_FEATURE;

    public Backbone(String name)
    {
        this(name, true);
    }
    
    public Backbone(String name, boolean isCreated)
    {
        super( name , isCreated);
    }

    public Backbone(Identified so)
    {
        super( so );
    }

    @PropertyName ( "Strand type" )
    public String getStrandType()
    {
        return strandType;
    }

    public void setStrandType(String strandType)
    {
        String oldValue = this.strandType;
        this.strandType = strandType;
        firePropertyChange( "strandType", oldValue, strandType );
    }

    @PropertyName ( "Topology type" )
    public String getTopologyType()
    {
        return topologyType;
    }

    public void setTopologyType(String topologyType)
    {
        String oldValue = this.topologyType;
        if( topologyType.equals( oldValue ) )
            return;
        this.topologyType = topologyType;
        try
        {
            SbolUtil.setTopologyType( getSbolObject(), topologyType );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        firePropertyChange( "topologyType", oldValue, topologyType );
    }

    @PropertyName ( "Type" )
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    @PropertyName ( "Role" )
    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }
    
    @Override
    public ComponentDefinition getSbolObject()
    {
        return (ComponentDefinition)super.getSbolObject();
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        setName( SbolUtil.generateUniqueName( diagram, getName() ) );
        SBOLDocument doc = SbolUtil.getDocument( diagram );
        if( doc == null )
            return DiagramElementGroup.EMPTY_EG;

        URI type = ( getType().equals( SbolConstants.TYPE_DNA ) ) ? ComponentDefinition.DNA_REGION : ComponentDefinition.RNA_REGION;
        ComponentDefinition cd = doc.createComponentDefinition( getName(), "1", type );
        SbolUtil.setTopologyType( cd, topologyType );
        this.setSbolObject( cd );
        this.setCreated( true );
        Compartment result = new Compartment( compartment, this );

        result.setShapeSize( new Dimension( 45 + 10, 45 + 20 ) );
        result.setLocation( location );
        compartment.put( result );

        if( viewPane != null )
            viewPane.completeTransaction();

        return new DiagramElementGroup( result );
    }
}