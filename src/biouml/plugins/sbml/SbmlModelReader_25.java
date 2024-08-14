package biouml.plugins.sbml;

import java.util.logging.Logger;
import org.w3c.dom.Element;

import biouml.model.DiagramType;

public class SbmlModelReader_25 extends SbmlModelReader_24
{
    public SbmlModelReader_25()
    {
        log = Logger.getLogger(SbmlModelReader_25.class.getName());
    }

    @Override
    protected DiagramType getDiagramType(Element modelElement)
    {
        return new SbmlDiagramType_L2v5();
    }
}
