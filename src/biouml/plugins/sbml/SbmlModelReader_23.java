package biouml.plugins.sbml;

import java.util.logging.Logger;
import org.w3c.dom.Element;

import biouml.model.DiagramType;
import biouml.model.Node;

public class SbmlModelReader_23 extends SbmlModelReader_22
{
    public SbmlModelReader_23()
    {
        log = Logger.getLogger(SbmlModelReader_23.class.getName());
    }

    @Override
    protected DiagramType getDiagramType(Element modelElement)
    {
        return new SbmlDiagramType_L2v3();
    }

    @Override
    public Node readSpecie(Element element, String specieId, String specieName) throws Exception
    {
        Node specie = super.readSpecie(element, specieId, specieName);
        readSBOTerm(element, specie.getAttributes());
        return specie;
    }
}
