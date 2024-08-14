package biouml.plugins.agentmodeling;

import java.awt.Point;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.standard.diagram.SubDiagramProperties;

public class AgentSubDiagramProperties extends SubDiagramProperties
{
    public AgentSubDiagramProperties(Diagram diagram)
    {
        super(diagram);
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        DiagramElementGroup elements = super.doCreateElements( compartment, location, viewPane );
        DiagramElement de = elements.get(0);
        AgentModelSemanticController.addDynamicProperties(de);
        return elements;
    }
}
