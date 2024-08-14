package biouml.plugins.bindingregions.model;

import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.MatchSiteModel;

public class MatchModelFactory implements SiteModelFactory
{
    /***
    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold)
    {
        return new MatchSiteModel(name, null, matrix, threshold, 0.0);
    }
    ***/
    
    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold, Integer window)
    {
        return new MatchSiteModel(name, null, matrix, threshold, 0.0);
    }
}
