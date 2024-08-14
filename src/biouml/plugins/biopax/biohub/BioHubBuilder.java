
package biouml.plugins.biopax.biohub;

import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.TargetOptions;

/**
 * Interface for BioHub creation
 *
 */
public interface BioHubBuilder
{
    /**
     * Add reference between two elements to BioHub
     * @param elementFrom - start element
     * @param elementTo - end element
     * @param dbOptions - options
     * @param relationType - relation type of the elements
     * @param length - reference length
     * @param direction - reference direction
     */
    public void addReference(Element elementFrom, Element elementTo, TargetOptions dbOptions, String relationType, int length, int direction);
    public void addReference(Element input, ReferenceType inputType, Element output, ReferenceType outputType, boolean isMain);
    public ReferenceType[] getMatchingTypes();
    public void finalizeBuilding();
}
