package biouml.plugins.sbgn;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.util.Clazz;
import ru.biosoft.util.DPSUtils;

public class SbgnUtil
{
    public static void moveToEdge(Node node, Rectangle domain, Point location)
    {
        Dimension nodeSize = ( node.getView() == null ) ? node.getShapeSize() : node.getView().getBounds().getSize();

        double leftDistance = location.x - domain.x; //distance to the left compartment boundary
        double rightDistance = ( domain.x + domain.width ) - ( location.x + nodeSize.width );//distance to the right compartment boundary
        double topDistance = location.y - domain.y;//distance to the top compartment boundary
        double bottomDistance = ( domain.y + domain.height ) - ( location.y + nodeSize.height );//distance to the boundary compartment boundary

        double distance = Double.MAX_VALUE;

        int side = 0;
        if( leftDistance < distance )
        {
            side = 1;
            distance = leftDistance;
        }
        if( rightDistance < distance )
        {
            side = 2;
            distance = rightDistance;
        }
        if( topDistance < distance )
        {
            side = 3;
            distance = topDistance;
        }
        if( bottomDistance < distance )
        {
            side = 4;
        }

        switch( side )
        {
            case 1: //left
                location.x = domain.x - nodeSize.width / 2;
                location.y = Math.min(Math.max(domain.y, location.y), domain.y + domain.height - nodeSize.height / 2);
                break;
            case 2://right
                location.x = domain.x + domain.width - nodeSize.width / 2;
                location.y = Math.min(Math.max(domain.y, location.y), domain.y + domain.height - nodeSize.height / 2);
                break;
            case 3://top
                location.y = domain.y - nodeSize.height / 2;
                location.x = Math.min(Math.max(domain.x, location.x), domain.x + domain.width - nodeSize.width / 2);
                break;
            case 4://bootom
                location.y = domain.y + domain.height - nodeSize.height / 2;
                location.x = Math.min(Math.max(domain.x, location.x), domain.x + domain.width - nodeSize.width / 2);
                break;
            default:
                break;
        }
    }

    public static void setSBGNTypes(Specie specie)
    {
        specie.setAvailableTypes(getSBGNTypes());
    }

    public static String[] getSBGNTypes()
    {
        return SBGNPropertyConstants.entityTypes.toArray(new String[SBGNPropertyConstants.entityTypes.size()]);
    }

    public static void setView(CompositeView container, DiagramElement de)
    {
        container.setModel(de);
        container.setActive(true);
        if( de instanceof Node )
            container.setLocation( ( (Node)de ).getLocation());
        de.setView(container);
    }

    public static void setCompartmentView(CompositeView container, View coreView, Compartment compartment)
    {
        coreView.setModel(compartment);
        coreView.setActive(true);
        container.setModel(compartment);
        container.setLocation(compartment.getLocation());
        compartment.setView(container);
    }

    /**
     * Given edge tries to find correspondent reaction.There can be 2 situations:<br>
     * 1. Edge connects to reaction node directly<br>
     * 2. Edge leads from species to logical element which in turn is connected with reaction
     */
    public static Node findReaction(Edge e)
    {
        if( Util.isReaction(e.getInput()) )
            return e.getInput();
        else if( Util.isReaction(e.getOutput()) )
            return e.getOutput();
        else if( isLogical(e.getInput()) )
            return getLogicReaction(e.getInput());
        else if( isLogical(e.getOutput()) )
            return getLogicReaction(e.getOutput());
        return null;
    }

    public static Node getLogicReaction(Node node)
    {
        return node.edges().select(Edge.class).map(e -> e.getOtherEnd(node)).filter(n -> Util.isReaction(n)).findAny().orElse(null);
    }

    public static List<SpecieReference> getLogicReferences(Node node)
    {
        return node.edges().select(Edge.class).map(Edge::getKernel).select(SpecieReference.class).toList();
    }

    public static boolean isLogical(DiagramElement node)
    {
        return Type.TYPE_LOGICAL.equals(node.getKernel().getType());
    }

    public static boolean isEquivalence(DiagramElement node)
    {
        return Type.TYPE_EQUIVALENCE.equals(node.getKernel().getType());
    }

    public static boolean isSubType(Node node)
    {
        return node.getAttributes().hasProperty(SBGNPropertyConstants.SUPER_TYPE);
    }

    //    public static String getSuperType(Node node)
    //    {
    //        node.getAttributes().hasProperty( SBGNPropertyConstants.SUPER_TYPE );
    //        return node.getAttributes().getValueAsString(  );
    //    }

    public static void setSuperType(Node node, Node superType)
    {
        node.getAttributes()
                .add(DPSUtils.createReadOnly(SBGNPropertyConstants.SUPER_TYPE, String.class, superType.getCompleteNameInDiagram()));
    }

    public static boolean isClone(Node node)
    {
        return node.getRole() instanceof VariableRole && node.getRole(VariableRole.class).getAssociatedElements().length > 1;
    }

    /**
     * generates appropriate SBO term for given element
     */
    public static String generateSBOTerm(DiagramElement de)
    {
        return Util.isInputPort(de) ? "SBO:0000600" : Util.isOutputPort(de) ? "SBO:0000601" : Util.isContactPort(de) ? "SBO:0000599" : "";
    }

    public static List<DiagramElement> generateSourceSink(Node reactionNode, boolean doPut)
    {
        List<DiagramElement> result = new ArrayList<>();
        if( ! ( reactionNode.getKernel() instanceof Reaction ) )
            return result;
        if( ! ( (SbgnDiagramViewOptions)Diagram.getDiagram(reactionNode).getViewOptions() ).isAddSourceSink() )
            return result;

        boolean hasProducts = false;
        boolean hasReactants = false;
        for( String role : reactionNode.edges().map(Edge::getKernel)
                .map(Clazz.of(SpecieReference.class).toObj(SpecieReference::getRole, Base::getType)) )
        {
            if( role.equals(Type.TYPE_PRODUCTION) || role.equals(SpecieReference.PRODUCT) )
                hasProducts = true;
            else if( role.equals(Type.TYPE_CONSUMPTION) || role.equals(SpecieReference.REACTANT) )
                hasReactants = true;

            if( hasProducts && hasReactants )
                break;
        }
        if( !hasProducts )
            result.addAll(getProductElements(reactionNode.getKernel().getName(), reactionNode, reactionNode.getCompartment()));
        if( !hasReactants )
            result.addAll(getReactantElements(reactionNode.getKernel().getName(), reactionNode, reactionNode.getCompartment()));

        for( DiagramElement element : result )
            SbgnSemanticController.setNeccessaryAttributes(element);

        if( doPut )
        {
            for( DiagramElement element : result )
                SbgnSemanticController.setNeccessaryAttributes(element);

            StreamEx.of(result).select(Node.class).forEach(n -> n.getOrigin().put(n));
            StreamEx.of(result).select(Edge.class).forEach(e -> e.getOrigin().put(e));
        }
        return result;
    }

    public static List<DiagramElement> getProductElements(String productName, Node reactionNode, Compartment compartment)
    {
        String sourceName = productName + "ProductSource";
        Node output = new Node(compartment, sourceName, new Stub(null, sourceName, "source-sink"));
        output.setShapeSize(new Dimension(30, 30));
        output.setLocation(reactionNode.getLocation().x + reactionNode.getShapeSize().width + 60, reactionNode.getLocation().y - 12);
        Edge sourceEdge = new Edge(compartment, new Stub(null, sourceName + "Edge", "production"), reactionNode, output);
        return StreamEx.of(output, sourceEdge).toList();
    }

    public static List<DiagramElement> getReactantElements(String reactantName, Node reactionNode, Compartment compartment)
    {
        String sourceName = reactantName + "ReactantSource";
        Node input = new Node(compartment, sourceName, new Stub(null, sourceName, "source-sink"));
        input.setShapeSize(new Dimension(30, 30));
        input.setLocation(reactionNode.getLocation().x - 60, reactionNode.getLocation().y - 12);
        Edge sourceEdge = new Edge(compartment, new Stub(null, sourceName + "Edge", "consumption"), input, reactionNode);
        return StreamEx.of(input, sourceEdge).toList();
    }

    public static Node getRedundantSourceSink(Node reactionNode, String edgeType)
    {
        if( ! ( reactionNode.getKernel() instanceof Reaction ) )
            return null;
        if( ! ( (SbgnDiagramViewOptions)Diagram.getDiagram(reactionNode).getViewOptions() ).isAddSourceSink() )
            return null;
        String stubType = edgeType.equals(SpecieReference.PRODUCT) ? Type.TYPE_PRODUCTION : Type.TYPE_CONSUMPTION;
        return reactionNode.edges().filter(edge -> edge.getOtherEnd(reactionNode).getKernel().getType().equals(Type.TYPE_SOURCE_SINK))
                .filter(edge -> edge.getKernel().getType().equals(stubType)).map(edge -> edge.getOtherEnd(reactionNode)).findAny()
                .orElse(null);
    }

    public static boolean isUnitOfInformation(biouml.model.Node node)
    {
        return ( node.getKernel() != null && ( Type.TYPE_UNIT_OF_INFORMATION.equals(node.getKernel().getType()) ) );
    }

    public static boolean isComplex(biouml.model.Node node)
    {
        return node.getKernel() != null && Type.TYPE_COMPLEX.equals(node.getKernel().getType());
    }

    public static boolean isMacromoleculeEntity(biouml.model.Node node)
    {
        return node.getKernel() != null && Type.TYPE_MACROMOLECULE.equals(node.getKernel().getType());
    }

    public static boolean isVariableNode(biouml.model.Node node)
    {
        return node.getKernel() != null && Type.TYPE_VARIABLE.equals(node.getKernel().getType());
    }

    public static boolean isPhenotype(biouml.model.Node node)
    {
        return node.getKernel() != null && Type.TYPE_PHENOTYPE.equals(node.getKernel().getType());
    }

    public static boolean isNotComplexEntity(biouml.model.Node node)
    {
        return ( node.getKernel() != null && ( Type.TYPE_MACROMOLECULE.equals(node.getKernel().getType())
                || Type.TYPE_SIMPLE_CHEMICAL.equals(node.getKernel().getType())
                || Type.TYPE_NUCLEIC_ACID_FEATURE.equals(node.getKernel().getType())
                || Type.TYPE_UNSPECIFIED.equals(node.getKernel().getType())
                || Type.TYPE_PERTURBING_AGENT.equals(node.getKernel().getType()) ) );
    }

    public static boolean isRegulationEdge(Edge edge)
    {
        return edge.getKernel() != null && Type.TYPE_REGULATION.equals(edge.getKernel().getType());
    }

    public static boolean isEquivalenceNode(biouml.model.Node node)
    {
        return node.getKernel() != null && Type.TYPE_EQUIVALENCE.equals(node.getKernel().getType());
    }

    public static boolean containsAny(String str, String[] strings)
    {
        for( String s : strings )
            if( str.contains(s) )
                return true;
        return false;
    }

    public static boolean isNoteEdge(Edge edge)
    {
        return edge.getKernel() != null && biouml.standard.type.Type.TYPE_NOTE_LINK.equals(edge.getKernel().getType());
    }

    public static boolean isNote(Node node)
    {
        return node.getKernel() != null && Type.TYPE_NOTE.equals(node.getKernel().getType());
    }
}
