package biouml.model.xml;

import java.awt.Point;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.ReactionPane;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import ru.biosoft.exception.InternalException;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class XmlReactionPane extends ReactionPane
{
    private final String type;
    private final String kernelType;
    private final DynamicPropertySet initAttributes;

    public XmlReactionPane(Diagram diagram, Compartment compartment, Point point, ViewEditorPane viewEditor, String type,
            String kernelType, DynamicPropertySet initAttributes)
    {
        super(diagram, compartment, point, viewEditor);
        this.type = type;
        this.kernelType = kernelType;
        this.initAttributes = initAttributes;
    }

    /**
     * @pending we create reaction in diagram compartment
     */
    @Override
    protected boolean createReaction() throws Exception
    {
        if( !isReactionValid() )
            throw new CreateReactionException(resources.getString("REACTION_COMPONENT_NOT_VALID"));

        Node reactionNode;

        viewEditor.startTransaction("Create reaction");

        if( kernelType != null && kernelType.equals("biouml.standard.type.Reaction") )
        {
            List<SpecieReference> componentsList = getComponentList();
            reactionNode = DiagramUtility.createReactionNode(diagram, compartment, null, componentsList, reactionEditPane.getFormula(),
                    point, type);
            reactionNode.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE_PD, String.class, type));
            for( Edge edge : reactionNode.getEdges() )
                edge.getAttributes().add( new DynamicProperty( XmlDiagramTypeConstants.XML_TYPE_PD, String.class, type ) );
        }
        else
        {
            String name = DefaultSemanticController.generateUniqueNodeName(compartment, type);

            Stub reaction = new Stub(null, name, type);
            reaction.setTitle(DiagramUtility.generateReactionTitle(getComponentList()));

            reactionNode = new Node(compartment, reaction);

            // add specie roles and edges
            for( SpecieReference prototype : getComponentList() )
            {
                Node de = diagram.findNode(prototype.getSpecie());

                Edge edge;
                String xmlType;
                if( prototype.getRole().equals(SpecieReference.PRODUCT) )
                {
                    xmlType = "production";
                    edge = new Edge(new Stub(null, "", xmlType), reactionNode, de);
                }
                else if( prototype.getRole().equals(SpecieReference.REACTANT) )
                {
                    xmlType = "consumption";
                    edge = new Edge(new Stub(null, "", xmlType), de, reactionNode);
                }
                else if( prototype.getRole().equals(SpecieReference.MODIFIER) )
                {
                    xmlType = "regulation";
                    edge = new Edge(new Stub(null, "", xmlType), de, reactionNode);
                }
                else
                    throw new InternalException("Invalid role: " + prototype.getRole() + "; specie = " + prototype);
                edge.getAttributes().add(new DynamicProperty("specieType", String.class, prototype.getRole()));
                DynamicProperty dp = new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE_PD, String.class, xmlType);
                edge.getAttributes().add(dp);
                edge.save();
            }
            reactionNode.setRelativeLocation(diagram, point);
            reactionNode.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE_PD, String.class, type));
        }

        for( DynamicProperty property : initAttributes )
            reactionNode.getAttributes().add(property);

        reactionNode = (Node)diagram.getType().getSemanticController().validate(diagram, reactionNode, true);
        compartment.put(reactionNode);
        
        viewEditor.completeTransaction();
        return true;
    }
}
