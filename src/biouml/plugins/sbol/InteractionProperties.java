package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;

import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.Interaction;
import org.sbolstandard.core2.ModuleDefinition;
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

public class InteractionProperties extends SbolBase implements InitialElementProperties
{
    public static String[] types = new String[] {"Association", "Dissociation", "Process", "Unspecified"};

    private String name = "Process";
    private String title = "Process";
    private String type = "Process";

    private boolean isCreated = false;

    public InteractionProperties()
    {
        super( null );
    }

    public InteractionProperties(Identified so)
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

        this.isCreated = true;
 
        if ( ( (SBOLDocument)doc ).getRootModuleDefinitions().isEmpty())
            ((SBOLDocument)doc).createModuleDefinition( "biouml", "Main_module", "1" );
        
       ModuleDefinition moduleDefinition =  ( (SBOLDocument)doc ).getRootModuleDefinitions().iterator().next();
        
       Interaction interaction = moduleDefinition.createInteraction( name,   SbolUtil.getSpeciesURIByType( SbolUtil.TYPE_PROCESS ));
       this.setSbolObject(interaction);
        Node node = new Node( compartment, this );
        node.setUseCustomImage( true );
        node.setLocation( location );

        node.setShapeSize( new Dimension( 10, 10 ) );
        
        node.getAttributes().add( new DynamicProperty( "node-image", String.class, "process" ) );

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