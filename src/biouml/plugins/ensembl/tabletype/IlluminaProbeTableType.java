
package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProbeTableType;

/**
 * @author anna
 *
 */
@ClassIcon("resources/probes-illumina.gif")
public class IlluminaProbeTableType extends ProbeTableType
{

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("ILMN_\\d{7,}")) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "Illumina";
    }

}
