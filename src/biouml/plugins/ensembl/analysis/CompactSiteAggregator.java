package biouml.plugins.ensembl.analysis;

import java.util.List;

import ru.biosoft.bsa.Site;

/**
 * @author lan
 *
 */
public class CompactSiteAggregator extends SiteAggregator
{
    @Override
    public String getName()
    {
        return "+ or -";
    }

    @Override
    public Object aggregate(Site gene, int fivePrimeSize, int threePrimeSize, List<SiteData> siteData)
    {
        return (siteData == null || siteData.size() == 0) ? "-" : "+";
    }
}
