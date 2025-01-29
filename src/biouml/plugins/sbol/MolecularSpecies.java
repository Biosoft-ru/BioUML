package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;

import org.sbolstandard.core2.AccessType;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.DirectionType;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.SBOLDocument;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class MolecularSpecies extends SbolBase implements InitialElementProperties
{
    public static String[] types = new String[] {SbolConstants.COMPLEX, SbolConstants.PROTEIN, SbolConstants.SIMPLE_CHEMICAL};

    private String type = SbolConstants.COMPLEX;
    private String role = "";

    public MolecularSpecies(String name, boolean isCreated)
    {
        super( name, isCreated );
    }
    
    public MolecularSpecies(String name)
    {
        this( name, true);
    }

    public MolecularSpecies(Identified so)
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

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        setName( DefaultSemanticController.generateUniqueName( diagram, getName() ) );
        SBOLDocument doc = SbolUtil.getDocument( diagram );
        if( doc == null )
            return DiagramElementGroup.EMPTY_EG;

        ComponentDefinition cd = ( (SBOLDocument)doc ).createComponentDefinition( getName(), "1", SbolUtil.getSpeciesURIByType( type ) );
        ModuleDefinition moduleDefinition = SbolUtil.getDefaultModuleDefinition( doc );
        moduleDefinition.createFunctionalComponent( getName() + "_fc", AccessType.PUBLIC, getName(), DirectionType.INOUT );

        this.setCreated( true );
        this.setSbolObject( cd );
        Node node = new Node( compartment, this );
        node.setTitle( getTitle() );
        node.setUseCustomImage( true );
        node.setLocation( location );
        node.setShapeSize( new Dimension( 60, 40 ) );
        SbolUtil.setSbolImage( node, cd );

        if( !diagram.getType().getSemanticController().canAccept( compartment, node ) )
            return DiagramElementGroup.EMPTY_EG;

        compartment.put( node );

        if( viewPane != null )
            viewPane.completeTransaction();

        return new DiagramElementGroup( node );
    }

}
