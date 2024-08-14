package ru.biosoft.access.biohub;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.jobcontrol.FunctionJobControl;

import ru.biosoft.access.core.DataElementPath;

/**
 * Base interface for BioHubs
 */
public interface BioHub
{
    /**
     * BioHub properties used in DataCollection configs
     */
    public static final String BIOHUB_CLASS = "bioHub";
    public static final String BIOHUB_NAME = "bioHubName";
    // search direction constants
    public final static int DIRECTION_UNDEFINED = -1;
    public final static int DIRECTION_UP = 0;
    public final static int DIRECTION_DOWN = 1;
    public final static int DIRECTION_BOTH = 2;
    
    /**
     * Returns public name
     */
    public String getName();
    
    /**
     * @return short name
     */
    public String getShortName();
    
    /**
     * Get priority of this BioHub for selected targets
     * 0 - BioHub is not available for this {@link TargetOptions}
     */
    public int getPriority(TargetOptions dbOptions);
    /**
     * Return array of equivalent elements using selected databases.
     * @param startElement - start element to search
     * @param dbOptions - target databases
     * @param relationTypes - relation types to use, null - all possible types
     * @param maxLength - max length of path
     * @param direction - search direction
     */
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction);
    /**
     * Return Map of equivalent elements for group of start elements using selected databases.
     * The same as getReference but for group of startElement. Can be used for request optimization.
     * @param startElements - array of start elements to search
     * @param dbOptions - target databases
     * @param relationTypes - relation types to use, null - all possible types
     * @param maxLength - max length of path
     * @param direction - search direction
     */
    public Map<Element, Element[]> getReferences(Element[] startElements, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction);
    /**
     * Returns the minimal path between elements. Null if path not exists in current BioHub.
     * @param element1 - start element in chain
     * @param element2 - end element in chain
     * @param dbOptions - target databases
     * @param relationTypes - relation types to use, null - all possible types
     * @param maxLength - max length of path
     * @param direction - search direction
     */
    public Element[] getMinimalPath(Element element1, Element element2, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction);
    
    public List<Element[]> getMinimalPaths(Element key, Element[] targets, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction);

    // Matching BioHub interface follows
    
    /**
     * Returns list of input types supported by this hub
     */
    public Properties[] getSupportedInputs();
    
    /**
     * Returns list of output types supported when matching object of given input type
     */
    public Properties[] getSupportedMatching(Properties input);
    
    /**
     * Returns how good this hub can match from given input type to given output type
     * 1 = best; 0 = no matching possible
     */
    public double getMatchingQuality(Properties input, Properties output);

    /**
     * Species Latin name
     */
    public static final @Nonnull String SPECIES_PROPERTY = "Species";
    /**
     * Reference type string ID
     */
    public static final @Nonnull String TYPE_PROPERTY = "ReferenceType";
    /**
     * Database version string
     */
    public static final @Nonnull String VERSION_PROPERTY = "Version";
    /**
     * Database version string
     */
    public static final @Nonnull String PROJECT_PROPERTY = "Project";
    /**
     * Performs the matching from given input type to given output type
     * Properties may contain list of advanced properties such as species or database version which should be used during matching
     */
    public Map<String, String[]> getReferences(String[] inputList, Properties input, Properties output, FunctionJobControl jobControl);
    
    public DataElementPath getModulePath();
}
