package biouml.plugins.sbgn;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

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
import biouml.standard.type.Specie;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.editor.ViewEditorPane;

@PropertyName ( "Equivalence operator properties" )
@PropertyDescription ( "Equivalence operator properties." )
public class EquivalenceOperatorProperties extends SbgnElementProperties
{
    private final Diagram diagram;
    private final String[] availableNodes;
    private String[] nodeNames;
    private String mainNodeName;

    public EquivalenceOperatorProperties(Diagram diagram, String name)
    {
        super(Type.TYPE_EQUIVALENCE, name);
        setProperties(SbgnSemanticController.getDPSByType(Type.TYPE_EQUIVALENCE));
        this.diagram = diagram;
        availableNodes = diagram.recursiveStream().select(Node.class)
                .filter(de -> ( de.getKernel() instanceof Specie && de.getRole() instanceof VariableRole ))
                .map(DiagramElement::getCompleteNameInDiagram).toArray(String[]::new);
    }

    @PropertyName ( "Subtype entities" )
    @PropertyDescription ( "Subtype entities." )
    public String[] getNodeNames()
    {
        return nodeNames;
    }
    public void setNodeNames(String[] nodeNames)
    {
        this.nodeNames = nodeNames;
    }

    @PropertyName ( "Main entity" )
    @PropertyDescription ( "Main entity." )
    public String getMainNodeName()
    {
        return mainNodeName;
    }
    public void setMainNodeName(String mainNodeName)
    {
        this.mainNodeName = mainNodeName;
    }

    public String[] getAvailableSubEntities()
    {
        return StreamEx.of(availableNodes).without(mainNodeName).toArray(String[]::new);
    }

    public StreamEx<String> getAvailableSuperEntities()
    {
        return StreamEx.of(availableNodes).filter(n -> !StreamEx.of(nodeNames).toSet().contains(n));
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        List<DiagramElement> result = new ArrayList<>();
        Node equivalenceNode = new Node(compartment, new Stub(null, getName(), Type.TYPE_EQUIVALENCE));
        result.add(equivalenceNode);
        getProperties().forEach(dp -> equivalenceNode.getAttributes().add(dp));
        String mainNodeName = getMainNodeName();
        Node mainNode = (Node)diagram.getDiagramElement(mainNodeName);
        for( String otherNodeName : getNodeNames() )
        {
            Node otherNode = diagram.findNode(otherNodeName);
            SbgnUtil.setSuperType(otherNode, mainNode);
            String name = DefaultSemanticController.generateUniqueNodeName(compartment, otherNode.getName() + "_isSubType");
            Edge e = new Edge(new Stub(null, name, Type.TYPE_EQUIVALENCE_ARC), otherNode, equivalenceNode);
            SbgnSemanticController.setNeccessaryAttributes(e);
            result.add(e);
        }
        String name = DefaultSemanticController.generateUniqueNodeName(compartment, mainNode.getName() + "_isSuperType");
        Edge e = new Edge(new Stub(null, name, Type.TYPE_EQUIVALENCE_ARC), equivalenceNode, mainNode);
        SbgnSemanticController.setNeccessaryAttributes(e);
        result.add(e);
        return new DiagramElementGroup(result);
    }
}
