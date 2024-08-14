package ru.biosoft.bsa;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;

/**
 * Helper class for the tracks to support SequenceRegion's
 */
public class SubSequence
{
    private final Interval interval;
    private Sequence sequence;
    private Sequence originalSequence;
    private SequenceRegion sequenceRegion = null;
    private String name = null;

    public SubSequence(String sequenceName, int from, int to)
    {
        this(SequenceFactory.getSequence(sequenceName), from, to, sequenceName);
    }
    
    public SubSequence(Sequence sequence)
    {
        this(sequence, sequence.getStart(), sequence.getStart()+sequence.getLength()-1);
    }
    
    private SubSequence(Sequence sequence, int from, int to, String name)
    {
        this.name = name;
        this.originalSequence = this.sequence = sequence;
        if(this.sequence instanceof SequenceRegion)
        {
            sequenceRegion = (SequenceRegion)this.sequence;
            from = sequenceRegion.translatePosition(from);
            to = sequenceRegion.translatePosition(to);
            this.originalSequence = sequenceRegion.getParentSequence();
        }
        if(from > to)
        {
            int tmp = from;
            from = to;
            to = tmp;
        }
        this.interval = new Interval(from, to);
    }

    public SubSequence(Sequence sequence, int from, int to)
    {
        this(sequence, from, to, sequence==null?null:sequence.getName());
    }

    public Sequence getSequence()
    {
        return originalSequence;
    }
    
    public DataElementPath getCompletePath()
    {
        if( originalSequence == null )
            return DataElementPath.create( this.name );
        else
        {
            DataCollection<?> origin = originalSequence.getOrigin();
            if(origin == null)
            {
                throw new DataElementReadException( sequenceRegion != null ? sequenceRegion.getOrigin() : originalSequence, "sequence" );
            }
            return origin.getCompletePath();
        }
    }

    public String getSequenceName()
    {
        return originalSequence == null ? DataElementPath.create( this.name ).getName() : originalSequence.getName();
    }
    
    public Interval getInterval()
    {
        return interval;
    }

    public int getFrom()
    {
        return interval.getFrom();
    }

    public int getTo()
    {
        return interval.getTo();
    }

    /**
     * Translate site from original sequence to region if sequence is sequence region
     * Leaves site as is if not
     */
    public Site translateSite(Site s)
    {
        if(s == null || sequenceRegion == null || s.getOriginalSequence() != originalSequence) return s;
        return new SiteImpl(s.getOrigin(), s.getName(), s.getType(), s.getBasis(), sequenceRegion.translatePositionBack(s.getStart()), s
                .getLength(), s.getPrecision(), sequenceRegion.translateStrand(s.getStrand()), sequence, s.getComment(), s
                .getProperties());
    }

    /**
     * Translate site from region to original sequence if sequence is sequence region
     * Leaves site as is if not
     */
    public Site translateSiteBack(Site s)
    {
        if(s == null || sequenceRegion == null || s.getOriginalSequence() != sequence) return s;
        return new SiteImpl(s.getOrigin(), s.getName(), s.getType(), s.getBasis(), sequenceRegion.translatePosition(s.getStart()), s
                .getLength(), s.getPrecision(), sequenceRegion.translateStrand(s.getStrand()), originalSequence, s.getComment(), s
                .getProperties());
    }
}
