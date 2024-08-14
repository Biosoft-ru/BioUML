package ru.biosoft.access.generic;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Interface for generic object access
 */
public interface DataElementTypeDriver
{
    /**
     * Checks whether specified class is supported as child elements class
     * @param childClass class to check
     * @return true if this driver supports elements of specified type, false otherwise
     */
    public boolean isSupported(Class<? extends DataElement> childClass);
    
    /**
     * Saves ru.biosoft.access.core.DataElement into GenericDataCollection
     * @param gdc GenericDataCollection to store element to
     * @param de ru.biosoft.access.core.DataElement to store. It's guaranteed that isSupported previously returned true for the type of this ru.biosoft.access.core.DataElement
     * @param dei DataElementInfo describing de
     */
    public void doPut(GenericDataCollection gdc, DataElement de, DataElementInfo dei) throws Exception;
    
    /**
     * Fetches specified ru.biosoft.access.core.DataElement from GenericDataCollection
     * @param gdc GenericDataCollection to fetch element from
     * @param dei DataElementInfo describing element to fetch
     * dei.getProperties().getProperty(DataElementInfo.ELEMENT_CLASS) may be of interest
     * @return fetched ru.biosoft.access.core.DataElement
     * @throws Exception if there's no such element or some error occurred during the fetching
     */
    public DataElement doGet(GenericDataCollection gdc, DataElementInfo dei) throws Exception;
    
    /**
     * Removes specified ru.biosoft.access.core.DataElement from GenericDataCollection
     * @param gdc GenericDataCollection to remove element from
     * @param dei DataElementInfo describing element to remove
     * For faster operation element itself is not passed, but you can fetch it if necessary using gdc.get(dei.getName())
     * @throws Exception if error occurred during the element deletion
     */
    public void doRemove(GenericDataCollection gdc, DataElementInfo dei) throws Exception;
    
    /**
     * Creates base DataCollection to store elements handled by this driver and returns it
     * @param gdc GenericDataCollection for which base DataCollection should be created
     * @return created ru.biosoft.access.core.DataCollection
     * Usually base collection should implement all functionality which is normally expected from collection carrying such elements
     * E.g. if driver handles FileDataElement elements, then base collection should extend FileCollection as normally
     * FileDataElement elements are stored there.
     * If your driver implementation doesn't require base ru.biosoft.access.core.DataCollection, you can simply return gdc (though it's discouraged)
     */
    public DataCollection createBaseCollection(GenericDataCollection gdc);
    
    /**
     * Checks whether specified ru.biosoft.access.core.DataElement is a leaf (should not have subelements in the tree)
     * @param gdc GenericDataCollection in which element resides
     * @param dei DataElementInfo describing element to test
     * @return true if element is a leaf, false otherwise
     * Note that though you can create element itself using gdc.get(dei.getName()), it's strongly discouraged to do this,
     * as it can be very slow for big collections. Normally this method should simply return true or false in all cases
     * or maybe make a decision based on ru.biosoft.access.core.DataElement class, which can be fetched via dei.getProperties().getProperty(DataElementInfo.ELEMENT_CLASS)
     */
    public boolean isLeafElement(GenericDataCollection gdc, DataElementInfo dei);
    
    /**
     * Estimate the drivespace size taken by the data element in bytes
     * @param gdc parent collection
     * @param dei DataElementInfo for ru.biosoft.access.core.DataElement to estimate
     * @param recalc whether to recalculate the value or use cached one (if applicable)
     * @return size in bytes.
     */
    public long estimateSize(GenericDataCollection gdc, DataElementInfo dei, boolean recalc);
}
