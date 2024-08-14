package ru.biosoft.bsa.track.big;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;

public class SiteWrapper implements Site
{
    private Site site;
    private String name;
    
    public SiteWrapper(Site site, String name)
    {
        this.site = site;
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return site.getOrigin();
    }

    @Override
    public String getType()
    {
        return site.getType();
    }

    @Override
    public int getBasis()
    {
        return site.getBasis();
    }

    @Override
    public int getStart()
    {
        return site.getStart();
    }

    @Override
    public int getLength()
    {
        return site.getLength();
    }

    @Override
    public int getFrom()
    {
        return site.getFrom();
    }

    @Override
    public int getTo()
    {
        return site.getTo();
    }

    @Override
    public Interval getInterval()
    {
        return site.getInterval();
    }

    @Override
    public int getPrecision()
    {
        return site.getPrecision();
    }

    @Override
    public int getStrand()
    {
        return site.getStrand();
    }

    @Override
    public Sequence getSequence()
    {
        return site.getSequence();
    }

    @Override
    public Sequence getOriginalSequence()
    {
        return site.getOriginalSequence();
    }

    @Override
    public String getComment()
    {
        return site.getComment();
    }

    @Override
    public double getScore()
    {
        return site.getScore();
    }

    @Override
    public DynamicPropertySet getProperties()
    {
        return site.getProperties();
    }
}
