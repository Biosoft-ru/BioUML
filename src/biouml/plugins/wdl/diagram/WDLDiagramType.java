package biouml.plugins.wdl.diagram;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;

/**
 * Workflow diagram type
 */
@PropertyName("Workflow diagram")
public class WDLDiagramType extends DiagramTypeSupport
{
    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return true;
    }

    public WDLDiagramType()
    {
        diagramViewBuilder = new WDLViewBuilder();
        semanticController = new WDLSemanticController();
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new String[] {WDLConstants.TASK_TYPE};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {WDLConstants.LINK_TYPE};
    }

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram d = super.createDiagram( origin, diagramName, kernel );
        WorkflowUtil.setVersion( d, "1.2");
        return d;
    }
    
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName) throws Exception
    {
        return createDiagram( origin,  diagramName, new DiagramInfo(diagramName) );
    }

    @Override
    public boolean needLayout(Node node)
    {
        return (WorkflowUtil.isCycle( node ) || WorkflowUtil.isConditional( node ));
    }

    @Override
    public boolean useConverterOnAdd()
    {
        return false;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }
}
