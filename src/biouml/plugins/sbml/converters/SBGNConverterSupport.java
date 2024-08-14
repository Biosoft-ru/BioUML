package biouml.plugins.sbml.converters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverterSupport;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.standard.type.Specie;
import biouml.standard.state.State;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * SBML/SBGN converter
 */
public abstract class SBGNConverterSupport extends DiagramTypeConverterSupport
{
    public static final String REACTION_TYPE_ATTR = "ReactionType";

    /**
     * @return diagram type of the result diagram
     * It can be diagram type or name of xml notation
     */
    protected Object getType()
    {
        return SbgnDiagramType.class;
    }

    @Override
    public final Diagram convert(Diagram diagram, Object type) throws Exception
    {
        return super.convert( diagram, getType() );
    }

    @Override
    public final Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        Diagram sbgnDiagram = createDiagram( diagramType, diagram );

        boolean isSbmlNotificationEnabled = diagram.isNotificationEnabled();
        boolean isSbmlPropogationEnabled = diagram.isPropagationEnabled();
        boolean isNotificationEnabled = sbgnDiagram.isNotificationEnabled();
        boolean isPropagationEnabled = sbgnDiagram.isPropagationEnabled();
        sbgnDiagram.setNotificationEnabled( false );
        sbgnDiagram.setPropagationEnabled( false );
        diagram.setNotificationEnabled( false );
        diagram.setPropagationEnabled( false );

        State currentState = diagram.getCurrentState();
        if( currentState != null )
            diagram.restore();

        createElements( diagram, sbgnDiagram );
        createEdges( diagram, sbgnDiagram, sbgnDiagram );

        copyDiagramProperties( diagram, sbgnDiagram );
        copyStates( diagram, sbgnDiagram, currentState );
        postProcess( diagram, sbgnDiagram );

        diagram.setNotificationEnabled( isSbmlNotificationEnabled );
        diagram.setPropagationEnabled( isSbmlPropogationEnabled );
        sbgnDiagram.setNotificationEnabled( isNotificationEnabled );
        sbgnDiagram.setPropagationEnabled( isPropagationEnabled );

        return sbgnDiagram;
    }


    /**
     * Transforms SBGN diagram back to SBML
     */
    public final Diagram restoreSBML(Diagram diagram, DiagramType baseType) throws Exception
    {
        Diagram newDiagram = createDiagram( baseType, diagram );

        boolean isSbmlNotificationEnabled = newDiagram.isNotificationEnabled();
        boolean isSbmlPropogationEnabled = newDiagram.isPropagationEnabled();
        boolean isNotificationEnabled = diagram.isNotificationEnabled();
        boolean isPropagationEnabled = diagram.isPropagationEnabled();
        diagram.setNotificationEnabled( false );
        diagram.setPropagationEnabled( false );
        newDiagram.setNotificationEnabled( false );
        newDiagram.setPropagationEnabled( false );

        State currentState = diagram.getCurrentState();
        if( currentState != null )
            diagram.restore();

        restoreElements( diagram, newDiagram );
        restoreEdges( diagram, newDiagram, newDiagram );

        copyDiagramProperties( diagram, newDiagram );       
        postProcessRestore( diagram, newDiagram );

        copyStates( diagram, newDiagram, currentState );
        
        newDiagram.setNotificationEnabled( isSbmlNotificationEnabled );
        newDiagram.setPropagationEnabled( isSbmlPropogationEnabled );
        diagram.setNotificationEnabled( isNotificationEnabled );
        diagram.setPropagationEnabled( isPropagationEnabled );

        if( diagram.getAttributes().getProperty( "Packages" ) != null )
            newDiagram.getAttributes().add( diagram.getAttributes().getProperty( "Packages" ) );
        if( diagram.getAttributes().getProperty( "fbc:activeObjective" ) != null )
            newDiagram.getAttributes().add( diagram.getAttributes().getProperty( "fbc:activeObjective" ) );
        if( diagram.getAttributes().getProperty( "listObjectives" ) != null )
            newDiagram.getAttributes().add( diagram.getAttributes().getProperty( "listObjectives" ) );
        return newDiagram;
    }

    protected abstract void createElements(Compartment baseCompartment, Compartment compartment) throws Exception;
    protected abstract void createEdges(Compartment baseCompartment, Compartment compartment, Diagram sbgnDiagram) throws Exception;
    protected abstract void restoreElements(Compartment sbgnCompartment, Compartment compartment) throws Exception;
    protected abstract void restoreEdges(Compartment sbgnCompartment, Compartment compartment, Diagram sbmlDiagram) throws Exception;

    protected Diagram createDiagram(DiagramType type, Diagram oldDiagram) throws Exception
    {
        return type.createDiagram( oldDiagram.getOrigin(), oldDiagram.getName(), null );
    }

    protected void postProcess(Diagram oldDiagram, Diagram diagram)
    {
        diagram.setView( null );//it is necessary to create view one more time when opening
    }

    protected void postProcessRestore(Diagram oldDiagram, Diagram diagram) throws Exception
    {
        diagram.setView( null );//it is necessary to create view one more time when opening
    }

    protected void copyDiagramProperties(Diagram from, Diagram to)
    {
        to.setTitle( from.getTitle() );

        copyViewOptions( from, to );
        copyDiagramInfo( from, to );

        Role diagramRole = from.getRole();
        if( diagramRole instanceof EModel )
            to.setRole( ( (EModel)diagramRole ).clone( to ) );

        copyAttributes( from, to );
    }

    protected void copyStates(Diagram from, Diagram to, State initialState) throws Exception
    {
        //state handling
        State newCurrentState = null;
        for( State state : from.states() )
        {
            State newState = state.clone( to, state.getName() );
            if( state.equals( initialState ) )
                newCurrentState = newState;
        }

        if( initialState != null )
        {
            to.setStateEditingMode( newCurrentState );
            from.setStateEditingMode( initialState );
        }
    }

    protected void copyViewOptions(Diagram from, Diagram to)
    {
        DiagramViewOptions viewOptions = to.getViewOptions();
        boolean notificationEnabled = viewOptions.isNotificationEnabled();
        viewOptions.setNotificationEnabled( false );
        viewOptions.setDependencyEdges( from.getViewOptions().isDependencyEdges() );
        viewOptions.setAutoLayout( from.getViewOptions().isAutoLayout() );
        viewOptions.setNotificationEnabled( notificationEnabled );
    }

    
    //TODO: use cloning?
    protected void copyDiagramInfo(Diagram from, Diagram to)
    {
        DiagramInfo fromInfo = (DiagramInfo)from.getKernel();
        DiagramInfo toInfo = (DiagramInfo)to.getKernel();
        toInfo.setDescription( fromInfo.getDescription() );
        toInfo.setDatabaseReferences( fromInfo.getDatabaseReferences() );
        toInfo.setLiteratureReferences( fromInfo.getLiteratureReferences() );
        toInfo.setAuthors( fromInfo.getAuthors().clone() );
        toInfo.setCreated( fromInfo.getCreated() );
        toInfo.setModified( fromInfo.getModified().clone() );
    }


    protected void updateEdgeConnections(final Diagram diagram, Compartment compartment)
    {
        compartment.visitEdges( edge -> diagram.getType().getSemanticController().recalculateEdgePath( edge ) );
    }

    /**
     * Try to guess diagram type basing on number of reactants and products
     */
    public static String guessReactionType(Node node)
    {
        //which diagrams can have this attribute?
        Object rtObject = node.getAttributes().getValue( REACTION_TYPE_ATTR );
        if( rtObject != null )
        {
            String s = rtObject.toString().toLowerCase();
            if( s.endsWith( "association" ) )
                return "association";
            else if( s.endsWith( "dissociation" ) )
                return "dissociation";
            else
                return "process";
        }
        return guessReactionType( (Reaction)node.getKernel() );
    }

    public static String guessReactionType(Reaction reaction)
    {
        int reactants = 0;
        int products = 0;
        for( SpecieReference sr : reaction.getSpecieReferences() )
        {
            if( sr.isReactant() )
                reactants++;
            else if( sr.isProduct() )
                products++;
        }

        if( reactants > 1 && products == 1 )
            return "association";
        else if( reactants == 1 && products > 1 )
            return "dissociation";
        else
            return "process";
    }

    protected static Compartment createCompartmentClone(Compartment compartment, Compartment base, String name) throws Exception
    {
        Compartment newCompartment = new Compartment( compartment, name, base.getKernel() );
        newCompartment.setLocation( base.getLocation() );
        newCompartment.setShapeSize( base.getShapeSize() );
        newCompartment.setShapeType( base.getShapeType() );
        newCompartment.setRole( base.getRole().clone( newCompartment ) );
        newCompartment.setTitle( base.getTitle() );
        newCompartment.setComment( base.getComment() );
        newCompartment.setVisible( base.isVisible() );
        newCompartment.setUseCustomImage(base.isUseCustomImage());
        if( newCompartment.isUseCustomImage() )
            newCompartment.setImage(base.getImage().clone());
        newCompartment.setPredefinedStyle(base.getPredefinedStyle());
        if( newCompartment.getPredefinedStyle().equals(DiagramElementStyle.STYLE_NOT_SELECTED) )
            newCompartment.setCustomStyle(base.getCustomStyle().clone());
        compartment.put( newCompartment );
        return newCompartment;
    }

    protected static Node createNodeClone(Compartment compartment, Node base, String name) throws Exception
    {
        Node newNode = new Node( compartment, name, base.getKernel() );
        newNode.setTitle( base.getTitle() );
        newNode.setLocation( base.getLocation() );
        newNode.setRole( base.getRole( Role.class ).clone( newNode ) );
        newNode.setComment( base.getComment() );
        newNode.setVisible( base.isVisible() );
        newNode.setUseCustomImage(base.isUseCustomImage());
        if( newNode.isUseCustomImage() )
            newNode.setImage(base.getImage().clone());
        newNode.setPredefinedStyle(base.getPredefinedStyle());
        if( newNode.getPredefinedStyle().equals(DiagramElementStyle.STYLE_NOT_SELECTED) )
            newNode.setCustomStyle(base.getCustomStyle().clone());
        compartment.put( newNode );
        return newNode;
    }

    protected static void copyAttributes(DiagramElement oldNode, DiagramElement newNode)
    {
        for( DynamicProperty dp : oldNode.getAttributes() )
            newNode.getAttributes().add( dp );
    }

    protected static void copyAttribute(DiagramElement oldNode, DiagramElement newNode, String attrName)
    {
        DynamicProperty dp = oldNode.getAttributes().getProperty( attrName );
        if( dp != null )
            newNode.getAttributes().add( dp );
    }


    /**
     * after conversion from sbgn model to sbml model  these properties  aren't copied
     */
    private final static Set<String> sbgnProperty = new HashSet<>(Arrays.asList("xmlElementType", SBGNPropertyConstants.SBGN_CLONE_MARKER,
            SBGNPropertyConstants.SBGN_ENTITY_TYPE, SBGNPropertyConstants.SBGN_MULTIMER, SBGNPropertyConstants.SBGN_REACTION_TYPE,
            SBGNPropertyConstants.STYLE_ATTR, SBGNPropertyConstants.ORIENTATION, "BodyColor"));

    protected static final boolean isSbmlProperty(DynamicProperty dp)
    {
        return !sbgnProperty.contains( dp.getName() );
    }

    protected void convertSbgnTypeToKernelType(Node de, Base kernel)
    {
        if( kernel instanceof Specie && de.getKernel().getType().equals( "macromolecule" ) )
            ( (Specie)kernel ).setType( Type.TYPE_PROTEIN );
    }

    protected static Edge findEdge(Node node, Base kernel)
    {
        return kernel == null ? null : node.edges().filter( e -> kernel.equals( e.getKernel() ) ).findAny().orElse( null );
    }

    protected static void restoreEdge(SpecieReference sr, Node input, Node output, Edge edge) throws Exception
    {
        Edge newEdge = new Edge( sr, input, output );
        newEdge.setRole( edge.getRole() );
        newEdge.setPath( edge.getPath() );
        newEdge.setComment(edge.getComment());
        for( DynamicProperty dp : edge.getAttributes() )
        {
            if( isSbmlProperty( dp ) )
                copyAttribute( edge, newEdge, dp.getName() );
        }
        newEdge.save();
    }
}
