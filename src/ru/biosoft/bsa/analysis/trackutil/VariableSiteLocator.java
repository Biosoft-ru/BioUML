package ru.biosoft.bsa.analysis.trackutil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import ru.biosoft.bsa.IntervalMap;
import ru.biosoft.bsa.Site;

public class VariableSiteLocator
{
    private final String[] fieldNames;
    private final int delta;

    private final Map<String, IntervalMap<SiteInfo>> intervalMap = new HashMap<>();

    public VariableSiteLocator(String[] fieldNames, int delta)
    {
        this.fieldNames = fieldNames;
        this.delta = delta;
    }

    public boolean contains(Site s)
    {
        return find( s ) != null;
    }

    public String find(Site s)
    {
        String chrom = s.getOriginalSequence().getName();
        IntervalMap<SiteInfo> intervals = intervalMap.get( chrom );
        if( intervals == null )
            return null;
        int from = s.getFrom();
        int to = s.getTo();
        Collection<SiteInfo> sites = intervals.getIntervals( from, to < from ? from : to );
        for( SiteInfo si : sites )
        {
            if( Math.abs( si.from - from ) <= delta && Math.abs( si.to - to ) <= delta && equalFields( s, si ) )
                return si.name;
        }
        return null;
    }

    public void put(Site s)
    {
        String chrom = s.getOriginalSequence().getName();
        IntervalMap<SiteInfo> intervals = intervalMap.computeIfAbsent( chrom, c -> new IntervalMap<>() );
        int from = s.getFrom();
        int to = s.getTo();
        intervals.add( from, to < from ? from : to, new SiteInfo( fieldNames, s ) );
    }

    public Collection<SiteInfo> getAndMark(Site s)
    {
        String chrom = s.getOriginalSequence().getName();
        IntervalMap<SiteInfo> intervals = intervalMap.get( chrom );
        if( intervals == null )
            return null;
        int from = s.getFrom();
        int to = s.getTo();
        Collection<SiteInfo> sites = intervals.getIntervals( from, to < from ? from : to );
        if( sites.isEmpty() )
            return null;
        List<SiteInfo> result = new ArrayList<>();
        boolean found = false;
        for( SiteInfo si : sites )
        {
            if( Math.abs( si.from - from ) <= delta && Math.abs( si.to - to ) <= delta && equalFields( s, si ) )
            {
                found = true;
                if( si.name != null )
                    result.add( si );
            }
        }
        return found ? result : null;
    }

    private boolean equalFields(Site s, SiteInfo si)
    {
        return StreamEx.zip( fieldNames, si.fields, (name, field) -> s.getProperties().getValue( name ).equals( field ) )
                .allMatch( Boolean.TRUE::equals );
    }
}
