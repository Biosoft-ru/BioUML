package ru.biosoft.access;

import java.io.File;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;

/**
 * @todo Document it.
 * @author DevelopmentOnTheEdge
 * @version 1.0
 */
public interface Repository extends DataCollection<DataCollection<?>>
{
    /**
     * Creates new ru.biosoft.access.core.DataCollection.
     * 
     * @param name            DataCollection name
     * @param properties      DataCollection properties
     * @param subDir          Sub directory for new collection.
     * @param files           Files to be moved or copied into repository
     * @param controller      Object for special control functions (dialogs for example).
     * @return Created data collection, or null.
     * @exception Exception   If error occured.
     */
    DataCollection createDataCollection(String name, Properties properties, String subDir, File[] files,
                                         CreateDataCollectionController controller ) throws Exception;
    
    /**
     * Invalidate all loaded collections from repository
     */
    public void updateRepository();
}
