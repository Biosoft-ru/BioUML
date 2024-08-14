package ru.biosoft.bsa;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;

/**
 * @author lan
 *
 */
public abstract class SlicedTrack extends DataElementSupport implements Track
{
    private final Map<String, Reference<SliceSet>> sliceSets = new HashMap<>();
    protected int sliceLength;

    protected SlicedTrack(String name, DataCollection<?> origin, int sliceLength)
    {
        super(name, origin);
        this.sliceLength = sliceLength;
    }
    
    public int getSliceLength()
    {
        return sliceLength;
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        SubSequence seq = new SubSequence(sequence, from, to);
        return new SlicedSitesCollection(seq);
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        return getSites(sequence, from, to).getSize();
    }

    protected Site doGetSite(String sequence, String siteName, Interval interval) throws Exception
    {
        throw new Exception("Unsupported method 'getSite' for class: " + getClass().getName());
    }

    /**
     * @param sequence sequence name
     * @param interval
     * @return Collection of Sites lying on the supplied sequence in the supplied interval
     * Must return sites in the same order for the same input
     * @throws Exception
     */
    protected abstract Collection<Site> loadSlice(String sequence, Interval interval) throws Exception;

    /**
     * @param sequence sequence name
     * @param interval
     * @param limit
     * @return number of sites lying in the supplied interval of the supplied sequence
     * if number of sites equals to or greater than limit, then it's allowed to return limit or any number higher
     * @throws Exception
     */
    protected abstract int countSitesLimited(String sequence, Interval interval, int limit) throws Exception;

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        SubSequence seq = new SubSequence(sequence, from, to);
        sequence = seq.getCompletePath().toString();
        from = seq.getFrom();
        to = seq.getTo();
        Site s = doGetSite(sequence, siteName, new Interval(from, to));
        if(s != null) s = seq.translateSite(s);
        return s;
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        throw new UnsupportedOperationException("Operation is not supported for this type of track");
    }

    protected TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    private SliceSet getSliceSet(String sequence)
    {
        Reference<SliceSet> reference = sliceSets.get(sequence);
        SliceSet set = reference == null ? null : reference.get();
        if(set == null)
        {
            synchronized(sliceSets)
            {
                reference = sliceSets.get(sequence);
                set = reference == null ? null : reference.get();
                if(set == null)
                {
                    set = new SliceSet(sequence);
                    sliceSets.put( sequence, new SoftReference<>( set ) );
                }
            }
        }
        return set;
    }

    private static class Slice
    {
        // sites started in the slice
        private final Site[] startedSites;
        // sites not started in the slice
        private final Site[] notStartedSites;
        
        public Slice(Interval slice, Collection<Site> sites)
        {
            Map<Boolean, Site[]> byStart = StreamEx.of( sites ).partitioningBy( site -> slice.inside( site.getFrom() ),
                    MoreCollectors.toArray( Site[]::new ) );
            this.startedSites = byStart.get( true );
            this.notStartedSites = byStart.get( false );
        }

        public Site[] getStartedSites()
        {
            return startedSites;
        }

        public Site[] getNotStartedSites()
        {
            return notStartedSites;
        }
    }

    /**
     * Contains all existing slices for given sequence
     * @author lan
     */
    private class SliceSet
    {
        private final List<Reference<Slice>> slices = new ArrayList<>();
        private final String sequence;
        
        public SliceSet(String sequence)
        {
            this.sequence = sequence;
        }
        
        /**
         * Returns slice if it's loaded; null otherwise
         * @param n slice number
         * @return
         */
        public Slice getLoadedSlice(int n)
        {
            if(slices.size() <= n) return null;
            Reference<Slice> reference = slices.get(n);
            if(reference == null) return null;
            return reference.get();
        }
        
        /**
         * Returns slice with given number; load it if it's not loaded
         * @param n slice number
         * @return
         * @throws Exception if slice loading threw an Exception
         */
        public Slice getSlice(int n) throws Exception
        {
            if(slices.size() <= n)
            {
                synchronized(slices)
                {
                    while(slices.size() <= n) slices.add(null);
                }
            }
            Reference<Slice> reference = slices.get(n);
            Slice slice = reference == null?null:reference.get();
            if(slice == null)
            {
                synchronized(slices)
                {
                    reference = slices.get(n);
                    slice = reference == null?null:reference.get();
                    if(slice == null)
                    {
                        Interval interval = new Interval(n*sliceLength, (n+1)*sliceLength-1);
                        try
                        {
                            slice = new Slice(interval, loadSlice(sequence, interval));
                        }
                        catch( Exception e )
                        {
                            throw new Exception("Unable to load slice: "+sequence+interval, e);
                        }
                        slices.set( n, new SoftReference<>( slice ) );
                    }
                }
            }
            return slice;
        }
    }
    
    public class SlicedSitesCollection extends AbstractDataCollection<Site> implements LimitedSizeSitesCollection
    {
        final SubSequence seq;
        final SliceSet set;
        final int startSlice;
        final int endSlice;
        final Interval totalInterval;
        TObjectLongMap<String> sitePositions;
        List<String> nameList;
        int size = -1;
        
        public SlicedSitesCollection(SubSequence seq)
        {
            super("Sites", null, null);
            this.seq = seq;
            this.startSlice = seq.getFrom()/sliceLength;
            this.endSlice = seq.getTo()/sliceLength;
            this.totalInterval = new Interval(this.startSlice*sliceLength, (this.endSlice+1)*sliceLength-1);
            this.set = getSliceSet(seq.getCompletePath().toString());
        }

        @Override
        public @Nonnull Iterator<Site> iterator()
        {
            return new Iterator<Site>()
            {
                // startSlice-2 = start
                // startSlice-1 = sites started before startSlice
                // startSlice..endSlice = sites started in current slice
                // endSlice+1 = end
                private int slice = startSlice-2;
                private Site[] currentSites;
                private int slicePos;
                private Site nextSite;
                
                {
                    try
                    {
                        advance();
                    }
                    catch( Exception e )
                    {
                    }
                }
                
                private void advance() throws Exception
                {
                    nextSite = null;
                    while(true)
                    {
                        if(currentSites == null || slicePos >= currentSites.length)
                        {
                            slice++;
                            if(slice > endSlice) return;
                            if(slice == startSlice-1) currentSites = set.getSlice(startSlice).getNotStartedSites();
                            else currentSites = set.getSlice(slice).getStartedSites();
                            slicePos = 0;
                            continue;
                        }
                        Site site = currentSites[slicePos++];
                        if((slice > startSlice && slice < endSlice) || seq.getInterval().intersects(site.getInterval()))
                        {
                            nextSite = site;
                            break;
                        }
                    }
                }

                @Override
                public boolean hasNext()
                {
                    return nextSite != null;
                }

                @Override
                public Site next()
                {
                    Site site = nextSite;
                    if(site == null) throw new NoSuchElementException();
                    try
                    {
                        advance();
                    }
                    catch( Exception e )
                    {
                    }
                    return seq.translateSite(site);
                }
            };
        }

        @Override
        public int getSizeLimited(int limit)
        {
            if(size != -1) return size;
            if(sitePositions != null)
            {
                size = sitePositions.size();
                return size;
            }
            // first try looking through loaded slices
            // second either load not loaded slices (at most 2) or count directly
            int partialSize = 0;
            TIntSet notLoadedSlices = new TIntHashSet();
            for(int i=startSlice; i<=endSlice; i++)
            {
                Slice slice = set.getLoadedSlice(i);
                if(slice == null)
                {
                    notLoadedSlices.add(i);
                    continue;
                }
                partialSize += countSites(i, slice);
                if(partialSize >= limit) return partialSize;
            }
            if(notLoadedSlices.size() > 2)
            {
                try
                {
                    partialSize = countSitesLimited(seq.getCompletePath().toString(), seq.getInterval(), limit);
                    if(partialSize < limit) size = partialSize;
                    return partialSize;
                }
                catch( Exception e )
                {
                    return 0;
                }
            }
            TIntIterator iterator = notLoadedSlices.iterator();
            while(iterator.hasNext())
            {
                int i = iterator.next();
                try
                {
                    Slice slice = set.getSlice(i);
                    partialSize += countSites(i, slice);
                    if(partialSize >= limit) return partialSize;
                }
                catch( Exception e )
                {
                    return 0;
                }
            }
            // total count reached: save it
            size = partialSize;
            return size;
        }

        @Override
        public int getSize()
        {
            return getSizeLimited(Integer.MAX_VALUE);
        }

        @Override
        public Site doGet(String name) throws Exception
        {
            initSitePositions();
            if(!sitePositions.containsKey(name)) return null;
            long pos = sitePositions.get(name);
            int slice = (int) ( pos >> 32 );
            int posInSlice = (int) ( pos );
            Site[] sites = slice < startSlice ? set.getSlice(startSlice).getNotStartedSites():set.getSlice(slice).getStartedSites();
            return seq.translateSite(sites[posInSlice]);
        }

        @Override
        public @Nonnull List<String> getNameList()
        {
            initSitePositions();
            if(nameList == null)
            {
                nameList = new ArrayList<>( sitePositions.keySet() );
                Collections.sort(nameList);
            }
            return nameList;
        }

        /**
         * Counts sites in the slice which belong to this collection
         * @param sliceNum
         * @param slice
         * @return
         */
        private int countSites(int sliceNum, Slice slice)
        {
            int count = 0;
            if(sliceNum == startSlice)
            {
                // Count sites started before our slices
                for(Site site: slice.getNotStartedSites())
                {
                    if(seq.getInterval().intersects(site.getInterval())) count++;
                }
            }
            if(sliceNum == startSlice || sliceNum == endSlice)
            {
                // Handle specially as they might be located outside of current interval
                for(Site site: slice.getStartedSites())
                {
                    if(seq.getInterval().intersects(new Interval(site.getFrom(), site.getTo()))) count++;
                }
            } else
            {
                count+=slice.getStartedSites().length;
            }
            return count;
        }
        
        /**
         * sitePositions map site names to their from positions in returned slices
         * This is used for get() and getNameList() methods
         */
        private void initSitePositions()
        {
            if(sitePositions != null) return;
            sitePositions = new TObjectLongHashMap<>();
            for(int i=startSlice; i<=endSlice; i++)
            {
                try
                {
                    Slice slice = set.getSlice(i);
                    
                    Site[] sites = slice.getStartedSites();
                    for(int j=0; j<sites.length; j++)
                    {
                        if(seq.getInterval().intersects(sites[j].getInterval()))
                            sitePositions.put(sites[j].getName(), (((long)i)<<32)+j);
                    }
                    if(i == startSlice)
                    {
                        sites = slice.getNotStartedSites();
                        for(int j=0; j<sites.length; j++)
                        {
                            if(seq.getInterval().intersects(sites[j].getInterval()))
                                sitePositions.put(sites[j].getName(), (((long)i-1)<<32)+j);
                        }
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, SlicedTrack.this+": "+e.getMessage(), e);
                }
            }
        }
    }
}
