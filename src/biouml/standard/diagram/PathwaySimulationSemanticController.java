package biouml.standard.diagram;

import java.awt.Point;
import java.util.logging.Level;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.State;
import biouml.model.dynamics.Transition;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

public class PathwaySimulationSemanticController extends PathwaySemanticController
{

    private static Map<Class<?>, String> typeClassToDescription = new HashMap<>();
    static
    {
        typeClassToDescription.put( Equation.class, Type.MATH_EQUATION );
        typeClassToDescription.put( Event.class, Type.MATH_EVENT );
        typeClassToDescription.put( Function.class, Type.MATH_FUNCTION );
        typeClassToDescription.put( State.class, Type.MATH_STATE );
        typeClassToDescription.put( Constraint.class, Type.MATH_CONSTRAINT );
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {            
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled(false);
        try
        {
            if( type == Event.class )
            {
                Node node = new Node(parent, new Stub(null, generateUniqueNodeName(parent, Type.MATH_EVENT), Type.MATH_EVENT));
                node.setRole(new Event(node));
                return new DiagramElementGroup( node );
            }
            else if( type == Equation.class )
            {
                Node node = new Node(parent, new Stub(null, generateUniqueNodeName(parent, Type.MATH_EQUATION), Type.MATH_EQUATION));
                node.setRole(new Equation(node));
                node.setShowTitle(false);
                return new DiagramElementGroup( node );
            }
            else if( type == Function.class )
            {
                Node node = new Node(parent, new Stub(null, generateUniqueNodeName(parent, Type.MATH_FUNCTION), Type.MATH_FUNCTION));
                node.setRole(new Function(node));
                node.setShowTitle(false);
                return new DiagramElementGroup( node );
            }
            else if( type == Constraint.class )
            {
                Node node = new Node(parent, new Stub(null, generateUniqueNodeName(parent, Type.MATH_CONSTRAINT), Type.MATH_CONSTRAINT));
                node.setRole(new Constraint(node));
                node.setShowTitle(false);
                return new DiagramElementGroup( node );
            }
            else if( type == State.class )
            {
                Node node = new Node(parent, new Stub(null, generateUniqueNodeName(parent, Type.MATH_STATE), Type.MATH_STATE));
                State state = new State(node);
                state.addOnEntryAssignment(new Assignment("unknown", "0"), false);
                state.addOnExitAssignment(new Assignment("unknown", "0"), false);
                node.setRole(state);
                return new DiagramElementGroup( node );
            }
            else if( type == Transition.class )
            {
                CreateEdgeDialog.getTransitionDialog(Module.getModule(parent), pt, viewEditor).setVisible(true);
                return DiagramElementGroup.EMPTY_EG;
            }

            try
            {
                Object properties = getPropertiesByType( parent, type, pt );
                if( properties != null )
                {
                    if( properties instanceof ReactionInitialProperties )
                    {
                        Diagram diagram = (Diagram)viewEditor.getView().getModel();
                        new CreateReactionDialog(Application.getApplicationFrame(), new ReactionPane(diagram, parent, pt, viewEditor, (ReactionInitialProperties)properties));
                        return DiagramElementGroup.EMPTY_EG;
                    }
                    else if (properties instanceof InitialElementProperties)
                    {
                        if( new PropertiesDialog(Application.getApplicationFrame(), "New element", properties).doModal() )
                                ( (InitialElementProperties)properties ).createElements(parent, pt, viewEditor);
                        return DiagramElementGroup.EMPTY_EG;
                    }
                }
            }
            catch( Throwable t )
            {
                throw ExceptionRegistry.translateException( t );
            }

            DiagramElement de = super.createInstance( parent, type, pt, viewEditor ).get( 0 );
            if( de != null && type != Reaction.class && type != Stub.Note.class && type != Stub.NoteLink.class)
                de.setRole(new VariableRole(de, 0));

            return new DiagramElementGroup( de );
        }
        finally
        {
            parent.setNotificationEnabled(isNotificationEnabled);
        }
    }


    //TODO: support of all diagram element types, now only listed in <b>typeClassToDescription</b> types are supported
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, Point point, Object properties)
    {
        try
        {
            if( type instanceof Class )
                return createNodeInstance( compartment, (Class<?>)type, properties, point );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While creating instance of type " + type.toString(), e);
        }
        return new DiagramElementGroup();
    }
    /**
     * Internal method for creating Node with Stub kernel without using any dialogs
     * Is used by both types of createInstance methods
     */
    private DiagramElementGroup createNodeInstance(Compartment parent, Class<?> type, Object properties, Point point)
    {
        try
        {
            if( type.equals(Reaction.class) )
            {
                Reaction oldReaction = (Reaction)properties;
                List<SpecieReference> components = Arrays.asList( oldReaction.getSpecieReferences() );
                ReactionInitialProperties reactionProperties = new ReactionInitialProperties();
                reactionProperties.setSpecieReferences(components);
                reactionProperties.setKineticlaw(new KineticLaw(oldReaction.getFormula()));
                DiagramElementGroup elements = reactionProperties.createElements( parent, point, null );
                return elements;
                //TODO: put reaction and other nodes outside this method !!!
                //reactionProperties.putResults(elements);
                //return (Node)elements.stream().filter(Util::isReaction).findAny().orElse(null); //return reaction here TODO: maybe return all elements
            }
            if( typeClassToDescription.containsKey(type) )
            {
                String typeDescr = typeClassToDescription.get(type);
                Node node = new Node(parent, new Stub(null, generateUniqueNodeName(parent, typeDescr), typeDescr));
                Role role = ( properties instanceof Role ) ? ( (Role)properties ).clone(node) : (Role)type
                        .getConstructor(DiagramElement.class).newInstance(node);
                node.setRole(role);
                node.setLocation(point);
                return new DiagramElementGroup( node );
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "While creating instance of type " + type.toString(), ex);
        }
        return new DiagramElementGroup();
    }


    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        if( Util.isBlock(compartment))
            return de.getKernel() != null && de.getKernel().getType().equals(Type.MATH_EQUATION);

        else if(  Util.isPort( de ) &&  ! ( compartment instanceof Diagram ))
                return false;

        Role role = de.getRole();
        if( role instanceof Event || role instanceof Equation || role instanceof Function || role instanceof State || role instanceof Constraint)
            return true;

        if( de instanceof Edge && role instanceof Transition )
            return ( (Edge)de ).nodes().allMatch( n -> n.getRole() instanceof State );

        return super.canAccept(compartment, de);
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        Class<?> typeClass = null;

        if (type instanceof Class)
        {
            typeClass = (Class<?>)type;
        }
        else if( type instanceof String )
        {
            if (type.equals( Type.TYPE_BLOCK ))
                return new StubInitialProperties( compartment, Type.TYPE_BLOCK, Compartment.class );
            else if( Type.TYPE_TABLE.equals( type ) )
            {
                String name = generateUniqueName( Diagram.getDiagram( compartment ), "table", false );
                return new SimpleTableElementProperties( name );
            }
            
            try
            {
                typeClass = ClassLoading.loadClass( (String)type ); //for web version
            }
            catch( Exception ex )
            {
            }
        }
        if (typeClass == null)
            return null;

        if (Reaction.class.isAssignableFrom(typeClass))
            return new ReactionInitialProperties();
        if( Stub.ConnectionPort.class.isAssignableFrom(typeClass))
            return new PortProperties(Diagram.getDiagram( compartment ), typeClass.asSubclass( Stub.ConnectionPort.class ));       
        
        return super.getPropertiesByType(compartment, type, point);
    }

    @Override
    protected Node changeNodeParent(Node oldNode, Compartment newParent) throws Exception
    {
        Node node = super.changeNodeParent( oldNode, newParent );
        if( node.getKernel() instanceof Reaction )
            return node;
        //adjust specie reference
        node.edges().map(e->e.getKernel()).select(SpecieReference.class).forEach(sr->sr.setSpecie( node.getCompleteNameInDiagram()));
        return node;
    }

    @Override
    public Node cloneNode(Node node, String newName, Point location)
    {
        Node newNode = super.cloneNode( node, newName, location );
        Role role = node.getRole();
        if (role instanceof VariableRole)
            ((VariableRole)role).addAssociatedElement(newNode);
        return newNode;
    }
    
    @Override
    public boolean isAcceptableForReaction(Node node)
    {
        return node.getRole() instanceof VariableRole && !(node.getKernel() instanceof biouml.standard.type.Compartment);
    }
}