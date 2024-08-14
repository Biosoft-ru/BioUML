package biouml.plugins.gxl;

import biouml.model.Edge;
import biouml.model.Node;

/**
 * The simpliest implementation of SAX parser context for parsing supported
 * sibset of GXL. Possible further support of hypergraphs will require
 * stack-implemented context.
 */
public class GxlParserContext
{
    protected Node currentNode;
    public void enterNodeContext(Node node)
    {
        currentNode = node;
    }
    public Node getNodeContext()
    {
        return currentNode;
    }
    public void leaveNodeContext()
    {
        currentNode = null;
    }

    protected Edge currentEdge;
    public void enterEdgeContext(Edge edge)
    {
        currentEdge = edge;
    }
    public Edge getEdgeContext()
    {
        return currentEdge;
    }
    public void leaveEdgeContext()
    {
        currentEdge = null;
    }
}