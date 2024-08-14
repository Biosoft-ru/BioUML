package biouml.plugins.research.workflow.yaml._test;

import biouml.model.Diagram;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.workbench.diagram.DiagramTextRepresentation;
import biouml.workbench.diagram.DiagramTextRepresentationFactory;
import ru.biosoft.access._test.AbstractBioUMLTest;

public class TestWorkflowYamlRepresentationFactory extends AbstractBioUMLTest
{
    public void testBasics() throws Exception
    {
        Diagram workflow = new WorkflowDiagramType().createDiagram(null, "workflow", null);
        DiagramTextRepresentation dtr = DiagramTextRepresentationFactory.getDiagramTextRepresentation( workflow );
        assertEquals("text/x-yaml", dtr.getContentType());
        assertEquals("{name: workflow}", dtr.getContent().trim());
    }
}
