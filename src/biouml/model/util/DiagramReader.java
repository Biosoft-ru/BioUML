package biouml.model.util;

import org.w3c.dom.Element;

import biouml.model.Compartment;

public interface DiagramReader
{
    public void readNodes(Element element, Compartment compartment) throws Exception;

    public void readEdges(Element element, Compartment compartment) throws Exception;
}
