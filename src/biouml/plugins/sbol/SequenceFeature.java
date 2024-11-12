package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;
import java.net.URI;
import java.net.URL;

import org.sbolstandard.core2.AccessType;
import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.RoleIntegrationType;
import org.sbolstandard.core2.SBOLDocument;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class SequenceFeature extends SbolBase implements InitialElementProperties
{
    private String type = "DNA";
    private String role = "Sequence feature";
    private String name = "Feature";
    private String title = "Feature";
    private boolean isCreated = false;
    private boolean isPrivate = false;

    public SequenceFeature()
    {
        super( null );
    }

    public SequenceFeature(Identified so)
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
        this.name = name;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
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
        this.name = role.replace( " ", "_" );
        this.title = role;
    }

    @PropertyName ( "Private" )
    public boolean isPrivate()
    {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate)
    {
        this.isPrivate = isPrivate;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        Object doc = diagram.getAttributes().getValue( SbolUtil.SBOL_DOCUMENT_PROPERTY );
        if( ! ( doc instanceof SBOLDocument ) )
            return DiagramElementGroup.EMPTY_EG;

        //        URI uriType = ( getType().equals( "DNA" ) ) ? ComponentDefinition.DNA_REGION : ComponentDefinition.RNA_REGION;
        URI uriRole = SbolUtil.getURIByRole( role );
        ComponentDefinition cd = ( (SBOLDocument)doc ).createComponentDefinition( "biouml", getName(), "1",
                ComponentDefinition.DNA_REGION );

        cd.addRole( uriRole );
        Component component = cd.createComponent( getName(), isPrivate ? AccessType.PRIVATE : AccessType.PUBLIC,
                ComponentDefinition.DNA_REGION );
        component.setRoleIntegration( RoleIntegrationType.OVERRIDEROLES );
        component.addRole( uriRole );

        this.isCreated = true;

        int y = compartment.getLocation().y + 5;
        int x = compartment.isEmpty() ? compartment.getLocation().x
                : StreamEx.of( compartment.getNodes() ).mapToInt( n -> n.getLocation().x ).max().orElse( 0 ) + 48;

        this.setSbolObject( component );
        Node node = new Node( compartment, this );
        node.setUseCustomImage( true );
        Point nodeLocation = new Point( x, y );
        node.setLocation( nodeLocation );
        String icon = SbolUtil.getSbolImagePath( cd );
        node.getAttributes()
                .add( new DynamicProperty( "node-image", URL.class, SbolDiagramReader.class.getResource( "resources/" + icon + ".png" ) ) );

        compartment.put( node );
        compartment.setShapeSize( new Dimension( compartment.getShapeSize().width + 48, compartment.getShapeSize().height ) );
        
        if( viewPane != null )
            viewPane.completeTransaction();

        return new DiagramElementGroup( node );
    }

    public boolean isCreated()
    {
        return isCreated;
    }

}
