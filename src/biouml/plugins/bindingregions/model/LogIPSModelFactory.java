package biouml.plugins.bindingregions.model;

import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.bsa.analysis.LogIPSSiteModel;
import ru.biosoft.bsa.analysis.LogWeightMatrixModelWithModeratePseudocounts;
import ru.biosoft.bsa.analysis.WeightMatrixModel;

public class LogIPSModelFactory implements SiteModelFactory
{
    /***
    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold)
    {
        WeightMatrixModel[] array = new WeightMatrixModel[] {new LogWeightMatrixModelWithModeratePseudocounts(name, null, matrix,
                -Double.MAX_VALUE)};
        return new LogIPSSiteModel(name, null, array, threshold, IPSSiteModel.DEFAULT_DIST_MIN);
    }
    ***/
    
    @Override
    public SiteModel create(String name, FrequencyMatrix matrix, double threshold, Integer window)
    {
        int actualWindow = window == null ? IPSSiteModel.DEFAULT_WINDOW : window;
        WeightMatrixModel[] weightMatrixModels = new WeightMatrixModel[]{new LogWeightMatrixModelWithModeratePseudocounts(name, null, matrix, -Double.MAX_VALUE)};
        return new LogIPSSiteModel(name, null, weightMatrixModels, threshold, actualWindow, LogIPSSiteModel.DEFAULT_MULTIPLIER);
    }
}
