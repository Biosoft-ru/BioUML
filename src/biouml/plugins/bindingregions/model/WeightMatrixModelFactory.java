package biouml.plugins.bindingregions.model;

import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixModel;

public class WeightMatrixModelFactory implements SiteModelFactory
{

    /***
    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold)
    {
        return new WeightMatrixModel(name, null, matrix, threshold);
    }
    ***/

    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold, Integer window)
    {
        return new WeightMatrixModel(name, null, matrix, threshold);
    }
}
