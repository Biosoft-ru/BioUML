package biouml.plugins.physicell;

import biouml.model.util.DiagramXmlWriter;
import biouml.model.util.ModelXmlWriter;

public class PhysicellDiagramWriter extends DiagramXmlWriter
{
    @Override
    protected ModelXmlWriter getModelWriter()
    {
        return new PhysicellModelWriter();
    }
}
