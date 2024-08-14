package ru.biosoft.bsa;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public class DiscontinuousCoordinateSystem
{
    
    private NavigableMap<Integer, Region> regionsByStart = new TreeMap<>();
    private NavigableMap<Integer, Region> regionsByOffset = new TreeMap<>();
    private boolean reverse;
    private int totalLength;
    
    public DiscontinuousCoordinateSystem(Collection<? extends Interval> regions, boolean reverse)
    {
        for(Interval r : regions) {
            if(regionsByStart.containsKey(r.getFrom()))
                throw new IllegalArgumentException("Intervals should not overlap each other in DiscontinuousCoordinateSystem");
            regionsByStart.put(r.getFrom(), new Region(r.getFrom(), r.getTo()));
        }
        
        int offset = 0;
        for(Region r : regionsByStart.values())
        {
            r.setOffset(offset);
            offset += r.getLength();
        }
        totalLength = offset;
        
        for(Region r : regionsByStart.values())
            regionsByOffset.put(r.getOffset(), r);
        
        checkNotOverlaps();
        
        this.reverse = reverse;
    }
    
    private void checkNotOverlaps()
    {
        Iterator<Region> it = regionsByStart.values().iterator();
        if(it.hasNext())
        {
            Interval prev = it.next();
            while(it.hasNext())
            {
                Interval cur = it.next();
                if(prev.intersects(cur))
                    throw new IllegalArgumentException("Intervals should not overlap each other in DiscontinuousCoordinateSystem");
            }
        }
    }
    
    /**
     * Translates coordinate from original coordinate system to this coordinate system.
     * For example, translate genomic coordinate to offset in cDNA
     * This coordinate system is zero based (return zero based offset for cDNA in example)
     */
    public int translateCoordinate(int x)
    {
        Entry<Integer, Region> e = regionsByStart.floorEntry(x);
        if(e == null)
            throw new IndexOutOfBoundsException();
        Region r = e.getValue();
        if(x > r.getTo())
            throw new IndexOutOfBoundsException();
        int forwardPosition = r.getOffset() + ( x - r.getFrom() );
        return reverse ? totalLength - forwardPosition - 1: forwardPosition;
    }
    
    /**
     * Translates coordinate from this coordinate system to the original coordinate system
     * For example, translate from offset in cDNA to absolute genomic coordinate
     */
    public int translateCoordinateBack(int x)
    {
        if(reverse)
            x = totalLength - x - 1;
        Entry<Integer, Region> e = regionsByOffset.floorEntry(x);
        if(e == null)
            throw new IndexOutOfBoundsException();
        Region r = e.getValue();
        if(x >= r.getOffset() + r.getLength())
            throw new IndexOutOfBoundsException();
        return r.getFrom() + ( x - r.getOffset() );
    }
    
    
    public Collection<? extends Interval> getIntervals()
    {
        return Collections.unmodifiableCollection(regionsByStart.values());
    }
    
    public int getLength()
    {
        return totalLength;
    }
    
    public boolean isReverse()
    {
        return reverse;
    }
    
    private static class Region extends Interval
    {
        /**
         * Coordinate of from in discontinuous coordinate system
         */
        private int offset;
        
        public Region(int from, int to)
        {
            super(from, to);
        }
        
        public int getOffset()
        {
            return offset;
        }
        
        public void setOffset(int offset)
        {
            this.offset = offset;
        }
    }
}
