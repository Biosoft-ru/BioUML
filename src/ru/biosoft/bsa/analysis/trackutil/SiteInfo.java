package ru.biosoft.bsa.analysis.trackutil;

import one.util.streamex.StreamEx;
import ru.biosoft.bsa.Site;

public class SiteInfo
{
    public String name;
    public int from, to;
    public Object[] fields;
    boolean added = false;

    public SiteInfo(String[] fieldNames, Site s)
    {
        name = s.getName();
        from = s.getFrom();
        to = s.getTo();
        fields = StreamEx.of( fieldNames ).map( name -> s.getProperties().getValue( name ) ).toArray();
    }
    public boolean isAdded()
    {
        return this.added;
    }
    public void setAdded(boolean added)
    {
        this.added = added;
    }
}
