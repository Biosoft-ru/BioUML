package ru.biosoft.bsa;

import ru.biosoft.access.core.DataCollection;

public class SiteIntervalsMap extends IntervalMap<Site>
{
    public SiteIntervalsMap(DataCollection<Site> sitesCollection)
    {
        for(Site site: sitesCollection)
        {
            add(site.getFrom(), site.getTo(), site);
        }
    }
    
    public void add(Site site)
    {
        add(site.getFrom(), site.getTo(), site);
    }
}