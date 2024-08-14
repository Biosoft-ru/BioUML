package biouml.plugins.gtrd;

import ru.biosoft.access.Entry;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.transformer.WeightMatrixTransformer;

public class GTRDMatrixTransformer extends WeightMatrixTransformer
{
    @Override
    public Class<GTRDMatrix> getOutputType()
    {
        return GTRDMatrix.class;
    }

    @Override
    public FrequencyMatrix transformInput(Entry input) throws Exception
    {
        FrequencyMatrix matrix = super.transformInput(input);
        return new GTRDMatrix(matrix.getOrigin(), matrix.getName(), matrix);
    }
}
