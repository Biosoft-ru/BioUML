package biouml.standard.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Base;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;
import biouml.standard.type.Type;
import ru.biosoft.access.core.CollectionFactory;

/**
 * Headless creation of the elements of a mathematical model — species, compartments, reactions and
 * equations — through the diagram type's own semantic controller, so the result carries the same
 * roles, model variables and attributes as an element created in the diagram editor (for example a
 * reaction gets its {@code $$rate_<name>} variable and rate equations, a species its variable role).
 *
 * <p>Used by the {@code model} JavaScript host object and the MCP diagram tools. Bad input is reported
 * with an {@link IllegalArgumentException} carrying a readable message; nothing is created then.
 * The diagram is changed in memory only: the caller saves it.</p>
 */
public class ModelElementFactory
{
    public static final List<String> SPECIE_REFERENCE_ROLES = Arrays.asList( SpecieReference.REACTANT, SpecieReference.PRODUCT,
            SpecieReference.MODIFIER );

    public static final List<String> EQUATION_TYPES = Arrays.asList( Equation.TYPE_SCALAR, Equation.TYPE_RATE,
            Equation.TYPE_ALGEBRAIC, Equation.TYPE_INITIAL_ASSIGNMENT );

    private ModelElementFactory()
    {
    }

    // ---------------------------------------------------------------- lookup

    /** Converts "a/b/c" (repository style) into "a.b.c" (diagram style). */
    private static String toDiagramStyle(String path)
    {
        return path.replace( '/', '.' );
    }

    /**
     * Finds a compartment of the diagram by its name or path ("default", "cell/nucleus" or "cell.nucleus").
     * An empty or null path denotes the diagram itself.
     */
    public static @Nonnull Compartment findCompartment(@Nonnull Diagram diagram, String path)
    {
        if( path == null || path.isEmpty() )
            return diagram;
        DiagramElement de = diagram.findDiagramElement( toDiagramStyle( path ) );
        if( ! ( de instanceof Compartment ) || de.getKernel() instanceof Specie )
            throw new IllegalArgumentException( "No compartment '" + path + "' in diagram '" + diagram.getName() + "'" );
        return (Compartment)de;
    }

    /**
     * Splits a qualified element name "compartment/name" into the parent compartment and the name.
     * @return two strings: the parent path (empty for the diagram itself) and the element name
     */
    public static String[] splitQualifiedName(@Nonnull String qualifiedName)
    {
        int index = qualifiedName.lastIndexOf( '/' );
        if( index < 0 )
            return new String[] {"", qualifiedName};
        return new String[] {qualifiedName.substring( 0, index ), qualifiedName.substring( index + 1 )};
    }

    /**
     * Finds a node by its name ("s1"), its path in the diagram ("default.s1" or "default/s1") or its
     * model variable ("$default.s1"). Returns null if nothing matches.
     */
    public static Node findNode(@Nonnull Diagram diagram, String id)
    {
        if( id == null || id.isEmpty() )
            return null;
        Node node = diagram.findNode( toDiagramStyle( id ) );
        if( node != null )
            return node;
        if( diagram.getRole() instanceof EModel )
        {
            Variable var = diagram.getRole( EModel.class ).getVariable( id );
            if( var instanceof VariableRole && ( (VariableRole)var ).getDiagramElement() instanceof Node )
                return (Node) ( (VariableRole)var ).getDiagramElement();
        }
        if( id.startsWith( "$" ) )
            return diagram.findNode( toDiagramStyle( id.substring( 1 ) ) );
        return null;
    }

    /**
     * Finds a node that carries a model variable (a species or a compartment) — see {@link #findNode}.
     * @throws IllegalArgumentException if there is no such node
     */
    public static @Nonnull Node findVariableNode(@Nonnull Diagram diagram, String id)
    {
        Node node = findNode( diagram, id );
        if( node == null )
            throw new IllegalArgumentException( "No species '" + id + "' in diagram '" + diagram.getName() + "'" );
        if( ! ( node.getRole() instanceof VariableRole ) )
            throw new IllegalArgumentException( "Element '" + id + "' of diagram '" + diagram.getName() + "' is not a species" );
        return node;
    }

    /**
     * Resolves a variable given by its model name ("k1", "$default.s1") or by the name or path of its
     * species node ("s1", "default.s1") to the variable's model name. Returns null if not found.
     */
    public static String resolveVariableName(@Nonnull Diagram diagram, String id)
    {
        if( id == null || id.isEmpty() || ! ( diagram.getRole() instanceof EModel ) )
            return null;
        Variable var = diagram.getRole( EModel.class ).getVariable( id );
        if( var != null )
            return var.getName();
        Node node = findNode( diagram, id );
        if( node != null && node.getRole() instanceof VariableRole )
            return node.getRole( VariableRole.class ).getName();
        return null;
    }

    private static EModel getEModel(Diagram diagram)
    {
        if( ! ( diagram.getRole() instanceof EModel ) )
            throw new IllegalArgumentException( "Diagram '" + diagram.getName() + "' has no mathematical model" );
        return diagram.getRole( EModel.class );
    }

    private static void checkNewName(Compartment parent, String name)
    {
        if( name == null || name.isEmpty() )
            throw new IllegalArgumentException( "Element name is empty" );
        if( name.contains( "/" ) || name.contains( "." ) )
            throw new IllegalArgumentException( "Element name '" + name + "' must not contain '/' or '.'" );
        Diagram diagram = Diagram.getDiagram( parent );
        if( diagram.containsRecursively( name ) )
            throw new IllegalArgumentException( "Diagram '" + diagram.getName() + "' already contains an element named '" + name + "'" );
    }

    private static void put(Compartment parent, DiagramElement de)
    {
        SemanticController controller = Diagram.getDiagram( parent ).getType().getSemanticController();
        if( !controller.canAccept( parent, de ) )
            throw new IllegalArgumentException( "Element '" + de.getName() + "' can not be placed into '" + parent.getName() + "'" );
        parent.put( de );
    }

    // ---------------------------------------------------------------- species and compartments

    /**
     * Adds a species with the given initial value. In a diagram with a mathematical model the species
     * gets a variable role (its variable is {@code $<compartment>.<name>}).
     *
     * @param parent compartment (or the diagram) to put the species into
     * @param name species name
     * @param kernel kernel to use when the diagram type does not create species itself (null: a Substance)
     * @param initialValue initial value of the species variable
     * @param location node location, may be null
     */
    public static @Nonnull Node addSpecies(@Nonnull Compartment parent, String name, Base kernel, double initialValue, Point location)
    {
        checkNewName( parent, name );
        Diagram diagram = Diagram.getDiagram( parent );
        SemanticController controller = diagram.getType().getSemanticController();
        Node node = null;
        if( controller instanceof CreatorElementWithName )
        {
            DiagramElement de = ( (CreatorElementWithName)controller ).createInstance( parent, Specie.class, name,
                    location == null ? new Point() : location, null ).getElement();
            if( de instanceof Node && de.getRole() instanceof VariableRole )
                node = (Node)de;
        }
        if( node == null )
        {
            node = new Node( parent, name, kernel != null ? kernel : new Substance( null, name ) );
            if( diagram.getRole() instanceof EModel )
                node.setRole( new VariableRole( node, initialValue ) );
        }
        if( location != null )
            node.setLocation( location );
        if( node.getRole() instanceof VariableRole )
            node.getRole( VariableRole.class ).setInitialValue( initialValue );
        put( parent, node );
        return node;
    }

    /**
     * Adds a compartment. In a diagram with a mathematical model the compartment gets a variable role
     * (its size, 1 by default).
     */
    public static @Nonnull Compartment addCompartment(@Nonnull Compartment parent, String name, Point location, Dimension size)
    {
        checkNewName( parent, name );
        Diagram diagram = Diagram.getDiagram( parent );
        SemanticController controller = diagram.getType().getSemanticController();
        Compartment compartment = null;
        if( controller instanceof CreatorElementWithName )
        {
            DiagramElement de = ( (CreatorElementWithName)controller ).createInstance( parent, biouml.standard.type.Compartment.class, name,
                    location == null ? new Point() : location, null ).getElement();
            if( de instanceof Compartment )
                compartment = (Compartment)de;
        }
        if( compartment == null )
        {
            compartment = new Compartment( parent, name, new biouml.standard.type.Compartment( null, name ) );
            if( diagram.getRole() instanceof EModel )
                compartment.setRole( new VariableRole( compartment, 1.0 ) );
        }
        if( location != null )
            compartment.setLocation( location );
        Dimension shape = compartment.getShapeSize();
        if( size != null )
            compartment.setShapeSize( size );
        else if( shape == null || shape.width == 0 || shape.height == 0 )
            compartment.setShapeSize( new Dimension( 300, 200 ) );
        put( parent, compartment );
        return compartment;
    }

    // ---------------------------------------------------------------- reactions

    /**
     * Creates a reaction participant (a prototype to pass to {@link #addReaction}).
     *
     * @param species name, path or variable of the species node
     * @param role reactant, product or modifier
     * @param stoichiometry stoichiometric coefficient, null for the default ("1")
     */
    public static @Nonnull SpecieReference createSpecieReference(@Nonnull Diagram diagram, String species, String role, String stoichiometry)
    {
        if( !SPECIE_REFERENCE_ROLES.contains( role ) )
            throw new IllegalArgumentException( "Unknown role of species '" + species + "' in reaction: '" + role + "', expected one of "
                    + SPECIE_REFERENCE_ROLES );
        Node node = findVariableNode( diagram, species );
        String path = node.getCompleteNameInDiagram();
        SpecieReference reference = new SpecieReference( null, path, role );
        reference.setSpecie( path );
        if( stoichiometry != null && !stoichiometry.isEmpty() )
            reference.setStoichiometry( stoichiometry );
        return reference;
    }

    /**
     * Adds a reaction with its participants and kinetic law. The diagram type's reaction properties
     * create the reaction node, its edges, the {@code $$rate_<name>} variable and the rate equations of
     * the participants (and, for SBGN diagrams, source/sink glyphs).
     *
     * @param parent compartment to put the reaction node into
     * @param name reaction name, or null to generate one
     * @param formula kinetic law, null or empty for "0"
     * @param components participants, see {@link #createSpecieReference}
     * @param location node location, null to place it between the participants
     * @return the reaction node
     */
    public static @Nonnull Node addReaction(@Nonnull Compartment parent, String name, String formula, List<SpecieReference> components,
            Point location) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( parent );
        getEModel( diagram );
        if( name != null )
            checkNewName( parent, name );

        List<SpecieReference> prototypes = new ArrayList<>();
        List<Node> participants = new ArrayList<>();
        if( components != null )
        {
            for( int i = 0; i < components.size(); i++ )
            {
                SpecieReference sr = components.get( i );
                if( sr == null )
                    throw new IllegalArgumentException( "Reaction participant #" + ( i + 1 ) + " is null (unknown species?)" );
                if( !SPECIE_REFERENCE_ROLES.contains( sr.getRole() ) )
                    throw new IllegalArgumentException( "Unknown role of reaction participant #" + ( i + 1 ) + ": '" + sr.getRole() + "'" );
                Node node = findVariableNode( diagram, sr.getSpecie() != null ? sr.getSpecie() : sr.getName() );
                String path = node.getCompleteNameInDiagram();
                SpecieReference prototype = new SpecieReference( null, path, sr.getRole() );
                prototype.setSpecie( path );
                prototype.setStoichiometry( sr.getStoichiometry() );
                if( sr.getModifierAction() != null )
                    prototype.setModifierAction( sr.getModifierAction() );
                prototypes.add( prototype );
                participants.add( node );
            }
        }

        Point point = location;
        if( point == null )
        {
            point = new Point();
            if( !participants.isEmpty() )
            {
                for( Node node : participants )
                    point.translate( node.getLocation().x, node.getLocation().y );
                point.setLocation( point.x / participants.size(), point.y / participants.size() );
            }
        }

        ReactionInitialProperties properties = getReactionProperties( parent, point );
        for( Node node : participants )
            if( !properties.acceptForReaction( node ) )
                throw new IllegalArgumentException( "Element '" + node.getName() + "' can not take part in a reaction" );
        properties.setReactionName( name );
        properties.setKineticlaw( new KineticLaw( formula == null || formula.isEmpty() ? "0" : formula ) );
        properties.setSpecieReferences( prototypes );
        DiagramElementGroup elements = properties.createElements( parent, point, null );
        return elements.nodesStream().findFirst( n -> n.getKernel() instanceof Reaction )
                .orElseThrow( () -> new IllegalArgumentException( "Diagram type of '" + diagram.getName() + "' did not create a reaction" ) );
    }

    private static ReactionInitialProperties getReactionProperties(Compartment parent, Point point)
    {
        Object properties = Diagram.getDiagram( parent ).getType().getSemanticController().getPropertiesByType( parent, Reaction.class, point );
        return properties instanceof ReactionInitialProperties ? (ReactionInitialProperties)properties : new ReactionInitialProperties();
    }

    /**
     * Adds a participant to an existing reaction: a species reference in the reaction, an edge between
     * the reaction and species nodes and, for reactants and products, the edge's rate equation.
     *
     * @return the new edge
     */
    public static @Nonnull Edge addReactionParticipant(@Nonnull Node reactionNode, @Nonnull Node species, String role, String stoichiometry)
            throws Exception
    {
        if( ! ( reactionNode.getKernel() instanceof Reaction ) )
            throw new IllegalArgumentException( "Element '" + reactionNode.getName() + "' is not a reaction" );
        if( ! ( species.getRole() instanceof VariableRole ) )
            throw new IllegalArgumentException( "Element '" + species.getName() + "' is not a species" );
        if( !SPECIE_REFERENCE_ROLES.contains( role ) )
            throw new IllegalArgumentException( "Unknown role of species in reaction: '" + role + "', expected one of " + SPECIE_REFERENCE_ROLES );
        Diagram diagram = Diagram.getDiagram( reactionNode );
        Reaction reaction = (Reaction)reactionNode.getKernel();
        String oldTitle = DiagramUtility.generateReactionTitle( Arrays.asList( reaction.getSpecieReferences() ) );

        String id = SpecieReference.generateSpecieReferenceName( reaction.getName(), species.getKernel().getName(), role );
        id = DefaultSemanticController.generateUniqueName( diagram, id );
        SpecieReference reference = new SpecieReference( reaction, id, role );
        reference.setSpecie( reaction.getOrigin() != null ? CollectionFactory.getRelativeName( species.getKernel(), Module.getModule( diagram ) )
                : species.getCompleteNameInDiagram() );
        if( stoichiometry != null && !stoichiometry.isEmpty() )
            reference.setStoichiometry( stoichiometry );

        ReactionInitialProperties properties = getReactionProperties( reactionNode.getCompartment(), new Point() );
        if( !properties.acceptForReaction( species ) )
            throw new IllegalArgumentException( "Element '" + species.getName() + "' can not take part in a reaction" );
        reaction.put( reference );

        //a real participant replaces the source/sink glyph on its side of the reaction (SBGN)
        if( reference.isReactantOrProduct() )
        {
            for( Edge e : reactionNode.getEdges() )
            {
                Node other = e.getOtherEnd( reactionNode );
                boolean sameSide = reference.isProduct() ? e.getInput() == reactionNode : e.getOutput() == reactionNode;
                if( sameSide && Util.isSourceSink( other ) && other.getEdges().length == 1 )
                {
                    e.getOrigin().remove( e.getName() );
                    other.getOrigin().remove( other.getName() );
                }
            }
        }

        Edge edge = properties.createEdge( reference, reactionNode, species );
        if( reference.isReactantOrProduct() )
            DiagramUtility.generateReactionRole( diagram, edge );
        edge.getOrigin().put( edge );

        String title = reaction.getTitle();
        if( title == null || title.isEmpty() || title.equals( oldTitle ) )
            reaction.setTitle( DiagramUtility.generateReactionTitle( Arrays.asList( reaction.getSpecieReferences() ) ) );
        return edge;
    }

    // ---------------------------------------------------------------- equations

    /**
     * Adds an equation (assignment rule, rate rule, algebraic rule or initial assignment).
     * If the target is not a model variable yet it is declared as a parameter with initial value 0.
     *
     * @param parent compartment to put the equation node into (normally the diagram)
     * @param type 'scalar' (default), 'rate', 'algebraic' or 'initial assignment'
     * @param variable target variable: parameter name, species name/path or variable ("$default.s1");
     *  ignored for algebraic equations
     * @param formula right-hand side
     * @param location node location, may be null
     * @return the equation node
     */
    public static @Nonnull Node addEquation(@Nonnull Compartment parent, String type, String variable, String formula, Point location)
    {
        Diagram diagram = Diagram.getDiagram( parent );
        EModel emodel = getEModel( diagram );
        if( type == null || type.isEmpty() )
            type = Equation.TYPE_SCALAR;
        if( !EQUATION_TYPES.contains( type ) )
            throw new IllegalArgumentException( "Unknown equation type '" + type + "', expected one of " + EQUATION_TYPES );
        if( formula == null || formula.isEmpty() )
            throw new IllegalArgumentException( "Equation formula is empty" );

        String variableName = "unknown";
        if( !Equation.TYPE_ALGEBRAIC.equals( type ) )
        {
            if( variable == null || variable.isEmpty() )
                throw new IllegalArgumentException( "Equation variable is empty" );
            variableName = resolveVariableName( diagram, variable );
            if( variableName == null )
            {
                if( variable.startsWith( "$" ) || !variable.matches( "[_a-zA-Z]\\w*" ) )
                    throw new IllegalArgumentException( "No variable '" + variable + "' in diagram '" + diagram.getName() + "'" );
                emodel.declareVariable( variable, 0.0 );
                variableName = variable;
            }
        }

        SemanticController controller = diagram.getType().getSemanticController();
        Node node = null;
        try
        {
            DiagramElement de = controller.createInstance( parent, Equation.class, location == null ? new Point() : location, (Object)null )
                    .getElement();
            if( de instanceof Node && de.getRole() instanceof Equation )
                node = (Node)de;
        }
        catch( RuntimeException e )
        {
            // the diagram type does not create equations headlessly: create the node below
        }
        if( node == null )
        {
            node = new Node( parent, new Stub( null, DefaultSemanticController.generateUniqueNodeName( parent, "equation" ), Type.MATH_EQUATION ) );
            node.setShowTitle( false );
        }
        node.setRole( new Equation( node, type, variableName, formula ) );
        if( location != null )
            node.setLocation( location );
        put( parent, node );
        return node;
    }

    /**
     * Adds an event: when <b>trigger</b> becomes true, after <b>delay</b>, the assignments are performed.
     * Assignment variables may be given by model name or as species name/path; they must exist.
     *
     * @param parent compartment to put the event node into (normally the diagram)
     * @return the event node
     */
    public static @Nonnull Node addEvent(@Nonnull Compartment parent, String trigger, String delay, Assignment[] assignments, Point location)
    {
        Diagram diagram = Diagram.getDiagram( parent );
        getEModel( diagram );
        if( trigger == null || trigger.isEmpty() )
            throw new IllegalArgumentException( "Event trigger is empty" );
        List<Assignment> actions = new ArrayList<>();
        if( assignments != null )
        {
            for( int i = 0; i < assignments.length; i++ )
            {
                Assignment a = assignments[i];
                if( a == null )
                    throw new IllegalArgumentException( "Event assignment #" + ( i + 1 ) + " is null" );
                String variable = resolveVariableName( diagram, a.getVariable() );
                if( variable == null )
                    throw new IllegalArgumentException( "No variable '" + a.getVariable() + "' in diagram '" + diagram.getName() + "'" );
                actions.add( new Assignment( variable, a.getMath() ) );
            }
        }

        SemanticController controller = diagram.getType().getSemanticController();
        Node node = null;
        try
        {
            DiagramElement de = controller.createInstance( parent, Event.class, location == null ? new Point() : location, (Object)null )
                    .getElement();
            if( de instanceof Node && de.getRole() instanceof Event )
                node = (Node)de;
        }
        catch( RuntimeException e )
        {
            // the diagram type does not create events headlessly: create the node below
        }
        if( node == null )
            node = new Node( parent, new Stub( null, DefaultSemanticController.generateUniqueNodeName( parent, "event" ), Type.MATH_EVENT ) );
        node.setRole( new Event( node, trigger, delay == null || delay.isEmpty() ? "0" : delay, actions.toArray( new Assignment[0] ) ) );
        if( location != null )
            node.setLocation( location );
        put( parent, node );
        return node;
    }

    // ---------------------------------------------------------------- parameters

    /**
     * Declares the parameters used in a formula that are not model variables yet (with value 0), the
     * same way the model generator does it when the model is simulated.
     *
     * @param formula a formula of the given equation, reaction or event
     * @param role the role the formula belongs to (used to resolve local names), may be null
     * @return names of the declared parameters
     */
    public static List<String> declareFormulaParameters(@Nonnull Diagram diagram, String formula, Role role)
    {
        List<String> declared = new ArrayList<>();
        if( formula == null || formula.isEmpty() || ! ( diagram.getRole() instanceof EModel ) )
            return declared;
        EModel emodel = diagram.getRole( EModel.class );
        Set<String> before = emodel.getVariables().names().collect( Collectors.toSet() );
        emodel.readMath( formula, role, EModel.VARIABLE_NAME_BY_ID );
        emodel.getVariables().names().filter( n -> !before.contains( n ) ).forEach( declared::add );
        return declared;
    }
}
