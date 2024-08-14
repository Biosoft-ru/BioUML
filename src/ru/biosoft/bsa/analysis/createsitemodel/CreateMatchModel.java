package ru.biosoft.bsa.analysis.createsitemodel;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.MatchSiteModel;

@ClassIcon("resources/create_match.gif")
public class CreateMatchModel extends CreateSiteModelAnalysis<CreateMatchModelParameters>
{
    public CreateMatchModel(DataCollection<?> origin, String name)
    {
        super(origin, name, new CreateMatchModelParameters());
    }

    @Override
    protected @Nonnull SiteModel createModel(String name, DataCollection<?> origin)
    {
        FrequencyMatrix matrix = parameters.getMatrixPath().getDataElement(FrequencyMatrix.class);
        if(parameters.isDefaultCore())
            return new MatchSiteModel(name, origin, matrix, parameters.getCutoff(), parameters.getCoreCutoff());
        return new MatchSiteModel(name, origin, matrix, parameters.getCutoff(), parameters.getCoreCutoff(), parameters.getCoreStart(), parameters.getCoreLength());
    }
}
