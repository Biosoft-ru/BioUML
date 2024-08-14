package ru.biosoft.bsa;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.view.TrackViewBuilder;

/**
 * Track which represents interval set which covers all sites in source track
 * If source track doesn't contain overlapping sites,
 * then this track will contain the same number of sites in the same positions as source one.
 * Otherwise overlapped sites will be merged.
 * Note that site names, properties and strand info will be lost
 */
public class MergedTrack extends DataElementSupport implements Track
{
    private final Track source;
    
    public MergedTrack(Track source)
    {
        super(source.getName(), source.getOrigin());
        this.source = source;
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        return getIntervalSet(sequence, from, to).size();
    }

    /**
     * Returns nothing as all sites in new track are unnamed
     */
    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        return null;
    }
    
    protected IntervalSet getIntervalSet(String sequence, int from, int to)
    {
        DataCollection<Site> sourceSites = source.getSites(sequence, from, to);
        if(sourceSites == null) return null;
        IntervalSet intSet = new IntervalSet();
        for(Site site: sourceSites)
        {
            if(site.getLength() > 0)
                intSet.add(site.getInterval());
        }
        return intSet;
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        VectorDataCollection<Site> sites = new VectorDataCollection<>("Sites", Site.class, null);
        Sequence seq = SequenceFactory.getSequence(sequence);
        Integer siteNum = 1;
        for(Interval interval: getIntervalSet(sequence, from, to))
        {
            sites.put(new SiteImpl(null, siteNum.toString(), interval.getFrom(), interval.getTo() - interval.getFrom() + 1,
                    Site.STRAND_NOT_APPLICABLE, seq));
            siteNum++;
        }
        return sites;
    }

    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return source.getViewBuilder();
    }
    
    /**
     * Helper class for merging intervals
     */
    public static class IntervalSet implements Iterable<Interval>
    {
        private final SortedSet<Interval> intSet = new TreeSet<>();
        
        public void add(Interval interval)
        {
            SortedSet<Interval> headSet = intSet.headSet(interval);
            if(!headSet.isEmpty())
            {
                Interval last = intSet.headSet(interval).last();
                if( last.getTo() >= interval.getFrom() - 1 )
                {
                    intSet.remove(last);
                    interval = new Interval(last.getFrom(), Math.max(last.getTo(), interval.getTo()));
                }
            }
            Iterator<Interval> iterator = intSet.tailSet(interval).iterator();
            while(iterator.hasNext())
            {
                Interval cur = iterator.next();
                if(cur.getFrom() <= interval.getTo() + 1)
                {
                    iterator.remove();
                    interval = new Interval(interval.getFrom(), Math.max(cur.getTo(), interval.getTo()));
                } else break;
            }
            intSet.add(interval);
        }
        
        @Override
        public String toString()
        {
            StringBuffer str = new StringBuffer();
            for(Interval i: this)
            {
                str.append(str.length()>0?",":"").append(i.toString());
            }
            return str.toString();
        }
        
        @Override
        public Iterator<Interval> iterator()
        {
            return intSet.iterator();
        }
        
        public int size()
        {
            return intSet.size();
        }
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        throw new UnsupportedOperationException("Not supported for this type of track yet");
    }
}
