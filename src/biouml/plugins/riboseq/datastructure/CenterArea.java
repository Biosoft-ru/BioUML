package biouml.plugins.riboseq.datastructure;

import biouml.plugins.riboseq.util.SiteUtil;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class CenterArea
{
    public static final int DELTA = 5;

    private static final int LEFT_INTERVAL = 0;
    private static final int RIGHT_INTERVAL = 1;

    private static final int INTERVAL = 0;

    private List<Interval> centerIntervalList;

    public CenterArea(Site site)
    {
        centerIntervalList = new ArrayList<>();

        final Cigar cigar = SiteUtil.getCigar( site );
        if( SiteUtil.isCigarContainsNoperator( cigar ) )
        {
            fillCenterIntervalListWithIntron( site );
        }
        else
        {
            final int centerSite = site.getFrom() + site.getLength() / 2;
            final Interval centerInterval = createCheckingSiteSizeCenterInterval( site, centerSite );

            centerIntervalList.add( centerInterval );
        }
    }

    public CenterArea(List<Interval> exonIntervalList)
    {
        centerIntervalList = exonIntervalList;
    }

    public List<Interval> getIntervalList()
    {
        return centerIntervalList;
    }

    public boolean hasIntron()
    {
        return centerIntervalList.size() > 1;
    }

    private void fillCenterIntervalListWithIntron(Site site)
    {
        final int centerWithIntron = getCenterWithIntron( site );

        final Interval deltaInterval = createDeltaInterval( centerWithIntron );
        final Interval intronInterval = SiteUtil.getIntronInterval( site );

        if( !deltaInterval.intersects( intronInterval ) )
        {
            centerIntervalList.add( deltaInterval );
        }
        else
        {
            // suggest one intersecting
            final List<Interval> halfDeltaList = deltaInterval.split( 2 );
            final Interval firstHalf = halfDeltaList.get( 0 );

            final int leftSideIntron = intronInterval.getFrom();
            final int rightSideIntron = intronInterval.getTo();

            final Interval leftInterval;
            final Interval rightInterval;

            final int distFromCenterToIntron;
            final int remainLengthPart;

            if( firstHalf.intersects( intronInterval ) )
            {
                distFromCenterToIntron = centerWithIntron - rightSideIntron;
                remainLengthPart = DELTA - distFromCenterToIntron;

                leftInterval = new Interval( leftSideIntron - remainLengthPart, leftSideIntron );
                rightInterval = new Interval( rightSideIntron, deltaInterval.getTo() );
            }
            else
            {
                distFromCenterToIntron = leftSideIntron - centerWithIntron;
                remainLengthPart = DELTA - distFromCenterToIntron;

                leftInterval = new Interval( deltaInterval.getFrom(), leftSideIntron );
                rightInterval = new Interval( rightSideIntron, rightSideIntron + remainLengthPart );
            }
            centerIntervalList.add( leftInterval );
            centerIntervalList.add( rightInterval );
        }
    }

    private Interval createCheckingSiteSizeCenterInterval(Site site, int centerSite)
    {
        final int siteLength = site.getLength();
        if( siteLength > 2 * DELTA )
        {
            return createDeltaInterval( centerSite );
        }
        else
        {
            return site.getInterval();
        }
    }

    private Interval createDeltaInterval(int center)
    {
        final int centerFrom = center - DELTA;
        final int centerTo = center + DELTA;

        return new Interval( centerFrom, centerTo );
    }

    public static int getCenterWithIntron(Site site)
    {
        final Cigar cigar = SiteUtil.getCigar( site );
        final List<CigarElement> cigarElements = cigar.getCigarElements();

        int lengthExons = 0;
        for( CigarElement element : cigarElements )
        {
            if( element.getOperator() != CigarOperator.N )
            {
                lengthExons += element.getLength();
            }
        }
        int remainHalfLengthExon = lengthExons / 2;

        int center = site.getFrom();
        for( CigarElement curElement : cigarElements )
        {
            final int curElementLength = curElement.getLength();
            if( curElement.getOperator() == CigarOperator.N )
            {
                center += curElementLength;
            }
            else
            {
                if( curElementLength <= remainHalfLengthExon )
                {
                    remainHalfLengthExon -= curElementLength;
                    center += curElementLength;
                }
                else
                {
                    center += remainHalfLengthExon;
                    break;
                }
            }
        }

        return center;
    }

    public static int compare(Site s1, Site s2)
    {
        final CenterArea centerArea1 = new CenterArea( s1 );
        final CenterArea centerArea2 = new CenterArea( s2 );

        final int x = centerArea1.getLeftMost();
        final int y = centerArea2.getLeftMost();

        return Integer.compare( x, y );
    }

    private int getLeftMost()
    {
        final Interval leftMostInterval = centerIntervalList.get( LEFT_INTERVAL );
        return leftMostInterval.getFrom();
    }

    public boolean intersects(CenterArea centerArea)
    {
        final List<Interval> anotherCIL = centerArea.centerIntervalList;

        final Interval firstInterval = centerIntervalList.get( LEFT_INTERVAL );
        final Interval anotherFirstInterval = anotherCIL.get( LEFT_INTERVAL );

        final int size1 = centerIntervalList.size();
        final int size2 = anotherCIL.size();
        if( size1 == 1 && size2 == 1 )
        {
            return firstInterval.intersects( anotherFirstInterval );
        }
        else
        {
            if( size1 == 2 && size2 == 2 )
            {
                // in any case it will be a intersected, since there is a common intron
                return true;
            }
            else
            {
                Interval interval1 = centerIntervalList.get( LEFT_INTERVAL );
                Interval anotherInterval = anotherCIL.get( INTERVAL );

                Interval interval2;

                if( size2 == 2 )
                {
                    interval2 = anotherCIL.get( RIGHT_INTERVAL );

                    anotherInterval = centerIntervalList.get( INTERVAL );
                    interval1 = anotherCIL.get( LEFT_INTERVAL );
                }
                else
                {
                    interval2 = centerIntervalList.get( RIGHT_INTERVAL );
                }

                if( anotherInterval.intersects( interval1 ) )
                {
                    return anotherInterval.getTo() <= interval1.getTo();
                }
                else if( anotherInterval.intersects( interval2 ) )
                {
                    return anotherInterval.getFrom() >= interval2.getFrom();
                }
                else
                {
                    return false;
                }
            }
        }
    }

    public void union(CenterArea siteCenterArea)
    {
        final List<Interval> siteIntervals = siteCenterArea.centerIntervalList;

        if( centerIntervalList.size() == 2 )
        {
            Interval leftClusterInterval = centerIntervalList.get( LEFT_INTERVAL );
            Interval rightClusterInterval = centerIntervalList.get( RIGHT_INTERVAL );
            if( siteIntervals.size() == 2 )
            {
                final Interval leftSiteInterval = siteIntervals.get( LEFT_INTERVAL );
                final Interval rightSiteInterval = siteIntervals.get( RIGHT_INTERVAL );

                leftClusterInterval = leftClusterInterval.union( leftSiteInterval );
                rightClusterInterval = rightClusterInterval.union( rightSiteInterval );

                centerIntervalList.set( LEFT_INTERVAL, leftClusterInterval );
                centerIntervalList.set( RIGHT_INTERVAL, rightClusterInterval );
            }
            else
            {
                final Interval siteInterval = siteIntervals.get( INTERVAL );

                if( siteInterval.intersects( leftClusterInterval ) )
                {
                    leftClusterInterval = leftClusterInterval.union( siteInterval );
                    centerIntervalList.set( LEFT_INTERVAL, leftClusterInterval );
                }
                else
                {
                    rightClusterInterval = rightClusterInterval.union( siteInterval );
                    centerIntervalList.set( RIGHT_INTERVAL, rightClusterInterval );
                }
            }
        }
        else
        {
            Interval clusterInterval = centerIntervalList.get( INTERVAL );

            if( siteIntervals.size() == 2 )
            {
                Interval leftSiteInterval = siteIntervals.get( LEFT_INTERVAL );
                Interval rightSiteInterval = siteIntervals.get( RIGHT_INTERVAL );

                if( clusterInterval.intersects( leftSiteInterval ) )
                {
                    leftSiteInterval = leftSiteInterval.union( clusterInterval );
                }
                else
                {
                    rightSiteInterval = rightSiteInterval.union( clusterInterval );
                }

                centerIntervalList.set( LEFT_INTERVAL, leftSiteInterval );
                centerIntervalList.add( rightSiteInterval );
            }
            else
            {
                final Interval siteInterval = siteIntervals.get( INTERVAL );
                clusterInterval = clusterInterval.union( siteInterval );

                centerIntervalList.set( INTERVAL, clusterInterval );
            }
        }
    }

    public boolean inside(CenterArea anotherArea)
    {
        final ListIterator<Interval> exonIterator = centerIntervalList.listIterator();
        final int anotherAreaLeftMost = anotherArea.getLeftMost();

        while( exonIterator.hasNext() )
        {
            final Interval exonInterval = exonIterator.next();
            if( exonInterval.inside( anotherAreaLeftMost ) ) {
                exonIterator.previous();
                break;
            }
        }

        if( !exonIterator.hasNext() )
        {
            return false;
        }

        final Iterator<Interval> anotherExonIterator = anotherArea.getIntervalList().iterator();
        while( anotherExonIterator.hasNext() && exonIterator.hasNext() )
        {
            final Interval anotherExonInterval = anotherExonIterator.next();
            final Interval exonInterval = exonIterator.next();

            if( !exonInterval.inside( anotherExonInterval ) )
            {
                return false;
            }
        }

        return exonIterator.hasNext();
    }
}
