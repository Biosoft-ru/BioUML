package biouml.plugins.sbgn;

import java.awt.Point;

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
import biouml.standard.type.Specie;
import biouml.standard.type.Stub;
import ru.biosoft.graphics.editor.ViewEditorPane;

@PropertyName ( "Phenotype properties" )
@PropertyDescription ( "Phenotype properties." )
public class PhenotypeProperties extends SbgnElementProperties
{
    private Diagram diagram;
    private String[] availableNodes;
    private String[] nodeNames;

    public PhenotypeProperties(Diagram diagram, String name)
    {
        super(Type.TYPE_PHENOTYPE, name);
        setProperties(SbgnSemanticController.getDPSByType(Type.TYPE_PHENOTYPE));
        this.diagram = diagram;
        availableNodes = diagram.recursiveStream().select(Node.class)
                .filter(de -> ( de.getKernel() instanceof Specie && de.getRole() instanceof VariableRole ))
                .map(DiagramElement::getCompleteNameInDiagram).toArray(String[]::new);
    }

    @PropertyName ( "Modifiers" )
    @PropertyDescription ( "Modifiers." )
    public String[] getNodeNames()
    {
        return nodeNames;
    }

    public void setNodeNames(String[] nodeNames)
    {
        this.nodeNames = nodeNames;
    }

    public String[] getAvailableNames()
    {
        return availableNodes;
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        DiagramElementGroup result = super.doCreateElements(compartment, location, viewPane);
        Node phenotypeNode = (Node)result.get(0);
        for( DynamicProperty dp : getProperties() )
            phenotypeNode.getAttributes().add(dp);

        for( String otherNodeName : getNodeNames() )
        {
            Node otherNode = diagram.findNode(otherNodeName);
            String edgeName = DefaultSemanticController.generateUniqueName(diagram, phenotypeNode.getName() + "_mod");
            //String edgeName = DefaultSemanticController.generateUniqueNodeName(diagram, Type.TYPE_REGULATION);
            Edge e = new Edge(new Stub(null, edgeName, Type.TYPE_REGULATION), otherNode, phenotypeNode);
            SbgnSemanticController.setNeccessaryAttributes(e);
            result.add(e);
        }
        return result;
    }
}
