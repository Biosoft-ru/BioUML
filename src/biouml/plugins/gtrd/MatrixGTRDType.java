package biouml.plugins.gtrd;

import ru.biosoft.bsa.MatrixTableType;

public class MatrixGTRDType extends MatrixTableType
{

    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "GTRD";
    }
}
