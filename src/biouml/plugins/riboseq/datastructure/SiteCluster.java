package biouml.plugins.riboseq.datastructure;

import biouml.plugins.riboseq.util.SiteUtil;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.util.StringUtil;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

import static java.lang.Character.isDigit;

public class SiteCluster
{
    private static final Interval notIntron = new Interval( 0, 0 );

    private Interval clusterInterval;
    private final boolean reversed;

    private final List<Site> sites;

    private boolean computeSequence;
    private String sequence;

    private ClusterInfo info;

    public SiteCluster(Site site)
    {
        clusterInterval = site.getInterval();
        reversed = SiteUtil.isSiteReversed( site );

        computeSequence = false;

        sites = new LinkedList<>();
        sites.add( site );
    }

    public void addSite(Site site)
    {
        Interval siteInterval = site.getInterval();
        clusterInterval = clusterInterval.union( siteInterval );

        computeSequence = false;

        sites.add( site );
    }

    public boolean isIntersects(Site site)
    {
        Interval siteInterval = site.getInterval();
        return clusterInterval.intersects( siteInterval );
    }

    public Interval getInterval()
    {
        return clusterInterval;
    }

    public boolean isReversed()
    {
        return reversed;
    }

    public int getNumberOfSites()
    {
        return sites.size();
    }

    public String getSequenceAverageString(Sequence originChr)
    {
        final char[] alfChar = new char[] {'A', 'C', 'G', 'T'};
        final String CIGAR = "Cigar";

        if( !computeSequence )
        {
            int clusterLength = clusterInterval.getLength();
            int clusterFrom = clusterInterval.getFrom();

            // TODO: why flag reversed don't work?
            String originChrClusterRegion = new SequenceRegion( originChr, clusterInterval, false, false ).toString();
            int[][] alfCounters = new int[clusterLength][4];

            for( Site site : sites )
            {
                final String siteSeq = (String)site.getProperties().getProperty( BAMTrack.READ_SEQUENCE ).getValue();
                final String cigarStr = (String)site.getProperties().getValue( CIGAR );
                final Cigar cigar = decodeCigar( cigarStr );

                int from = site.getFrom();
                int shiftSiteFromStartCluster = from - clusterFrom;

                int offsetSite = 0;
                int offsetOrigin = 0;

                for( CigarElement element : cigar.getCigarElements() )
                {
                    CigarOperator operator = element.getOperator();
                    switch( operator )
                    {
                        case M:
                            for( int i = 0; i < element.getLength(); i++ )
                            {
                                int siteInd = offsetSite + i;
                                int globInd = shiftSiteFromStartCluster + offsetOrigin + i;
                                char siteByte = Character.toUpperCase( siteSeq.charAt( siteInd ) );
                                for( int j = 0; j < 4; j++ )
                                {
                                    if( siteByte == alfChar[j] )
                                    {
                                        alfCounters[globInd][j]++;
                                        break;
                                    }
                                }
                            }

                            break;
                        case I:
                        case D:
                        case N:
                            break;
                        default:
                            // TODO: choose class
                            throw new RuntimeException( "cigar operator" );
                    }

                    if( operator.consumesReadBases() )
                    {
                        offsetSite += element.getLength();
                    }
                    if( operator.consumesReferenceBases() )
                    {
                        offsetOrigin += element.getLength();
                    }
                }
            }


            StringBuilder stringBuilder = new StringBuilder( clusterLength );
            for( int i = 0; i < clusterLength; i++ )
            {
                boolean uncertainty = false;
                int maxInd = 0;
                for( int j = 1; j < 4; j++ )
                {
                    if( alfCounters[i][maxInd] < alfCounters[i][j] )
                    {
                        maxInd = j;
                        uncertainty = false;
                    }
                    else if( alfCounters[i][maxInd] == alfCounters[i][j] )
                    {
                        uncertainty = true;
                    }
                }

                if( !uncertainty )
                {
                    stringBuilder.append( alfChar[maxInd] );
                }
                else
                {
                    stringBuilder.append( Character.toUpperCase( originChrClusterRegion.charAt( i ) ) );
                }
            }

            sequence = stringBuilder.toString();

            computeSequence = true;
        }

        return sequence;
    }

    public void calculateInfo(Sequence chrSequence)
    {
        info = new ClusterInfo();

        fillModasHistogram();
        info.shiftStartCodon = findStartCodonInChromosome( chrSequence );

        info.initCodonPosition = computeInitCodonPosition( info.startModa );
    }

    private Cigar decodeCigar(String cigarStr)
    {
        final byte ZERO_BYTE = (byte)'0';

        if( SAMRecord.NO_ALIGNMENT_CIGAR.equals( cigarStr ) )
        {
            return new Cigar();
        }

        final Cigar resultCigar = new Cigar();
        final byte[] cigarBytes = StringUtil.stringToBytes( cigarStr );
        for( int i = 0; i < cigarBytes.length; ++i )
        {
            int length = ( cigarBytes[i] - ZERO_BYTE );
            for( ++i; isDigit( cigarBytes[i] ); ++i )
            {
                length = ( length * 10 ) + cigarBytes[i] - ZERO_BYTE;
            }

            final CigarOperator operator = CigarOperator.characterToEnum( cigarBytes[i] );
            final CigarElement cigarElement = new CigarElement( length, operator );

            resultCigar.add( cigarElement );
        }

        return resultCigar;
    }

    protected int computeInitCodonPosition(int absoluteStartModa)
    {
        int initCodonPosition;
        int averagedSumMiddleSites = 0;

        final List<Site> startModaSiteList = getStartModaSiteList( absoluteStartModa );
        for( Site site : startModaSiteList )
        {
            averagedSumMiddleSites += site.getLength();
        }

        final int avarageMiddleSiteLength = averagedSumMiddleSites / startModaSiteList.size();
        final int halfLength = avarageMiddleSiteLength / 2;

        if( reversed )
        {
            initCodonPosition = absoluteStartModa - halfLength;
        }
        else
        {
            initCodonPosition = absoluteStartModa + halfLength;
        }

        return initCodonPosition;
    }

    protected List<Site> getStartModaSiteList(int absoluteStartModa)
    {
        final List<Site> siteList = new ArrayList<>();

        for( Site site : sites )
        {
            final int startShift = site.getStart();
            if( startShift == absoluteStartModa )
            {
                siteList.add( site );
            }
        }

        return siteList;
    }

    public ClusterInfo getInfo()
    {
        return info;
    }

    private void fillModasHistogram()
    {
        Map<Integer, Integer> shiftsMap = new HashMap<>();

        for( Site site : sites )
        {
            int clusterStart = reversed ? clusterInterval.getTo() : clusterInterval.getFrom();
            int startShift = Math.abs( site.getStart() - clusterStart );

            if( shiftsMap.containsKey( startShift ) )
            {
                Integer counter = shiftsMap.get( startShift );
                shiftsMap.put( startShift, counter + 1 );
            }
            else
            {
                shiftsMap.put( startShift, 1 );
            }
        }

        int shiftCenterModa = 0;
        int modaCenterScore = Integer.MIN_VALUE;
        for( Map.Entry<Integer, Integer> entry : shiftsMap.entrySet() )
        {
            int score = entry.getValue();
            if( score > modaCenterScore )
            {
                modaCenterScore = score;
                shiftCenterModa = entry.getKey();
            }
        }

        info.startModa = reversed ? clusterInterval.getTo() - shiftCenterModa : clusterInterval.getFrom() + shiftCenterModa;

        final int deltaHistogram = 5;
        final int centerModasIndex = deltaHistogram;
        final int modasSize = 2 * deltaHistogram + 1;

        int[] modasHistogram = new int[modasSize];

        modasHistogram[centerModasIndex] = modaCenterScore;
        for( Map.Entry<Integer, Integer> entry : shiftsMap.entrySet() )
        {
            int shiftModa = entry.getKey();
            int delta = shiftModa - shiftCenterModa;
            if( Math.abs( delta ) <= deltaHistogram )
            {
                modasHistogram[centerModasIndex + delta] = entry.getValue();
            }
        }

        info.modasHistogram = modasHistogram;
    }

    private int findStartCodonInChromosome(Sequence chr)
    {
        final String directStartCodon = "ATG";
        final String complementStartCodon = "TAC";

        String startCodonPattern = directStartCodon;

        // TODO: why flag reversed don't work?
        int start = clusterInterval.getFrom();
        SequenceRegion clusterRegion = new SequenceRegion( chr, start, clusterInterval.getLength(), false, false );

        if( reversed )
        {
            startCodonPattern = complementStartCodon;
            clusterRegion = SequenceRegion.getReversedSequence( clusterRegion );
        }

        String chrClusterIntervalString = clusterRegion.toString().toUpperCase();

        return chrClusterIntervalString.indexOf( startCodonPattern );
    }

    public List<SiteCluster> trySplitCluster()
    {
        final List<SiteCluster> dividedClusters = new ArrayList<>();

        final Map<Interval, List<Site>> intronSiteListMap = getSiteMapByIntron();
        final List<Site> siteListWithoutIntrons = getSortedSitesWithoutIntrons( intronSiteListMap );

        for( Map.Entry<Interval, List<Site>> intervalListEntry : intronSiteListMap.entrySet() )
        {
            final List<Site> intronSiteList = intervalListEntry.getValue();
            sort( intronSiteList );

            final List<IntronSiteCluster> splitClusters = splitClusters( intronSiteList );
            addingSitesWithoutIntronToClusters( splitClusters, siteListWithoutIntrons );
            dividedClusters.addAll( splitClusters );
        }

        final List<IntronSiteCluster> siteClustersWithoutIntrons = splitClusters(siteListWithoutIntrons);
        dividedClusters.addAll( siteClustersWithoutIntrons );

        return dividedClusters;
    }

    private List<Site> getSortedSitesWithoutIntrons(Map<Interval, List<Site>> intronSiteListMap)
    {
        final List<Site> siteList;
        if( intronSiteListMap.containsKey( notIntron ) )
        {
            siteList = intronSiteListMap.remove( notIntron );
            Collections.sort( siteList, Comparator.comparingInt( Site::getFrom ) );
        }
        else
        {
            siteList = new ArrayList<>();
        }

        return siteList;
    }

    private void addingSitesWithoutIntronToClusters(List<IntronSiteCluster> clusters, List<Site> siteList)
    {
        for( IntronSiteCluster cluster : clusters )
        {
            final Iterator<Site> siteIterator = siteList.iterator();
            while( siteIterator.hasNext() )
            {
                final Site site = siteIterator.next();
                if( cluster.isIntersects( site ) )
                {
                    cluster.addSite( site );
                    siteIterator.remove();
                }
            }
        }
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

    private List<IntronSiteCluster> splitClusters(List<Site> siteList)
    {
        if( siteList.isEmpty() )
        {
            return new ArrayList<>();
        }

        List<IntronSiteCluster> intronSiteClusters = new ArrayList<>();
        IntronSiteCluster curCluster = new IntronSiteCluster( siteList.get( 0 ) );
        for( int i = 1; i < siteList.size(); i++ )
        {
            Site site = siteList.get( i );
            if( curCluster.isIntersects( site ) )
            {
                curCluster.addSite( site );
            }
            else
            {
                intronSiteClusters.add( curCluster );
                curCluster = new IntronSiteCluster( site );
            }
        }
        intronSiteClusters.add( curCluster );

        return intronSiteClusters;
    }

    private void sort(List<Site> siteList)
    {
        Collections.sort( siteList, CenterArea::compare );
    }

    private Map<Interval, List<Site>> getSiteMapByIntron()
    {
        return StreamEx.of( sites ).groupingBy( this::getIntronInterval );
    }
    
    private Interval getIntronInterval(Site site)
    {
        final Cigar cigar = SiteUtil.getCigar( site );
        return SiteUtil.isCigarContainsNoperator( cigar ) ? SiteUtil.getIntronInterval( site ) : notIntron;
    }

    public List<Interval> getExons() {
        return Collections.singletonList( getInterval() );
    }
}
