package biouml.standard.diagram;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.InternalException;
import ru.biosoft.graphics.editor.ViewEditorPane;

/**
 * Class generates reaction and all necessary additional elements on the diagram
 * Override it if your diagram type needs to add more attributes or diagram elements to reaction
 * @TODO: will replace DiagramUtility.createReactionNode
 * @author Ilya
 */
public class ReactionInitialProperties implements InitialElementProperties
{
    private List<SpecieReference> references;
    private DataCollection origin;
    private KineticLaw kineticLaw;
    private Reaction reaction;
    private String reactionName = null;
    private String reactionTitle = null;

    public ReactionInitialProperties()
    {
        references = new ArrayList<>();
        kineticLaw = new KineticLaw();
    }

    public void setSpecieReferences(List<SpecieReference> references)
    {
        this.references = references;
    }

    public void setKineticlaw(KineticLaw kineticLaw)
    {
        this.kineticLaw = kineticLaw;
    }

    public void setReactionName(String name)
    {
        this.reactionName = name;
    }

    public String getReactionName()
    {
        return reactionName;
    }

    public String getReactionTitle()
    {
        return reactionTitle;
    }

    public void setReactionTitle(String reactionTitle)
    {
        this.reactionTitle = reactionTitle;
    }

    /**
     * Creates reaction node, components nodes (SpecieReferences) and edges. Put them to diagram.
     */
    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Diagram diagram = Diagram.getDiagram(compartment);
        boolean notificationEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled(false);
        origin = initReactionOrigin(diagram); //TODO: maybe two options: to try to check module and do not

        if( reactionName != null && origin == null && diagram.recursiveStream().map(de -> de.getName()).toSet().contains(reactionName) )
            throw new IllegalArgumentException("Reaction with name " + reactionName + " already exists.");

        if( reactionName == null )
            reactionName = DefaultSemanticController.generateReactionName(origin != null ? origin : diagram);

        reaction = new Reaction(origin, reactionName);

        reaction.setTitle(reactionTitle != null ? reactionTitle : DiagramUtility.generateReactionTitle(references));
        reaction.setKineticLaw(kineticLaw);
        Node reactionNode = new Node(compartment, reaction);
        reaction.setParent(reactionNode);
        reactionNode.setShowTitle(false);
        reactionNode.setNotificationEnabled(false);
        reactionNode.setRelativeLocation(diagram, location);
        DiagramElementGroup result = new DiagramElementGroup(reactionNode);
        // add species roles and edges
        for( SpecieReference prototype : references )
            result.add(createSpecieReference(diagram, prototype, reactionNode));
        DiagramUtility.generateRoles(diagram, reactionNode);

        reactionNode.setNotificationEnabled(true);
        diagram.setNotificationEnabled(notificationEnabled);

        putResults( result );
        return result;
    }

    private Edge createSpecieReference(Diagram diagram, SpecieReference prototype, Node reactionNode) throws Exception
    {
        //It's necessary to find node by prototype name (not by prototype specie) for the case when we have two nodes with identifiers like this:
        //CMP0225.CMP0034.PRT003455 and CMP0225.PRT003455
        Node de = diagram.findNode(DiagramUtility.toDiagramPath(prototype.getName()));
        if( de == null )
            de = diagram.findNode(DiagramUtility.toDiagramPath(prototype.getSpecie()));

        if( !acceptForReaction(de) )
            throw new Exception("incorrect reaction participant: " + de.getName());

        String id = SpecieReference.generateSpecieReferenceName(reaction.getName(), de.getKernel().getName(), prototype.getRole());
        id = DefaultSemanticController.generateUniqueName(diagram, id);
        SpecieReference real = prototype.clone(reaction, id);
        String specieLink = ( origin != null ) ? CollectionFactory.getRelativeName(de.getKernel(), Module.getModule(diagram))
                : de.getCompleteNameInDiagram();
        real.setSpecie(specieLink);
        reaction.put(real);

        return createEdge(real, reactionNode, de);
    }

    //TODO: use semantic controller method createEdge, extend it to use existing SpecieReference 
    public Edge createEdge(SpecieReference sr, Node reactionNode, Node otherNode)
    {
        Edge edge = sr.isProduct() ? new Edge(sr, reactionNode, otherNode) : new Edge(sr, otherNode, reactionNode);
        reactionNode.addEdge(edge);
        return edge;
    }

    /**
     * Returns true if node is appropriate as reaction participant. Was created to avid reactions with compartments in first place.
     * Override in subclasses to specify according your diagram type.
     */
    public boolean acceptForReaction(Node node)
    {
        return node != null && node.getRole() instanceof VariableRole && ! ( node instanceof Compartment );
    }

    public void putResults(List<DiagramElement> elements)
    {
        StreamEx.of(elements).select(Node.class).forEach(n -> n.getOrigin().put(n));
        StreamEx.of(elements).select(Edge.class).forEach(e -> e.getOrigin().put(e));

        if( reaction != null && reaction.getOrigin() != null )
            reaction.getOrigin().put(reaction);
    }

    public void putResults(DiagramElementGroup elements)
    {
        elements.nodesStream().forEach(n -> n.getOrigin().put(n));
        elements.edgesStream().forEach(e -> e.getOrigin().put(e));
        putReaction();
    }

    public void putReaction()
    {
        if( reaction != null && reaction.getOrigin() != null )
            reaction.getOrigin().put(reaction);
    }

    protected DataCollection<Reaction> initReactionOrigin(Diagram diagram)
    {
        return getReactionOrigin(diagram);
    }

    public static DataCollection<Reaction> getReactionOrigin(Diagram diagram)
    {
        Module module = null;
        try
        {
            module = Module.getModule(diagram);
        }
        catch( InternalException ex )
        {
            //ex.log();
            return null;
        }
        return module.getType().isCategorySupported() ? module.getCategory(Reaction.class) : null;
    }

    public static String generateReactionName(Diagram diagram)
    {
        DataCollection<?> origin = getReactionOrigin(diagram);
        if( origin == null )
            return DefaultSemanticController.generateUniqueName(diagram, "Reaction", false);
        return DefaultSemanticController.generateReactionName(origin);
    }
}