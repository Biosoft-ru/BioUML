
package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.List;

import one.util.streamex.StreamEx;

import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;

/**
 * @author yura
 *
 */
public class CompositeElement
{
    Site firstSite;
    Site secondSite;
    
    CompositeElement(Site firstSite, Site secondSite)
    {
        this.firstSite = firstSite;
        this.secondSite = secondSite;
    }
    
    public Site getFirstSite()
    {
        return firstSite;
    }
    
    public Site getSecondSite()
    {
        return secondSite;
    }

    
    public static List<CompositeElement> getDistinctCompositeElements(Site firstSite, List<Site> fittedFilters, Track secondSiteCandidatesTrack, int maximalLengthOfFilter, int maximalDistanceToEdge) throws Exception
    {
        if( fittedFilters.isEmpty() ) return null;
        String chromosome = firstSite.getSequence().getName();
        List<Site> secondSites = new ArrayList<>();
        for( Site filter : fittedFilters )
        {
            for( Site site : secondSiteCandidatesTrack.getSites(chromosome, filter.getFrom(), filter.getTo()) )
            {
                int siteCenter = site.getInterval().getCenter();
                if( filter.getLength() <= maximalLengthOfFilter ||
                    Math.abs(siteCenter - filter.getFrom()) <= maximalDistanceToEdge ||
                    Math.abs(siteCenter - filter.getTo()) <= maximalDistanceToEdge
                  )
                    addSecondSite(site, secondSites);
            }
        }
        return StreamEx.of(secondSites).map( site -> new CompositeElement(firstSite, site) ).toList();
    }
    
    private static void addSecondSite(Site site, List<Site> sites)
    {
        if( sites.isEmpty() )
            sites.add(site);
        else
        {
            boolean isDistinct = sites.stream().noneMatch( listSite -> listSite.getInterval().equals( site.getInterval() ) );
            if( isDistinct )
                sites.add(site);
        }
    }
}
