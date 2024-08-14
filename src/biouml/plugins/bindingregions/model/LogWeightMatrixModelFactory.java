package biouml.plugins.bindingregions.model;

import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.LogWeightMatrixModelWithModeratePseudocounts;

public class LogWeightMatrixModelFactory implements SiteModelFactory
{
    /***
    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold)
    {
        return new LogWeightMatrixModelWithModeratePseudocounts(name, null, matrix, threshold);
    }
    ***/

    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold, Integer window)
    {
        return new LogWeightMatrixModelWithModeratePseudocounts(name, null, matrix, threshold);
    }
}
