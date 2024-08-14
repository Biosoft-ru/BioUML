package biouml.plugins.ensembl.tabletype;

import java.util.regex.Pattern;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProbeTableType;

@ClassIcon("resources/probes-agilent.gif")
public class AgilentProbeTableType extends ProbeTableType
{
    Pattern pattern = Pattern.compile("A_\\d+_P\\d+|\\(\\-\\)3xSLv1|\\(\\+\\)E1A_r.+|\\(\\+\\)eQC\\-|CUST_\\d+_PI\\d+|RC\\d+|DCP_\\d+_\\d+|ETG\\d+_\\d+|GE_BrightCorner|DarkCorner");
    
    @Override
    public String getSource()
    {
        return "Agilent";
    }

    @Override
    public int getIdScore(String id)
    {
        if(pattern.matcher(id).matches()) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
