package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;

import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.Interaction;
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
import ru.biosoft.util.DPSUtils;

public class InteractionProperties extends SbolBase implements InitialElementProperties
{
    private String type = "Process";

    public InteractionProperties( String name)
    {
        this(name, true);
    }
    
    public InteractionProperties(String name, boolean isCreated)
    {
        super( name, isCreated );
    }

    public InteractionProperties(Identified so)
    {
        super( so );
    }

    @Override
    public Interaction getSbolObject()
    {
        return (Interaction)super.getSbolObject();
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

    protected Node doCreateInteraction(Compartment compartment, SBOLDocument doc, Point location)  throws Exception
    {
        ModuleDefinition moduleDefinition = SbolUtil.getDefaultModuleDefinition( (SBOLDocument)doc );
        Interaction interaction = moduleDefinition.createInteraction( getName(), SbolUtil.getInteractionURIByType(  getType() ) );
        this.setSbolObject( interaction );
        Node node = new Node( compartment, this );
        node.setTitle( getName() );
        node.getAttributes().add( DPSUtils.createHiddenReadOnly(SbolConstants.NODE_IMAGE, String.class, SbolUtil.getSbolImagePath(interaction) ) );
        node.setUseCustomImage( true );
        node.setLocation( location );
        node.setShapeSize( new Dimension( 15, 15 ) );
        return node;
    }
    
    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        setName( DefaultSemanticController.generateUniqueName( diagram, getName() ) );
        SBOLDocument doc = SbolUtil.getDocument( diagram );
        if( doc == null )
            return DiagramElementGroup.EMPTY_EG;
        this.setCreated( true );
        Node node = doCreateInteraction( compartment, doc, location );
        if( !diagram.getType().getSemanticController().canAccept( compartment, node ) )
            return DiagramElementGroup.EMPTY_EG;
        compartment.put( node );
        if( viewPane != null )
            viewPane.completeTransaction();
        return new DiagramElementGroup( node );
    }
}