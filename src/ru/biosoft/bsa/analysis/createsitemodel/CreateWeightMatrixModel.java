package ru.biosoft.bsa.analysis.createsitemodel;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.LogOddsWeightMatrixModel;
import ru.biosoft.bsa.analysis.LogWeightMatrixModel;
import ru.biosoft.bsa.analysis.WeightMatrixModel;

@ClassIcon("resources/create_pwm.gif")
public class CreateWeightMatrixModel extends CreateSiteModelAnalysis<CreateWeightMatrixModelParameters>
{
    public CreateWeightMatrixModel(DataCollection<?> origin, String name)
    {
        super(origin, name, new CreateWeightMatrixModelParameters());
    }

    @Override
    protected @Nonnull SiteModel createModel(String name, DataCollection<?> origin)
    {
        FrequencyMatrix matrix = parameters.getMatrixPath().getDataElement(FrequencyMatrix.class);
        String modelType = parameters.getModelType();
        if( modelType.equals("Identity") )
            return new WeightMatrixModel(name, origin, matrix, parameters.getThreshold());
        else if( modelType.equals("Log") )
            return new LogWeightMatrixModel(name, origin, matrix, parameters.getThreshold());
        else if( modelType.equals("LogOdds") )
            return new LogOddsWeightMatrixModel(name, origin, matrix, parameters.getThreshold(), parameters.getNucleotideFrequencies().toArray());
        throw new ParameterNotAcceptableException( "modelType", modelType );
    }
}
