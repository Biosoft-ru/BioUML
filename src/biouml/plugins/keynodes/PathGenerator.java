package biouml.plugins.keynodes;

import java.util.List;

import ru.biosoft.access.biohub.Element;
import ru.biosoft.table.StringSet;
import biouml.plugins.keynodes.biohub.KeyNodesHub;

public interface PathGenerator
{
    /**
     * Generates shortest paths using given input.
     * @param startElement element to starts path with
     * @param hits elements which will be used in paths generating
     * @return list of paths
     */
    public List<Element[]> generatePaths(String startElement, StringSet hits);

    /**
     * Finds all reaction elements from all paths which can be generated
     * using given input.
     * @param startElement element to start paths with
     * @param hits elements which will be used in paths generating
     * @return list of reaction elements
     */
    public List<Element> getAllReactions(String startElement, StringSet hits);

    /**
     * Generates keys' names using given name
     * @param name name to generate keys' names from
     * @return set of keys' names
     */
    public StringSet getKeysFromName(String name);

    public KeyNodesHub<?> getKeyNodesHub();
}
