package ru.biosoft.bsa;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChrIntervalMap<T>
{
    private Map<String, IntervalMap<T>> intervalsByChr = new HashMap<>();
    
    public void add(String chr, int from, int to, T value)
    {
        IntervalMap<T> chrIntervals = intervalsByChr.get( chr );
        if(chrIntervals == null)
            intervalsByChr.put( chr, chrIntervals = new IntervalMap<>() );
        chrIntervals.add( from, to, value );
    }
    
    public Collection<T> getIntervals(String chr, int from, int to)
    {
        IntervalMap<T> chrIntervals = intervalsByChr.get( chr );
        if(chrIntervals == null)
            return Collections.emptyList();
        return chrIntervals.getIntervals( from, to );
    }
    
    public Collection<T> getIntervals(String chr, int point)
    {
        IntervalMap<T> chrIntervals = intervalsByChr.get( chr );
        if(chrIntervals == null)
            return Collections.emptyList();
        return chrIntervals.getIntervals( point );
    }
}
