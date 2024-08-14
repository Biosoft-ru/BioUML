package biouml.plugins.research.research;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.plugins.research.MessageBundle;
import biouml.standard.type.Base;
import biouml.standard.type.Type;

/**
 * Research diagram type
 */
public class ResearchDiagramType extends DiagramTypeSupport
{
    public ResearchDiagramType()
    {
        diagramViewBuilder = new ResearchDiagramViewBuilder();
        semanticController = new ResearchSemanticController();
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {Type.TYPE_DATA_ELEMENT, Type.ANALYSIS_METHOD, Type.TYPE_EXPERIMENT, Type.TYPE_SIMULATION_RESULT,
                Type.TYPE_NOTE};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {Base.TYPE_DIRECTED_LINK, Base.TYPE_UNDIRECTED_LINK, Type.TYPE_NOTE_LINK};
    }
    
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram d = super.createDiagram(origin, diagramName, kernel);
        d.getInfo().setNodeImageLocation(MessageBundle.class, "resources/workflow.gif");
        return d;
    }
}
