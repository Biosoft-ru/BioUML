package biouml.plugins.research.workflow;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.MessageBundle;
import biouml.standard.type.Base;
import biouml.standard.type.Type;

/**
 * Workflow diagram type
 */
public class WorkflowDiagramType extends DiagramTypeSupport
{
    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return true;
    }

    public WorkflowDiagramType()
    {
        diagramViewBuilder = new WorkflowDiagramViewBuilder();
        semanticController = new WorkflowSemanticController();
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {Type.ANALYSIS_METHOD, Type.ANALYSIS_PARAMETER, Type.ANALYSIS_EXPRESSION, Type.ANALYSIS_CYCLE,
                Type.ANALYSIS_SCRIPT, Type.TYPE_NOTE};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {Base.TYPE_DIRECTED_LINK, Type.TYPE_NOTE_LINK};
    }

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram d = super.createDiagram(origin, diagramName, kernel);
        d.getInfo().setNodeImageLocation(MessageBundle.class, "resources/workflow.gif");
        return d;
    }

    @Override
    public boolean needLayout(Node node)
    {
        return ( node instanceof Compartment ) && ( node.getKernel() == null || ! ( node.getKernel().getType().equals(Type.ANALYSIS_METHOD)))
                && !node.getKernel().getType().equals(Type.ANALYSIS_CYCLE) ;
    }

    @Override
    public boolean useConverterOnAdd()
    {
        return false;
    }
}
