package biouml.plugins.kegg;

import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.standard.diagram.PathwaySemanticController;
import biouml.standard.type.Type;

/** @todo implement */
public class KeggPathwaySemanticController extends PathwaySemanticController
{

    /**
     * If node is compartment or node kernel is {@link TYPE_DIAGRAM_REFERENCE} then
     * it can be resized.
     */
    @Override
    public boolean isResizable(DiagramElement de)
    {
        if( de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_DIAGRAM_REFERENCE) && de instanceof Node )
            return true;

        return super.isResizable(de);
    }
}