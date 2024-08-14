package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProbeTableType;

@ClassIcon("resources/probes-affymetrix.gif")
public class AffymetrixProbeTableType extends ProbeTableType
{
    @Override
    public String getSource()
    {
        return "Affymetrix";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches(".+_[as]t")) return SCORE_HIGH_SPECIFIC;
        if(id.matches("\\d+") && Integer.parseInt(id)>1000000) return SCORE_MEDIUM_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
