package ru.biosoft.bsa.analysis.createsitemodel;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;

@ClassIcon("resources/create_ips.gif")
public class CreateIPSModel extends CreateSiteModelAnalysis<CreateIPSModelParameters>
{
    public CreateIPSModel(DataCollection<?> origin, String name)
    {
        super(origin, name, new CreateIPSModelParameters());
    }

    @Override
    protected @Nonnull SiteModel createModel(String name, DataCollection<?> origin) throws Exception
    {
        FrequencyMatrix[] matrices = parameters.getFrequencyMatrices().elements( FrequencyMatrix.class ).toArray( FrequencyMatrix[]::new );
        return new IPSSiteModel(name, origin, matrices, parameters.getCritIPS(),
                parameters.getDistMin(), parameters.getWindowSize());
    }
}
