package ru.biosoft.bsa;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;

/**
 * Represents single track on some sequence in given region (from, to)
 * @author lan
 *
 */
public class TrackRegion extends DataElementSupport
{
    private Interval interval;
    private DataElementPath sequence;
    private String sequenceName;
    private Track track;
    private Sequence seq;

    public TrackRegion(Track track, String sequence, int from, int to)
    {
        this(track, DataElementPath.create(sequence), new Interval(from, to));
    }

    /**
     * Creates TrackRegion having specified size
     * @param track
     * @param sequence
     * @param from
     * @param to
     */
    public TrackRegion(Track track, DataElementPath sequence, Interval interval)
    {
        super(track.getName(), track.getOrigin());
        this.track = track;
        this.sequence = sequence;
        if( sequence == null )
        {
            this.interval = new Interval(0, 0);
            this.seq = null;
            this.sequenceName = null;
            return;
        }
        seq = this.sequence.getDataElement(AnnotatedSequence.class).getSequence();
        sequenceName = this.sequence.getName();
        this.interval = interval.intersect(seq.getInterval());
    }

    /**
     * Creates TrackRegion having size of whole sequence
     * @param track
     * @param sequence
     */
    public TrackRegion(Track track, String sequence)
    {
        super(track.getName(), track.getOrigin());
        this.track = track;
        this.sequence = DataElementPath.create(sequence);
        if( sequence == null )
        {
            this.interval = new Interval(0, 0);
            this.seq = null;
            return;
        }
        seq = this.sequence.getDataElement(AnnotatedSequence.class).getSequence();
        sequenceName = this.sequence.getName();
        this.interval = seq.getInterval();
    }

    public TrackRegion(Track track, DataElementPath sequence)
    {
        this(track, sequence.toString());
    }

    /**
     * Creates TrackRegion representing track on all sequences
     * Note that some tracks may not support this mode
     * @param track
     */
    public TrackRegion(Track track)
    {
        this(track, (String)null);
    }

    /**
     * Returns Site count for selected region
     */
    public int countSites() throws Exception
    {
        return getSites().getSize();
    }

    /**
     * Returns collection of Site
     */
    public DataCollection<Site> getSites() throws Exception
    {
        return track.getSites(sequence.toString(), interval.getFrom(), interval.getTo());
    }

    /**
     * Returns sites on custom interval
     */
    public DataCollection<Site> getSites(Interval interval) throws Exception
    {
        return track.getSites(sequence.toString(), interval.getFrom(), interval.getTo());
    }

    /**
     * Look for site by name
     */
    public Site getSite(String siteName) throws Exception
    {
        return track.getSite(sequence.toString(), siteName, interval.getFrom(), interval.getTo());
    }

    public int getTo()
    {
        return interval.getTo();
    }

    public int getFrom()
    {
        return interval.getFrom();
    }

    public Interval getInterval()
    {
        return interval;
    }

    public DataElementPath getSequence()
    {
        return sequence;
    }

    public Sequence getSequenceObject()
    {
        return seq;
    }

    public String getSequenceName()
    {
        return sequenceName;
    }

    public Track getTrack()
    {
        return track;
    }

    @Override
    public String toString()
    {
        return "Track = " + getTrack().getName() + "; sequence = " + getSequenceName() + "; interval = " + getInterval();
    }
}
