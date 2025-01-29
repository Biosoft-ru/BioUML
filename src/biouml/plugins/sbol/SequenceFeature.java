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

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class SequenceFeature extends SbolBase implements InitialElementProperties
{
    private String type = SbolConstants.TYPE_DNA;
    private String role = SbolConstants.PROMOTER;
    
    private boolean isPrivate = false;

    public SequenceFeature( String name)
    {
        this(name, true);
    }
    
    public SequenceFeature(String name, boolean isCreated)
    {
        super( name, isCreated );
    }
    
    public SequenceFeature(Identified so)
    {
        super( so );
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

        if( compartment.getKernel() instanceof SequenceFeature )
        {
            compartment = compartment.getCompartment();
        }

        if( compartment instanceof Diagram || ! ( compartment.getKernel() instanceof Backbone ) ) //try to fix wrong compartment
        {
            compartment = diagram.recursiveStream().select( Compartment.class ).filter( de -> de.getKernel() instanceof Backbone ).findAny()
                    .orElse( diagram );
        }

        setName( DefaultSemanticController.generateUniqueName( diagram, getName() ) );
        Object doc = SbolUtil.getDocument( diagram );
        if( doc == null )
            return DiagramElementGroup.EMPTY_EG;

        //        URI uriType = ( getType().equals( "DNA" ) ) ? ComponentDefinition.DNA_REGION : ComponentDefinition.RNA_REGION;
        URI uriRole = SbolUtil.getURIByRole( role );
        ComponentDefinition cd = ( (SBOLDocument)doc ).createComponentDefinition( getName(), "1", ComponentDefinition.DNA_REGION );
        cd.addRole( uriRole );

        if( SbolUtil.isSbol( compartment ) )
        {
            Identified so = SbolUtil.getSbolObject(compartment);
            if( so instanceof ComponentDefinition )
            {
                ComponentDefinition parentCd = ( (ComponentDefinition)so );
                Component component = parentCd.createComponent( getName() + "_1", isPrivate ? AccessType.PRIVATE : AccessType.PUBLIC,
                        cd.getIdentity() );
                component.setRoleIntegration( RoleIntegrationType.OVERRIDEROLES );
                component.addRole( uriRole );
                //Add order constraint
                Component lastComponent = getLastComponent( parentCd );
                if( lastComponent != null )
                    parentCd.createSequenceConstraint( getName() + "_sc", RestrictionType.PRECEDES, lastComponent.getDisplayId(),
                            component.getDisplayId() );
            }
        }

        this.setCreated( true );

        int y = compartment.getLocation().y + 10;
        int x = compartment.isEmpty() ? compartment.getLocation().x + 5
                : StreamEx.of( compartment.getNodes() ).mapToInt( n -> n.getLocation().x ).max().orElse( 0 ) + 45;

        setSbolObject( cd );
        Compartment node = new Compartment( compartment, this );
        node.setTitle( getTitle() );
        node.setUseCustomImage( true );
        node.setLocation( new Point( x, y ) );
        node.setShapeSize( new Dimension( 45, 45 ) );
        SbolUtil.setSbolImage( node, cd );

        if( !diagram.getType().getSemanticController().canAccept( compartment, node ) )
            return DiagramElementGroup.EMPTY_EG;

        compartment.put( node );
        
        int width = compartment.isEmpty() ? 45 + 10 : compartment.getShapeSize().width + 45;
        compartment.setShapeSize( new Dimension( width, compartment.getShapeSize().height ) );

        if( viewPane != null )
            viewPane.completeTransaction();

        return new DiagramElementGroup( node );
    }

    private Component getLastComponent(ComponentDefinition cd)
    {
        Component lastComponent = null;
        if( !cd.getComponents().isEmpty() )
        {
            Set<URI> objects = new HashSet<>();
            Set<URI> subjects = new HashSet<>();
            for( SequenceConstraint sc : cd.getSequenceConstraints() )
            {
                if( RestrictionType.PRECEDES.equals( sc.getRestriction() ) )
                {
                    objects.add( sc.getObjectURI() );
                    subjects.add( sc.getSubjectURI() );
                }
            }
            objects.removeAll( subjects );
            if( objects.size() == 1 )
                lastComponent = cd.getComponent( objects.iterator().next() );
        }
        return lastComponent;
    }
}