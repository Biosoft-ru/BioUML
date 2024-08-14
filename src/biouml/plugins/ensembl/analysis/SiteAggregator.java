package biouml.plugins.ensembl.analysis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.ensembl.analysis.SiteData.Location;

import ru.biosoft.bsa.Site;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * Abstract class which allows to aggregate information about several Sites into single table cell
 * @author lan
 */
public abstract class SiteAggregator
{
    abstract public String getName();
    
    public Class<?> getType()
    {
        return String.class;
    }
    
    abstract public Object aggregate(Site gene, int fivePrimeSize, int threePrimeSize, List<SiteData> siteData);
    
    @Override
    public String toString()
    {
        return getName();
    }
    
    private static final SiteAggregator[] aggregators = {
        new ViewSiteAggregator(),
        new CompactSiteAggregator(),
        new CountSiteAggregator(),
        new LocationCountSiteAggregator("Count in exons", Location.EXON),
        new LocationCountSiteAggregator("Count in introns", Location.INTRON),
        new LocationCountSiteAggregator("Count in 5'", Location.FIVE_PRIME),
        new LocationCountSiteAggregator("Count in 3'", Location.THREE_PRIME),
        new StructureSiteAggregator(),
        new PositionsSiteAggregator()
    };
    
    static final Map<String, Location> locationNames = new LinkedHashMap<>();
    
    static
    {
        locationNames.put("5'", Location.FIVE_PRIME);
        locationNames.put("Gene", Location.GENE);
        locationNames.put("Exon", Location.EXON);
        locationNames.put("Intron", Location.INTRON);
        locationNames.put("3'", Location.THREE_PRIME);
    }

    public static SiteAggregator createInstance(String name)
    {
        for(SiteAggregator aggregator: aggregators)
        {
            if(aggregator.toString().equals(name))
                return aggregator;
        }
        return null;
    }
    
    public static class SiteAggregatorSelector extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return aggregators;
        }
    }
}
