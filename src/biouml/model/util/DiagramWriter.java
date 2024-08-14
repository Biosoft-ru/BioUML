package biouml.model.util;

import org.w3c.dom.Element;

import biouml.model.Edge;
import biouml.model.Node;

public interface DiagramWriter
{
    public void writeNode(Element parent, Node node);

    public void writeEdge(Element parent, Edge edge);
}
