package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProteinTableType;

@ClassIcon("resources/proteins-ipi.gif")
public class IPIProteinTableType extends ProteinTableType
{
    @Override
    public String getSource()
    {
        return "IPI";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("IPI\\d{8}(\\.\\d{1,2}|)")) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
