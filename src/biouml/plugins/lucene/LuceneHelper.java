package biouml.plugins.lucene;

import java.util.Vector;

import ru.biosoft.access.core.DataElement;

/**
 * Lucene helper interface
 */
public interface LuceneHelper
{
    /**
     * Get available properties names
     */
    public Vector<String> getPropertiesNames();

    /**
     * Get value for bean field
     */
    public String getBeanValue(DataElement de, String name);
}
