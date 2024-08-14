package biouml.plugins.bindingregions.model;

import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;

/**
 * Create site model by matrix and threshold
 * @author lan
 */
public interface SiteModelFactory
{
    // public SiteModel create(String name, FrequencyMatrix matrix, double threshold);
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold, Integer window);
}
