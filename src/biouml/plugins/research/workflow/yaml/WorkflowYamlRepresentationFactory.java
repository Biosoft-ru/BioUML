package biouml.plugins.research.workflow.yaml;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import ru.biosoft.gui.Document;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.workbench.diagram.AbstractDiagramTextRepresentation;
import biouml.workbench.diagram.DiagramTextRepresentation;
import biouml.workbench.diagram.DiagramTextRepresentationFactory;

import com.developmentontheedge.application.Application;

public class WorkflowYamlRepresentationFactory extends DiagramTextRepresentationFactory
{
    @Override
    protected DiagramTextRepresentation create(Diagram d)
    {
        DiagramType type = d.getType();
        if(type instanceof WorkflowDiagramType)
        {
            return new WorkflowYamlRepresentation(d);
        }
        return null;
    }
    
    private static class WorkflowYamlRepresentation extends AbstractDiagramTextRepresentation
    {
        public WorkflowYamlRepresentation(Diagram workflow)
        {
            super(workflow);
        }

        @Override
        public String getContentType()
        {
            return "text/x-yaml";
        }

        @Override
        public String getContent()
        {
            WorkflowToYamlConverter converter = new WorkflowToYamlConverter();
            Map<String, Object> yamlModel = converter.convert( diagram );
            return new Yaml().dump( yamlModel );
        }

        @Override
        public void doSetContent(String text)
        {
            Document document = (Document)Application.getApplicationFrame().getDocumentManager().getActiveDocument();
            WorkflowUpdater updater = new WorkflowUpdater( diagram, document );
            updater.updateWorkflow( text );
        }
    }
}
