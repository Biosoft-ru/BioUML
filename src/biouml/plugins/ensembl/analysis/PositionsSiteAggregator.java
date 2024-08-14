package biouml.plugins.ensembl.analysis;

import java.util.List;
import one.util.streamex.StreamEx;
import ru.biosoft.bsa.Site;

/**
 * @author lan
 *
 */
public class PositionsSiteAggregator extends SiteAggregator
{
    @Override
    public String getName()
    {
        return "Positions";
    }

    @Override
    public Object aggregate(Site gene, int fivePrimeSize, int threePrimeSize, List<SiteData> siteData)
    {
        if(siteData == null) return "";
        return StreamEx.of( siteData ).mapToInt( SiteData::getOffset ).sorted().distinct().joining( ", " );
    }
}
