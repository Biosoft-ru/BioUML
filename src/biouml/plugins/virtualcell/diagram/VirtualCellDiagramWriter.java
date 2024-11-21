package biouml.plugins.virtualcell.diagram;

import biouml.model.util.DiagramXmlWriter;
import biouml.model.util.ModelXmlWriter;

public class VirtualCellDiagramWriter extends DiagramXmlWriter
{
    @Override
    protected ModelXmlWriter getModelWriter()
    {
        return new VirtualCellModelWriter();
    }
}
