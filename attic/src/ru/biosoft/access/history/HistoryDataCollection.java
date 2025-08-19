package ru.biosoft.access.history;

import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

/**
 * Base interface for history collection
 */
public interface HistoryDataCollection extends DataCollection
{
    /**
     * Get ID for new history element
     */
    public String getNextID();
    
    /**
     * Get next version for selected element
     */
    public int getNextVersion(DataElementPath elementPath);
    
    /**
     * Get list of history elements for selected object, ordered from maxVersion to minVersion
     * @param elementPath data element path
     * @param minVersion minimal version number to request (0 to get all versions)
     * @return
     */
    public List<String> getHistoryElementNames(DataElementPath elementPath, int minVersion);
}
