package biouml.plugins.keynodes.graph;

import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.standard.type.RelationType;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;

/**
 * An interface representing the edge of {@link HubGraph}
 * 
 * @author lan
 */
public interface HubEdge
{
    /**
     * Create {@link Element} by this edge.
     * 
     * @param hub hub for which element should be created. Can be used to access {@link BioHubSupport#getModulePath()} method
     * or specific methods in specific hub implementation.
     * @return a new element linked to the reaction or relation in the tree.
     */
    public Element createElement(KeyNodesHub<?> hub);

    /**
     * Returns the corresponding relation type string for given edge
     * 
     * @param upstream if true then relation from output node to input node is requested, otherwise the relation from input node to output node is requested.
     * @return string representation of relation
     * @see RelationType
     */
    public String getRelationType(boolean upstream);
}
