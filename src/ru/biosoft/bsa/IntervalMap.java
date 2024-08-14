package ru.biosoft.bsa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Map objects to linear intervals with coordinates >= 0
 * The class is not synchronized!
 * @author lan
 */
public class IntervalMap<T>
{
    private final NavigableMap<Integer, List<T>> map = new TreeMap<>();
    
    public IntervalMap()
    {
        map.put(Integer.MIN_VALUE, new ArrayList<T>());
    }
    
    /**
     * Add object to the map
     * @param from - left border of the range where the object resides (including)
     * @param to - right border of the range where the object resides (including)
     * @param object - object to put into map
     */
    public void add(int from, int to, T object)
    {
        if( to < from )
        {
            if( from - to == 1 && to != Integer.MAX_VALUE )
            {
                //TODO: think of better way to add zero-length insertions
                add( to, from, object );
                return;
            }
            else
                throw new IllegalArgumentException( "IntervalMap.add failed: " + from + ">" + to );
        }
        if(!map.containsKey(from))
        {
            map.put(from, new ArrayList<>(map.lowerEntry( from ).getValue()));
        }
        if(!map.containsKey(to+1))
        {
            map.put(to+1, new ArrayList<>(map.floorEntry( to ).getValue()));
        }
        for(List<T> list: map.subMap(from, to+1).values())
        {
            list.add(object);
        }
    }

    /**
     * Get objects overlapping the given point. Complexity is O(log(nIntervals))
     * @param point to check
     * @return Collection of the objects which overlap the given point
     */
    public Collection<T> getIntervals(int point)
    {
        if(point == Integer.MAX_VALUE) point--;
        Entry<Integer, List<T>> floorEntry = map.floorEntry( point );
        if(floorEntry == null) return Collections.emptyList();
        return Collections.unmodifiableCollection(floorEntry.getValue());
    }
    
    /**
     * Get objects overlapping the given interval
     * @param from interval left border
     * @param to interval right border
     * @return Collection of the objects which overlap the given interval
     */
    public Collection<T> getIntervals(int from, int to)
    {
        Set<T> result = Collections.newSetFromMap(new IdentityHashMap<T, Boolean>());
        if( from - to == 1 && to != Integer.MAX_VALUE )
        {
            Entry<Integer, List<T>> floorEntryTo = map.floorEntry( to );
            result.addAll( floorEntryTo.getValue() );
            Entry<Integer, List<T>> floorEntryFrom = map.floorEntry( from );
            if( floorEntryFrom != null )
                result.retainAll( floorEntryFrom.getValue() );
            return result;
        }
        if(to == Integer.MAX_VALUE) to--;
        for(List<T> elements: map.subMap(from+1, to+1).values())
            result.addAll(elements);
        Entry<Integer, List<T>> floorEntry = map.floorEntry( from );
        if(floorEntry != null)
            result.addAll(floorEntry.getValue());
        return result;
    }
}
