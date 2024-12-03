package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.sbolstandard.core2.AccessType;
import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.RestrictionType;
import org.sbolstandard.core2.RoleIntegrationType;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SequenceConstraint;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SemanticController;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class SequenceFeature extends SbolBase implements InitialElementProperties
{
    private String type = "DNA";
    private String role = "Promoter";
    private String name = "Promoter";
    private String title = "Promoter";
    private boolean isCreated = false;
    private boolean isPrivate = false;

    public SequenceFeature()
    {
        super( null );
        this.setRole( SbolUtil.getFeatureRoles()[0] );
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

    @PropertyName ( "Role" )
    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        Object oldValue = this.role;
        this.role = role;
        setName( role.replace( " ", "_" ) );
        setTitle( role );
        firePropertyChange( "role", oldValue, role );

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
        
        if (compartment instanceof Diagram) //try to fix wrong compartment
        {
           compartment = diagram.recursiveStream().select( Compartment.class ).filter( de->de.getKernel() instanceof Backbone ).findAny().orElse( diagram );
        }
        setName( DefaultSemanticController.generateUniqueName( diagram, name ) ); 
        Object doc = diagram.getAttributes().getValue( SbolUtil.SBOL_DOCUMENT_PROPERTY );
        if( ! ( doc instanceof SBOLDocument ) )
            return DiagramElementGroup.EMPTY_EG;

        //        URI uriType = ( getType().equals( "DNA" ) ) ? ComponentDefinition.DNA_REGION : ComponentDefinition.RNA_REGION;
        URI uriRole = SbolUtil.getURIByRole( role );
        ComponentDefinition cd = ( (SBOLDocument)doc ).createComponentDefinition( "biouml", getName(), "1",
                ComponentDefinition.DNA_REGION );
        cd.addRole( uriRole );

        if ( compartment.getKernel() instanceof SbolBase )
        {
            Identified so = ((SbolBase) compartment.getKernel()).getSbolObject();
            if ( so instanceof ComponentDefinition )
            {
                ComponentDefinition parentCd = ((ComponentDefinition) so);
                Component component = parentCd.createComponent(getName() + "_1", isPrivate ? AccessType.PRIVATE : AccessType.PUBLIC, cd.getIdentity());
                component.setRoleIntegration(RoleIntegrationType.OVERRIDEROLES);
                component.addRole(uriRole);
                //Add order constraint
                Component lastComponent = getLastComponent(parentCd);
                if ( lastComponent != null )
                    parentCd.createSequenceConstraint(getName() + "_sc", RestrictionType.PRECEDES, lastComponent.getDisplayId(), component.getDisplayId());
            }
        }

        this.isCreated = true;

        int y = compartment.getLocation().y;
        int x = compartment.isEmpty() ? compartment.getLocation().x
                : StreamEx.of( compartment.getNodes() ).mapToInt( n -> n.getLocation().x ).max().orElse( 0 ) + 48;

        this.setSbolObject(cd);
        Node node = new Node( compartment, this );
        node.setUseCustomImage( true );
        Point nodeLocation = new Point( x, y );
        node.setLocation( nodeLocation );
        String icon = SbolUtil.getSbolImagePath(cd);
        node.getAttributes().add(new DynamicProperty("node-image", String.class, icon));

        SemanticController semanticController = diagram.getType().getSemanticController();
        if( !semanticController.canAccept(compartment, node) )
        {
            return DiagramElementGroup.EMPTY_EG;
        }
        
        compartment.put( node );
        compartment.setShapeSize( new Dimension( compartment.getShapeSize().width + 48, compartment.getShapeSize().height ) );

        if( viewPane != null )
            viewPane.completeTransaction();

        return new DiagramElementGroup( node );
    }

    private Component getLastComponent(ComponentDefinition cd)
    {
        Component lastComponent = null;
        if ( !cd.getComponents().isEmpty() )
        {
            Set<URI> objects = new HashSet<>();
            Set<URI> subjects = new HashSet<>();
            for ( SequenceConstraint sc : cd.getSequenceConstraints() )
            {
                if ( RestrictionType.PRECEDES.equals(sc.getRestriction()) )
                {
                    objects.add(sc.getObjectURI());
                    subjects.add(sc.getSubjectURI());
                }
            }
            objects.removeAll(subjects);
            if ( objects.size() == 1 )
                lastComponent = cd.getComponent(objects.iterator().next());
        }
        return lastComponent;
    }

    public boolean isCreated()
    {
        return isCreated;
    }

}
