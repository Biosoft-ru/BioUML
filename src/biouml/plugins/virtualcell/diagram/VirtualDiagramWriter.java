package biouml.plugins.virtualcell.diagram;

import biouml.model.util.DiagramXmlWriter;
import biouml.model.util.ModelXmlWriter;
import biouml.plugins.physicell.PhysicellModelWriter;

public class VirtualDiagramWriter extends DiagramXmlWriter
{
    @Override
    protected ModelXmlWriter getModelWriter()
    {
        return new PhysicellModelWriter();
    }
}
