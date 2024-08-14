package ru.biosoft.bsa.project;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;

import com.developmentontheedge.beans.Option;

/**
 * Description of the sequence part
 */
@SuppressWarnings ( "serial" )
public class Region extends Option implements Cloneable
{
    /**
     * Target sequence
     */
    protected Sequence sequence;
    /**
     * Name of the sequence
     */
    protected String sequenceName;
    /**
     * Title of the region
     */
    protected String title;
    /**
     * Position
     */
    protected Interval interval;
    /**
     * Preferred position in project
     */
    protected int order;
    /**
     * Indicates if region should be visible
     */
    protected boolean visible;
    /**
     * Region description
     */
    protected String description;
    /**
     * Strand
     */
    protected int strand;
    
    public Region(AnnotatedSequence map)
    {
        this(map.getSequence(), map.getCompletePath().toString());
    }

    public Region(Sequence sequence, String sequenceName)
    {
        this.sequence = sequence;
        this.sequenceName = sequenceName;
        this.title = sequence.getName();
        this.interval = sequence.getInterval();
        this.visible = true;
    }

    public Sequence getSequence()
    {
        return sequence;
    }

    public void setSequence(Sequence sequence)
    {
        Object oldValue = this.sequence;
        this.sequence = sequence;
        firePropertyChange("sequence", oldValue, sequence);
    }
    
    public DataElementPath getSequencePath()
    {
        return DataElementPath.create(sequenceName);
    }

    public String getSequenceName()
    {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName)
    {
        Object oldValue = this.sequenceName;
        this.sequenceName = sequenceName;
        firePropertyChange("sequenceName", oldValue, sequenceName);
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        Object oldValue = this.title;
        this.title = title;
        firePropertyChange("title", oldValue, title);
    }

    public int getFrom()
    {
        return getInterval().getFrom();
    }

    public void setFrom(int from)
    {
        setInterval(new Interval(from, getInterval().getTo()));
    }

    public int getTo()
    {
        return getInterval().getTo();
    }

    public void setTo(int to)
    {
        setInterval(new Interval(getInterval().getFrom(), to));
    }
    
    public Interval getInterval()
    {
        return interval;
    }
    
    public void setInterval(Interval interval)
    {
        Interval oldValue = this.interval;
        if(interval.getLength() < 100) interval = interval.zoomToLength(100);
        this.interval = interval.fit(sequence.getInterval());
        firePropertyChange("interval", oldValue, this.interval);
    }

    public int getOrder()
    {
        return order;
    }

    public void setOrder(int order)
    {
        int oldValue = this.order;
        this.order = order;
        firePropertyChange("to", oldValue, order);
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        boolean oldValue = this.visible;
        this.visible = visible;
        firePropertyChange("visible", oldValue, visible);
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getStrand()
    {
        return strand;
    }

    public void setStrand(int strand)
    {
        this.strand = strand;
    }

    @Override
    protected Region clone()
    {
        try
        {
            return (Region)super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalError();
        }
    }
}
