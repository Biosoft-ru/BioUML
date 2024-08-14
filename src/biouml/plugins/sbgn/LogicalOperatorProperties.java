package biouml.plugins.sbgn;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.ReactionUtility;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.DPSUtils;

@PropertyName ( "Logical operator properties" )
@PropertyDescription ( "Logical operator properties." )
public class LogicalOperatorProperties extends SbgnElementProperties
{
    static final String[] operatorTypes = new String[] {"And", "Or", "Not"};
    private final Diagram diagram;
    private final String[] availableNodes;
    private final String[] availableReactions;

    private String modifierType = "modulation";
    private String[] nodeNames;
    private String reactionName;

    public LogicalOperatorProperties(Diagram diagram, String name)
    {
        super(Type.TYPE_LOGICAL, name);
        setProperties(SbgnSemanticController.getDPSByType(Type.TYPE_LOGICAL));
        this.diagram = diagram;

        availableReactions = diagram.recursiveStream().select(Node.class).filter(de -> de.getKernel() instanceof Reaction)
                .map(DiagramElement::getCompleteNameInDiagram).toArray(String[]::new);

        if( availableReactions.length > 0 )
            reactionName = availableReactions[0];

        availableNodes = diagram.recursiveStream().select(Node.class)
                .filter(de -> ( de.getKernel() instanceof Specie && de.getRole() instanceof VariableRole ))
                .map(DiagramElement::getCompleteNameInDiagram).toArray(String[]::new);
    }

    @PropertyName ( "Modifiers" )
    @PropertyDescription ( "Modifier species." )
    public String[] getNodeNames()
    {
        return nodeNames;
    }
    public void setNodeNames(String[] nodeNames)
    {
        this.nodeNames = nodeNames;
    }

    @PropertyName ( "Reaction" )
    @PropertyDescription ( "Traget reaction." )
    public String getReactionName()
    {
        return reactionName;
    }
    public void setReactionName(String reactionName)
    {
        this.reactionName = reactionName;
    }

    @PropertyName ( "Modifier type" )
    @PropertyDescription ( "Modifier type." )
    public String getModifierType()
    {
        return modifierType;
    }
    public void setModifierType(String modifierType)
    {
        this.modifierType = modifierType;
    }

    public String[] getAvailableNames()
    {
        return availableNodes;
    }
    public String[] getAvailableReactions()
    {
        return availableReactions;
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        List<DiagramElement> result = new ArrayList<>();
        Node logicalNode = new Node(compartment, new Stub(null, getName(), Type.TYPE_LOGICAL));
        result.add(logicalNode);
        getProperties().forEach(dp -> logicalNode.getAttributes().add(dp));
        String reactionName = getReactionName();
        Node reactionNode = diagram.findNode(reactionName);
        Reaction r = (Reaction)reactionNode.getKernel();
        for( String otherNodeName : getNodeNames() )
        {
            Node otherNode = diagram.findNode(otherNodeName);
            SpecieReference reference = new SpecieReference(r, r.getName(), otherNode.getKernel().getName(), SpecieReference.MODIFIER);
            r.put(reference);
            reference.setSpecie(otherNode.getCompleteNameInDiagram());
            Edge e = new Edge(Compartment.findCommonOrigin(logicalNode, otherNode), reference, otherNode, logicalNode);
            //this is specie reference but we need to show it on the diagram differently);
            e.getAttributes().add(DPSUtils.createReadOnly(SBGNPropertyConstants.SBGN_EDGE_TYPE, String.class, Type.TYPE_LOGIC_ARC));
            SbgnSemanticController.setNeccessaryAttributes(e);
            result.add(e);
        }
        int ref = (int)ReactionUtility.getModifiers(r).count() + 1;
        String edgeName = DefaultSemanticController.generateUniqueName(diagram, r.getName() + "_mod_" + ref);
        Edge reactionEdge = new Edge(Compartment.findCommonOrigin(logicalNode, reactionNode),
                new Stub(null, edgeName, Type.TYPE_REGULATION), logicalNode, reactionNode);
        SbgnSemanticController.setNeccessaryAttributes(reactionEdge);
        result.add(reactionEdge);
        return new DiagramElementGroup(result);
    }

}
