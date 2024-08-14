package biouml.plugins.riboseq.datastructure;

import biouml.plugins.riboseq.util.SiteUtil;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ChromosomeClusters
{
    private List<SiteCluster> clusters;

    private final DataCollection<Site> siteDC;
    private List<Site> siteList;

    private final Sequence chrSequence;
    /*how many sites can be minimum in the cluster*/
    private final int minNumberSites;
    private final int maxLengthCluster;
    private boolean emptyChrClusters;

    public ChromosomeClusters(DataCollection<Site> sites, Sequence chr, int minNumberSites, int maxLengthCluster)
    {
        siteDC = sites;
        chrSequence = chr;

        clusters = new ArrayList<>();
        emptyChrClusters = true;

        this.minNumberSites = minNumberSites;
        this.maxLengthCluster = maxLengthCluster;
    }

    public void runClustering()
    {
        readSitesToArray();

        clustering();

        if( !emptyChrClusters )
        {
            calculateClustersInfo();
        }
    }

    public int sizeClusters()
    {
        return clusters.size();
    }

    public SiteCluster getCluster(int index)
    {
        return clusters.get( index );
    }

    public Sequence getChromosome()
    {
        return chrSequence;
    }

    public boolean empty()
    {
        return emptyChrClusters;
    }

    public void filterClusters()
    {
        List<SiteCluster> filteredClusters = new ArrayList<>();

        for( SiteCluster cluster : clusters )
        {
            final int numberSites = cluster.getNumberOfSites();
            final int clusterLength = cluster.getInterval().getLength();
            if( numberSites >= minNumberSites )
            {
                if( maxLengthCluster == 0 || clusterLength <= maxLengthCluster )
                {
                    filteredClusters.add( cluster );
                }
            }
        }

        clusters = filteredClusters;
    }

    private void clustering()
    {
        List<Site> siteDirectedArrayList = new ArrayList<>();
        List<Site> siteReversedArrayList = new ArrayList<>();

        splitSitesByStrand( siteDirectedArrayList, siteReversedArrayList );

        if( ! ( siteDirectedArrayList.isEmpty() && siteReversedArrayList.isEmpty() ) )
        {
            emptyChrClusters = false;

            siteDirectedArrayList = sortArraySitesByFromInterval( siteDirectedArrayList );
            siteReversedArrayList = sortArraySitesByFromInterval( siteReversedArrayList );

            final List<SiteCluster> regionClusters = computeClustersFromSites( siteDirectedArrayList );
            regionClusters.addAll( computeClustersFromSites( siteReversedArrayList ) );

            clusters = trySplitClusters( regionClusters );
        }
    }

    private void splitSitesByStrand(List<Site> siteDirectedList, List<Site> siteReversedList)
    {
        for( Site site : siteList )
        {
            if( SiteUtil.isSiteReversed( site ) )
            {
                siteReversedList.add( site );
            }
            else
            {
                siteDirectedList.add( site );
            }
        }
    }

    private List<SiteCluster> trySplitClusters(List<SiteCluster> regionClusters)
    {
        final List<SiteCluster> dividedClusters = new ArrayList<>();

        for( SiteCluster regionCluster : regionClusters )
        {
            final List<SiteCluster> splitCluster = regionCluster.trySplitCluster();
            dividedClusters.addAll( splitCluster );
        }

        return dividedClusters;
    }

    private void readSitesToArray()
    {
        siteList = siteDC.stream().collect( Collectors.toList() );
    }

    private List<Site> sortArraySitesByFromInterval(List<Site> siteArrayList)
    {
        siteArrayList.sort( Comparator.comparingInt( Site::getFrom ) );
        return siteArrayList;
    }

    private List<SiteCluster> computeClustersFromSites(List<Site> siteArrayList)
    {
        if( siteArrayList.isEmpty() )
        {
            return new ArrayList<>();
        }

        List<SiteCluster> siteClusters = new LinkedList<>();
        SiteCluster curCluster = new SiteCluster( siteArrayList.get( 0 ) );
        for( int i = 1; i < siteArrayList.size(); i++ )
        {
            Site site = siteArrayList.get( i );
            if( curCluster.isIntersects( site ) )
            {
                curCluster.addSite( site );
            }
            else
            {
                siteClusters.add( curCluster );
                curCluster = new SiteCluster( site );
            }
        }
        siteClusters.add( curCluster );

        return siteClusters;
    }

    private void calculateClustersInfo()
    {
        for( SiteCluster cluster : clusters )
        {
            cluster.calculateInfo( chrSequence );
        }
    }
}
