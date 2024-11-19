package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;
import java.net.URL;

import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.SBOLDocument;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SemanticController;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class MolecularSpecies extends SbolBase implements InitialElementProperties
{
    public static String[] types = new String[] {"Complex", "Protein", "Simple Chemical"};//, "Double-Stranded Nucleic Acid", "Macromolecule", "Protein",
    //            "Single-Stranded Nucleic Acid", "Unspecified"};

    private String name = "Complex";
    private String title = "Complex";
    private String type = "Complex";
    private String role = "";
    private boolean isCreated = false;

    public MolecularSpecies()
    {
        super( null );
    }

    public MolecularSpecies(Identified so)
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

    public boolean isCreated()
    {
        return isCreated;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        setName( DefaultSemanticController.generateUniqueName( diagram, name ) );
        Object doc = diagram.getAttributes().getValue( SbolUtil.SBOL_DOCUMENT_PROPERTY );
        if( ! ( doc instanceof SBOLDocument ) )
            return DiagramElementGroup.EMPTY_EG;

        ComponentDefinition cd = ( (SBOLDocument)doc ).createComponentDefinition( "biouml", getName(), "1",
                SbolUtil.getSpeciesURIByType( type ) );

        this.isCreated = true;

        this.setSbolObject( cd );
        Node node = new Node( compartment, this );
        node.setUseCustomImage( true );
        node.setLocation( location );
        String icon = SbolUtil.getSbolImagePath( cd );
        node.getAttributes()
                .add( new DynamicProperty( "node-image", URL.class, SbolDiagramReader.class.getResource( "resources/" +  icon + ".png" ) ) );

        SemanticController semanticController = diagram.getType().getSemanticController();
        if( !semanticController.canAccept( compartment, node ) )
        {
            return DiagramElementGroup.EMPTY_EG;
        }

        compartment.put( node );
        compartment.setShapeSize( new Dimension( compartment.getShapeSize().width + 48, compartment.getShapeSize().height ) );

        if( viewPane != null )
            viewPane.completeTransaction();

        return new DiagramElementGroup( node );
    }

}
