package biouml.plugins.wdl.diagram;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Base;

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
        return new Object[] {Base.TYPE_DIRECTED_LINK};
    }

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram d = super.createDiagram( origin, diagramName, kernel );
        return d;
    }

    @Override
    public boolean needLayout(Node node)
    {
        if( ! ( node instanceof Compartment ) )
            return false;
        Base kernel = node.getKernel();
        if( kernel == null )
            return true;
        return false;
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
