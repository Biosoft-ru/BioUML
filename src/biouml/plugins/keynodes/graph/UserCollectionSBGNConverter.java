package biouml.plugins.keynodes.graph;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverterSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnDiagramViewOptions;
import biouml.plugins.sbgn.SbgnSemanticController;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbgn.Type;
import biouml.plugins.sbgn.extension.SbgnExDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.workbench.diagram.DiagramTypeConverterRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.bean.StaticDescriptor;

public class UserCollectionSBGNConverter extends DiagramTypeConverterSupport
{
    private static final String PATH_IN_USER_COLLECTION = "PathInUserCollection";
    private static final PropertyDescriptor PATH_IN_USER_COLLECTION_PROP = StaticDescriptor.create( PATH_IN_USER_COLLECTION );

    @Override
    public Diagram convert(Diagram diagram, Object type) throws Exception
    {
        return super.convert( diagram, SbgnExDiagramType.class );
    }

    @Override
    protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        Diagram sbgnDiagram = diagramType.createDiagram( diagram.getOrigin(), diagram.getName(), null );
        sbgnDiagram.setTitle( diagram.getTitle() );
        ( (DiagramInfo)sbgnDiagram.getKernel() ).setDescription( ( (DiagramInfo)diagram.getKernel() ).getDescription() );
        ( (DiagramInfo)sbgnDiagram.getKernel() ).setDatabaseReferences( ( (DiagramInfo)diagram.getKernel() ).getDatabaseReferences() );

        SbgnDiagramViewOptions options = (SbgnDiagramViewOptions)sbgnDiagram.getViewOptions();
        options.setStyles( diagram.getViewOptions().getStyles() );

        boolean notificationEnabled = sbgnDiagram.isNotificationEnabled();
        sbgnDiagram.setNotificationEnabled( false );

        createElements( diagram, sbgnDiagram );
        createEdges( diagram, sbgnDiagram, sbgnDiagram );

        updateDiagramModel( sbgnDiagram, sbgnDiagram );
        sbgnDiagram.setNotificationEnabled( notificationEnabled );

        DiagramTypeConverterRegistry.assignConverterToDiagram( sbgnDiagram, this );
        return sbgnDiagram;
    }

    protected void createElements(Compartment baseCompartment, Compartment compartment) throws Exception
    {
        List<String> elementNames = baseCompartment.getNameList();
        for( String name : elementNames )
        {
            DiagramElement de = baseCompartment.get( name );
            if( de instanceof Compartment )
            {
                addCompartment( compartment, name, de );
            }
            else if( de instanceof Node )
            {
                addNode( compartment, name, de, de.getKernel(), ( (Node)de ).getLocation(), ( (Node)de ).isVisible() );
            }
        }
    }

    protected void addCompartment(Compartment parent, String name, DiagramElement de) throws Exception
    {
        Base kernel = de.getKernel();
        Compartment newCompartment = new Compartment( parent, name, kernel );
        newCompartment.setLocation( ( (Compartment)de ).getLocation() );
        newCompartment.setShapeSize( ( (Compartment)de ).getShapeSize() );
        newCompartment.setTitle( de.getTitle() );
        newCompartment.setPredefinedStyle( de.getPredefinedStyle() );
        newCompartment = (Compartment)validate( newCompartment );
        addKernelPropertyMarker( kernel, kernel.getCompletePath().toString() );
        copyAttributes( de.getAttributes(), newCompartment );
        parent.put( newCompartment );
        createElements( (Compartment)de, newCompartment );
    }

    protected static void addKernelPropertyMarker(Base kernel, String value)
    {
        DynamicPropertySet attrs = kernel.getAttributes();
        DynamicProperty property = attrs.getProperty( PATH_IN_USER_COLLECTION );
        if( property == null )
            attrs.add( new DynamicProperty( PATH_IN_USER_COLLECTION_PROP, String.class, value ) );
    }

    protected void createEdges(Compartment baseCompartment, Compartment compartment, Diagram sbgnDiagram) throws Exception
    {
        List<String> elementNames = baseCompartment.getNameList();
        for( String name : elementNames )
        {
            DiagramElement de = baseCompartment.get( name );
            if( de instanceof Compartment )
            {
                createEdges( (Compartment)de, (Compartment)compartment.get( name ), sbgnDiagram );
            }
            else if( de instanceof Edge )
            {
                Edge edge = (Edge)de;
                Node node1 = sbgnDiagram.findNode( edge.getInput().getCompleteNameInDiagram() );
                Node node2 = sbgnDiagram.findNode( edge.getOutput().getCompleteNameInDiagram() );
                if( node1 != null && node2 != null )
                {
                    if( edge.getKernel() instanceof SpecieReference )
                    {
                        SpecieReference sr = (SpecieReference)edge.getKernel();
                        Edge newEdge = createEdge( node1, node2, sr );
                        copyAttributes( edge.getAttributes(), newEdge );
                        newEdge.save();
                    }
                    else if( edge.getKernel() instanceof SemanticRelation )
                    {
                        Edge newEdge = (Edge)validate( new Edge( sbgnDiagram, edge.getKernel(), node1, node2 ) );
                        copyAttributes( edge.getAttributes(), newEdge );
                        newEdge.setTitle( edge.getTitle() );
                        newEdge.save();
                    }
                }
            }
        }
    }

    private static Edge createEdge(@Nonnull Node node1, @Nonnull Node node2, SpecieReference sr) throws Exception
    {
        Reaction reaction = null;
        Node otherNode = null;
        if( node1.getKernel() instanceof Reaction )
        {
            reaction = (Reaction)node1.getKernel();
            otherNode = node2;
        }
        else if( node2.getKernel() instanceof Reaction )
        {
            reaction = (Reaction)node2.getKernel();
            otherNode = node1;
        }
        else
            throw new InternalException( "Non-reaction nodes were passed to addEdge: " + DataElementPath.create( node1 ) + " and "
                    + DataElementPath.create( node2 ) );

        String edgeName = reaction.getName() + ": " + otherNode.getKernel().getName() + " as " + sr.getRole();
        SpecieReference newSR = sr.clone( reaction, edgeName );
        newSR.setSpecie( otherNode.getCompleteNameInDiagram() );
        if( reaction.contains( sr.getName() ) )
            reaction.remove( sr.getName() );
        reaction.put( newSR );
        Edge newEdge = (Edge)validate( new Edge( edgeName, newSR, node1, node2 ) );
        if( sr.getRole().equals( SpecieReference.MODIFIER ) )
            newEdge.getAttributes()
                    .add( new DynamicProperty( SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, Type.TYPE_CATALYSIS ) );
        DiagramUtility.generateReactionRole( Diagram.getDiagram( newEdge ), newEdge );
        return newEdge;
    }

    private static @Nonnull List<DiagramElement> convertNode(Compartment compartment, String name, DiagramElement de, Base kernel,
            Point location, boolean visible) throws Exception
    {
        try
        {
            List<DiagramElement> nodes = new ArrayList<>();
            if( kernel instanceof Reaction )
            {
                Reaction reaction = (Reaction)kernel;

                Reaction newKernel = new Reaction( null, kernel.getName() );
                newKernel.setTitle( reaction.getTitle() );

                for( DynamicProperty dp : reaction.getAttributes() )
                    newKernel.getAttributes().add( dp );

                Node reactionNode = new Node( compartment, newKernel );
                String formula = ( (Reaction)kernel ).getKineticLaw().getFormula();
                newKernel.getKineticLaw().setFormula( formula );
                DiagramUtility.generateReactionRole( Diagram.getDiagram( reactionNode ), reactionNode );
                reactionNode.setVisible( visible );
                reactionNode.setTitle( reaction.getTitle() );
                reactionNode = (Node)validate( reactionNode );
                addKernelPropertyMarker( newKernel, kernel.getCompletePath().toString() );
                copyAttributes( de.getAttributes(), reactionNode );
                nodes.add( reactionNode );
            }
            else
            {
                Compartment newNode;
                if( ! ( kernel instanceof Specie ) )
                {
                    Specie newKernel = new Specie( null, kernel.getName() );
                    if( kernel instanceof Protein )
                        newKernel.setType( Type.TYPE_MACROMOLECULE );
                    else
                        newKernel.setType( Type.TYPE_UNSPECIFIED );
                    SbgnUtil.setSBGNTypes( newKernel );
                    addKernelPropertyMarker( newKernel, kernel.getCompletePath().toString() );
                    newNode = new Compartment( compartment, name, newKernel );
                }
                else
                {
                    addKernelPropertyMarker( kernel, kernel.getCompletePath().toString() );
                    newNode = new Compartment( compartment, name, kernel );
                }

                newNode.setVisible( visible );
                newNode.setLocation( location );
                newNode.setPredefinedStyle( de.getPredefinedStyle() );
                newNode.setRole( new VariableRole( newNode, 0 ) );
                newNode = (Compartment)validate( newNode );
                copyAttributes( de.getAttributes(), newNode );
                nodes.add( newNode );
            }
            return nodes;
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Cannot convert node " + name, e );
        }

    }

    @Override
    public DiagramElement[] convertDiagramElement(DiagramElement de, Diagram diagram) throws Exception
    {
        //check diagram type
        if( ! ( diagram.getType() instanceof SbgnDiagramType ) )
            return null;

        Base kernel = de.getKernel();
        if( kernel == null )
            return null;

        if( !DiagramTypeConverterRegistry.checkConverter( diagram, this ) && !canConvert( de ) )
            return null;

        DataCollection<?> parent = de.getOrigin();
        if( parent instanceof Compartment )
        {
            if( de instanceof Compartment )
            {
                addCompartment( (Compartment)parent, de.getName(), de );
            }
            else if( de instanceof Node )
            {
                List<DiagramElement> converted = convertNode( (Compartment)parent, de.getName(), de, de.getKernel(),
                        ( (Node)de ).getLocation(), true );
                DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
                DiagramViewOptions viewOptions = diagram.getViewOptions();
                Graphics graphics = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_RGB ).getGraphics();
                for( DiagramElement elem : converted )
                {
                    if( elem instanceof Compartment )
                        viewBuilder.createCompartmentView( (Compartment)elem, viewOptions, graphics );
                    else if( elem instanceof Node )
                        viewBuilder.createNodeView( (Node)elem, viewOptions, graphics );

                    return converted.toArray( new DiagramElement[converted.size()] );
                }
            }
            else if( de instanceof Edge )
            {
                return new DiagramElement[] {createEdge( ( (Edge)de ).getInput(), ( (Edge)de ).getOutput(), (SpecieReference)kernel )};
            }
        }
        return null;
    }

    @Override
    public boolean canConvert(DiagramElement de)
    {
        return de.getKernel().getAttributes().hasProperty( PATH_IN_USER_COLLECTION );
    }

    @Override
    public void updateDiagramModel(Compartment compartment, Diagram diagram)
    {
        EModel model = diagram.getRole( EModel.class );
        for( DiagramElement de : compartment.recursiveStream() )
        {
            Role deRole = de.getRole();
            if( deRole instanceof Variable )
            {
                model.put( (Variable)deRole );
            }
            else if( deRole instanceof Equation )
            {
                String varName = ( (Equation)deRole ).getVariable();
                if( ( !varName.startsWith( "$" ) || varName.startsWith( "$$" ) ) && !model.containsVariable( varName ) )
                    model.put( new Variable( varName, model, model.getVariables() ) );
            }
        }
    }

    private static void copyAttributes(DynamicPropertySet attributes, DiagramElement de)
    {
        for( DynamicProperty dp : attributes )
            de.getAttributes().add( new DynamicProperty( dp.getDescriptor(), dp.getType(), dp.getValue() ) );
    }

    private static Node addNode(Compartment compartment, String name, DiagramElement de, Base kernel, Point location, boolean visible)
            throws Exception
    {
        List<DiagramElement> nodes = convertNode( compartment, name, de, kernel, location, visible );
        for( DiagramElement el : nodes )
            compartment.put( el );

        if( nodes.size() == 0 || ! ( nodes.get( 0 ) instanceof Node ) )
            return null;

        return (Node)nodes.get( 0 );
    }

    private static DiagramElement validate(DiagramElement de) throws Exception
    {
        SbgnSemanticController.setNeccessaryAttributes( de );
        return de;
    }
}