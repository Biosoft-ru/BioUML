package biouml.plugins.ensembl.analysis;

import java.util.List;

import biouml.plugins.ensembl.analysis.SiteData.Location;

import ru.biosoft.bsa.Site;

/**
 * @author lan
 *
 */
public class LocationCountSiteAggregator extends SiteAggregator
{
    private String name;
    private Location location;
    
    public LocationCountSiteAggregator(String name, Location location)
    {
        this.name = name;
        this.location = location;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Object aggregate(Site gene, int fivePrimeSize, int threePrimeSize, List<SiteData> siteData)
    {
        if(siteData == null) return 0;
        return siteData.stream().map( SiteData::getLocation ).filter( location::equals ).count();
    }

    @Override
    public Class<?> getType()
    {
        return Integer.class;
    }
}
