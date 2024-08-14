package ru.biosoft.bsa;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.IntStreamEx;

/**
 * Represents linear interval on the chromosome or gene. Both ends are considered to be included
 */
public class Interval implements Comparable<Interval>
{
    private final int from, to;
    private static Pattern blockParsingPattern = Pattern.compile("^\\(([-+]?\\d+),([-+]?\\d+)\\)$");
    
    /**
     * Constructs zero-length interval
     */
    public Interval(int from)
    {
        this(from, from-1);
    }

    /**
     * Constructs interval from its ends
     * @param fromInclusive left bound of the interval (inclusive)
     * @param toInclusive right bound of the interval (inclusive)
     */
    public Interval(int fromInclusive, int toInclusive)
    {
        this.from = fromInclusive;
        this.to = toInclusive;
    }
    
    /**
     * Constructs interval from string representation (exactly the same as returned by toString())
     */
    public Interval(String str) throws IllegalArgumentException
    {
        Matcher m = blockParsingPattern.matcher(str);
        if( m.matches() )
        {
            from = Integer.parseInt(m.group(1));
            to = Integer.parseInt(m.group(2));
        } else
            throw new IllegalArgumentException();
    }

    /**
     * Creates new interval with absolute coordinates given that current one had relative coordinates for supplied Site
     */
    public Interval translateFromSite(Site s)
    {
        return s.getStrand() == Site.STRAND_MINUS?
                new Interval(s.getStart()-getTo(), s.getStart()-getFrom()):
                new Interval(s.getStart()+getFrom(), s.getStart()+getTo());
    }
    
    /**
     * Creates new interval with coordinates relative to supplied Site
     */
    public Interval translateToSite(Site s)
    {
        return s.getStrand() == Site.STRAND_MINUS?
                new Interval(s.getStart()-getTo(), s.getStart()-getFrom()):
                new Interval(getFrom()-s.getStart(), getTo()-s.getStart());
    }
     
    /**
     * Tests whether two intervals intersect
     */
    public boolean intersects(Interval interval)
    {
        if( isZeroLength( interval ) ) //Zero-length interval like SNP insertion
        {
            if( isZeroLength( this ) )
                return interval.from == from && interval.to == to;
            else
                return inside( interval.from ) && inside( interval.to );
        }
        return interval.from<=to && interval.to>=from;
    }
    
    public boolean inside(int pos)
    {
        return from<=pos && to>=pos;
    }
    
    /**
     * Returns true if specified interval lies fully inside this interval
     * @param interval
     * @return
     */
    public boolean inside(Interval interval)
    {
        return inside(interval.getFrom()) && inside(interval.getTo());
    }

    /**
     * Intersects two intervals
     * @param interval
     * @return
     */
    public Interval intersect(Interval interval)
    {
        if( isZeroLength( this ) )
        {
            if( intersects( interval ) )
                return this;
            else
                return null;
        }
        if(isZeroLength(interval) )
        {
            if(intersects(interval)) return interval;
            else return null;
        }
        Interval result = new Interval(Math.max(interval.from, from), Math.min(interval.to, to));
        if(result.from > result.to) return null;
        return result;
    }

    /**
     * Union two intervals
     * @param interval
     * @return smallest interval includes both interval
     */
    public Interval union(Interval interval)
    {
        Interval unionInterval = new Interval(Math.min(interval.from, from), Math.max(interval.to, to));

        return unionInterval;
    }

    /**
     * Moves this interval to the specified shift
     * @param shift - distance to move
     * @return moved interval
     */
    public Interval shift(int shift)
    {
        return new Interval( from + shift, to + shift );
    }
    
    /**
     * Moves this interval to fit the specified one
     * @param interval bounds interval
     * @return moved interval
     */
    public Interval fit(Interval interval)
    {
        // Check if no move is necessary
        if(interval.getFrom() <= getFrom() && interval.getTo() >= getTo()) return this;
        if(interval.getFrom() > getFrom())
            return new Interval(interval.getFrom(), Math.min(interval.getTo(), getTo()-getFrom()+interval.getFrom()));
        return new Interval(Math.max(interval.getFrom(), interval.getTo()-(getTo()-getFrom())), interval.getTo());
    }
    
    /**
     * Coverage of this interval by the list of supplied intervals
     * @param intervals - list of intervals in sorted order
     * @return number of positions in this interval that are also in supplied intervals
     */
    public int coverage(List<Interval> intervals)
    {
        int covered = 0;
        int curFrom = getFrom();
        for(Interval i : intervals)
        {
            int intersection = Math.min( getTo(), i.getTo() ) - Math.max( curFrom, i.getFrom() ) + 1;
            if(intersection > 0)
            {
                covered += intersection;
                curFrom = i.getTo() + 1;
                if(curFrom > getTo())
                    break;
            }
        }
        return covered;
    }
    
    /**
     * Returns list of intervals with specified length (the last one might be shorter) covering this interval
     * @param length - length of individual intervals
     * @return
     */
    public List<Interval> splitByStep(final int length)
    {
        if(length >= getLength()) return Collections.singletonList(this);
        return new AbstractList<Interval>()
        {
            @Override
            public Interval get(int index)
            {
                return intersect(new Interval(getFrom()+length*index, getFrom()+length*(index+1)-1));
            }

            @Override
            public int size()
            {
                return (getLength()+length-1)/length;
            }
        };
    }
    
    /**
     * Returns list of max(count,interval.getLength()) intervals covering this interval
     * @param count - maximal number of sub-intervals. If count <= 1 then list containing single this Interval is returned
     * @return
     */
    public List<Interval> split(int count)
    {
        if(count <= 1) return Collections.singletonList(this);
        final int finalCount = Math.min(count, getLength());
        return new AbstractList<Interval>()
        {
            @Override
            public Interval get(int index)
            {
                return new Interval((int)(getFrom()+(long)getLength()*index/finalCount), (int)(getFrom()+(long)getLength()*(index+1)/finalCount-1));
            }

            @Override
            public int size()
            {
                return finalCount;
            }
        };
    }

    /**
     * Remain of intersection two intervals
     * @param intersectedInterval - interval, which intersect
     * @return list of remain of intervals after intersection interval by intersectedInterval
     */
    public List<Interval> remainOfIntersect(Interval intersectedInterval)
    {
        final List<Interval> remainIntervalList = new ArrayList<>();

        if( !intersects( intersectedInterval ) )
        {
            remainIntervalList.add( this );
        }
        else
        {
            final int intersectFrom = intersectedInterval.getFrom();
            final int intersectTo = intersectedInterval.getTo();

            Interval remainInterval;
            if( inside( intersectFrom ) )
            {
                remainInterval = new Interval( from, intersectFrom - 1 );
                remainIntervalList.add( remainInterval );
            }
            if( inside( intersectTo ) )
            {
                remainInterval = new Interval( intersectTo + 1, to );
                remainIntervalList.add( remainInterval );
            }
        }

        return remainIntervalList;
    }

    public Interval grow(int len)
    {
        if( len == 0 )
            return this;
        return new Interval( from - len, to + len );
    }

    /**
     * Returns new interval which is zoomFactor times bigger than current with centerPoint relative location remains unchanged
     * @param centerPoint
     * @param zoomFactor
     * @return
     */
    public Interval zoom(int centerPoint, double zoomFactor)
    {
        return new Interval((int) ( ( from - centerPoint ) * zoomFactor + centerPoint + 0.5 ), (int) ( ( to - centerPoint ) * zoomFactor
                + centerPoint + 0.5 ));
    }
    
    /**
     * Returns new interval which is zoomFactor times bigger than current with center location remains unchanged
     * @param zoomFactor
     * @return
     */
    public Interval zoom(double zoomFactor)
    {
        return zoom(getCenter(), zoomFactor);
    }
    
    /**
     * Returns new interval which has the specified length and centerPoint relative location remains unchanged
     * @param centerPoint
     * @param zoomFactor
     * @return
     */
    public Interval zoomToLength(int centerPoint, int length)
    {
        double zoomFactor = ((double)(length-1))/(getLength()-1);
        int newFrom = (int) ( (from-centerPoint)*zoomFactor+centerPoint+0.5 );
        return new Interval(newFrom, newFrom+length-1);
    }
    
    /**
     * Returns new interval which has the specified length and centerPoint relative location remains unchanged
     * @param centerPoint
     * @param zoomFactor
     * @return
     */
    public Interval zoomToLength(int length)
    {
        int delta = length - getLength();
        int newFrom = getFrom() - delta / 2;
        return new Interval(newFrom, newFrom + length - 1 );
    }
    
    @Override
    public String toString()
    {
        return "("+from+","+to+")";
    }

    public int getFrom()
    {
        return from;
    }

    public int getTo()
    {
        return to;
    }
    
    public int getCenter()
    {
        return (from+to)/2;
    }
    
    public int getLength()
    {
        return to - from + 1;
    }
    
    /**
     * Returns relative position of point within the interval
     * @param point inside the interval
     * @return number from 0 to 1 specifying the point position (0 = from; 1 = to; 0.5 = center and so on)
     */
    public double getPointPos(int point)
    {
        return ((double)point-from)/getLength();
    }
    
    /**
     * Returns relative position of interval within the interval
     * @param interval inside this interval
     * @return number from 0 to 1 specifying the interval position (0 = left bounds coincide; 1 = right bounds coincide; 0.5 = centers coincide and so on)
     * 0 if intervals equal
     * If interval doesn't lie fully inside this interval, then behavior is undetermined
     */
    public double getIntervalPos(Interval interval)
    {
        return interval.equals(this)?0:((double)interval.getFrom()-getFrom())/(getLength()-interval.getLength());
    }

    @Override
    public int compareTo(Interval i)
    {
        int res = Integer.compare( from, i.from );
        return res == 0 ? Integer.compare( to, i.to ) : res;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if(obj == this) return true;
        if(!(obj instanceof Interval)) return false;
        Interval i = (Interval)obj;
        return from == i.from && to == i.to;
    }
    
    @Override
    public int hashCode()
    {
        return from*1987+to;
    }

    /**
     * @return stream of possible positions within this interval
     */
    public IntStreamEx positions()
    {
        return IntStreamEx.rangeClosed( getFrom(), getTo() );
    }

    public static boolean isZeroLength(Interval interval)
    {
        return interval.from - interval.to == 1 && interval.to != Integer.MAX_VALUE;
    }
}