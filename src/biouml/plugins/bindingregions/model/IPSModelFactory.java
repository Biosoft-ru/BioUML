package biouml.plugins.bindingregions.model;

import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;

public class IPSModelFactory implements SiteModelFactory
{

    /***
    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold)
    {
        return new IPSSiteModel(name, null, new FrequencyMatrix[] {matrix}, threshold, -1, IPSSiteModel.DEFAULT_WINDOW);
    }
    ***/

    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold, Integer window)
    {
        int actualWindow = window == null ? IPSSiteModel.DEFAULT_WINDOW : window;  
        return new IPSSiteModel(name, null, new FrequencyMatrix[]{matrix}, threshold, -1, actualWindow);
    }
}
