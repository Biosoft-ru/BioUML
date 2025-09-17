package biouml.standard.diagram;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.JFrame;

import com.developmentontheedge.application.Application;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Base;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;
import biouml.standard.type.Type;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graphics.editor.ViewEditorPane;

/**
 * Base implementation of SemanticController for pathway network diagram
 */
public class PathwaySemanticController extends DefaultSemanticController
{

    protected static final Logger log = Logger.getLogger(PathwaySemanticController.class.getName());

    protected MessageBundle messageBundle = new MessageBundle();

    /**
     * Removes from diagram elements storing in the vector using semantic
     * controller.
     */
    protected void doRemove(Collection<? extends DiagramElement> v) throws Exception
    {
        for(DiagramElement de : v)
            remove(de);
    }

    /** Removes the diagram element and all related edges. */
    @Override
    public boolean remove(DiagramElement de) throws Exception
    {
        if( de instanceof Diagram )
            return false;

        if( de.getKernel() instanceof Stub.Note || de.getKernel() instanceof Stub.NoteLink )
            return super.remove(de);

        if( de instanceof Edge )
        {
            // we can remove only semantic relations or regulatory events
            Edge edge = (Edge)de;
            if( edge.getKernel().getType().equals(Type.TYPE_SEMANTIC_RELATION) )
            {
                edge.getOrigin().remove(edge.getName());
                return true;
            }

            // reaction edges can not be removed. We should remove the whole
            // reaction
            return false;
        }

        // to remove compartment we should remove all its edges
        if( de instanceof Compartment )
        {
            doRemove(((Compartment)de).stream().toList());
        }

        // remove node
        Node node = (Node)de;
        Base kernel = node.getKernel();
        DataCollection<?> parent = node.getOrigin();

        // for usual node we should remove all related reactions
        if( ! ( kernel.getType().equals(Type.TYPE_REACTION) ) )
        {
            List<Edge> edgesToRemove = new ArrayList<>();
            List<Node> toRemove = node.edges().peek( edgesToRemove::add ).flatMap( Edge::nodes )
                .filter( n -> n.getKernel().getType().equals( Type.TYPE_REACTION ) )
                .toList();
            for( Edge edge : edgesToRemove )
            {
                edge.getOrigin().remove(edge.getName());
            }
            doRemove(toRemove);
        }
        else
        {
            // now we are remove usual edges
            for(Edge edge : node.edges().toList())
            {
                edge.getCompartment().remove(edge.getName());
            }
        }
        parent.remove(node.getName());

        return true;
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        Class<? extends DataElement> typeClass = ((Class<?>)type).asSubclass(DataElement.class);
        DiagramElement de = null;
        Diagram diagram = (Diagram)viewEditor.getView().getModel();

        if( typeClass == Stub.Note.class )
        {
            String id = generateUniqueNodeName(parent, Type.TYPE_NOTE);
            return new DiagramElementGroup( new Node( parent, new Stub.Note( null, id ) ) );
        }

        else if( typeClass == Stub.NoteLink.class )
        {
            new CreateEdgeAction().createEdge(point, viewEditor, new NoteLinkEdgeCreator());
            return DiagramElementGroup.EMPTY_EG;
        }

        // Relations and reactions
        else if( typeClass == SemanticRelation.class )
        {
            CreateEdgeDialog dialog = CreateEdgeDialog.getSemanticRelationDialog(Module.getModule(parent), point, viewEditor);
            dialog.setVisible(true);
            return DiagramElementGroup.EMPTY_EG;
        }
        else if( type == Reaction.class )
        {
            ReactionPane reactionPane = new ReactionPane(diagram, parent, point, viewEditor);
            JFrame frame = Application.getApplicationFrame();
            CreateReactionDialog dialog = new CreateReactionDialog(frame, reactionPane);
            dialog.pack();
            dialog.setLocationRelativeTo(Application.getApplicationFrame());
            dialog.setVisible(true);
        }
        else
        {
            try
            {
                CreateDiagramElementDialog dialog = new CreateDiagramElementDialog(parent, typeClass);
                if( dialog.doModal() )
                    de = dialog.getNode();
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not create new diagram element, error: " + t, t);
            }
        }

        return new DiagramElementGroup( de );
    }

    @Override
    public DiagramElementGroup createInstance(Compartment compartment, Object type, Point point, Object properties)
    {
        //TODO: support other types if needed
        if( type == Reaction.class )
        {
            Reaction oldReaction = (Reaction)properties;
            List<SpecieReference> components = Arrays.asList( oldReaction.getSpecieReferences() );
            SemanticReactionProperties reactionProperties = new SemanticReactionProperties();//oldReaction.getName(), oldReaction.getKineticLaw(), components);
            reactionProperties.setReactionName( oldReaction.getName() );
            reactionProperties.setKineticlaw( oldReaction.getKineticLaw() );
            reactionProperties.setSpecieReferences( components );
            DiagramElementGroup elements;
            try
            {
                elements = reactionProperties.createElements( compartment, point, null );
                //reactionProperties.putReaction();
                return elements;
            }
            catch( Exception e )
            {
                return DiagramElementGroup.EMPTY_EG;
            }
        }
        return super.createInstance( compartment, type, point, properties );
    }



    protected Node getNode(Base kernel, Set<Node> elements)
    {
        DataCollection<?> origin = null;
        String kernelName = kernel.getName();
        for( Node node : elements )
        {
            if( origin == null )
                origin = node.getOrigin();
            // END
            if( node.getName().equals(kernelName) )
                return node;
        }
        Node node = new Node(origin, kernel);
        elements.add(node);
        return node;
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {

        if( type instanceof Class && Reaction.class.isAssignableFrom( (Class<?>)type ) )
            return new SemanticReactionProperties();
        return super.getPropertiesByType( compartment, type, point );
    }
    
    @Override
    public boolean isAcceptableForReaction(Node node)
    {
        return node.getKernel() instanceof Gene || node.getKernel() instanceof RNA || node.getKernel() instanceof Protein
                || node.getKernel() instanceof Substance;
    }
}
