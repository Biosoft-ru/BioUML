package biouml.plugins.ensembl.analysis;

import java.util.List;

import ru.biosoft.bsa.Site;

/**
 * @author lan
 *
 */
public class CountSiteAggregator extends SiteAggregator
{
    @Override
    public String getName()
    {
        return "Count";
    }
    
    @Override
    public Object aggregate(Site gene, int fivePrimeSize, int threePrimeSize, List<SiteData> siteData)
    {
        return siteData == null ? 0 : siteData.size();
    }

    @Override
    public Class<?> getType()
    {
        return Integer.class;
    }
}
