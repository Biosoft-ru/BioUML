package ru.biosoft.bsa;

import java.util.stream.Stream;

import one.util.streamex.StreamEx;

/** ... PENDING: - comments for reverse - circular reverse */
public class SequenceRegion extends SequenceSupport
{
    /** Start of the sequence relative the parent sequence. */
    protected int start;
    
    /** Start of the sequence itself */
    protected int sequenceStart = 1;

    /** The sequence length. */
    private final int length;

    /** Orientation of the sequence relative the parent sequence. */
    private boolean reverse;

    /** Indicates whether the sequence is circular. */
    private final boolean circular;

    /** The parent for this sequence. */
    private Sequence parent;
    protected byte[] complementLetterMatrix;
    ////////////////////////////////////////
    // public
    //

    /**
     * Creates SequenceRegion
     * @param parent the parent sequence
     * @param start start of the sequence region (first position is 1)
     * @param length the sequence region length
     * @param reverse indicates that sequence region is reverced relative the parent sequence end.
     * @todo perhaps we need additional constructor to process strand instead of boolean 'reverse'
     */
    public SequenceRegion(Sequence parent, int start, int length, boolean reverse, boolean circular)
    {
        super(parent.getName(), parent.getOrigin(), parent.getAlphabet());
        // SequenceRegion created on SequenceRegion will be mapped to original sequence instead
        if(parent instanceof SequenceRegion)
        {
            SequenceRegion parentRegion = (SequenceRegion)parent;
            this.parent = parentRegion.getParentSequence();
            this.reverse = parentRegion.reverse ^ reverse;
            this.start = parentRegion.translatePosition(start);
        } else
        {
            this.parent = parent;
            this.reverse = reverse;
            this.start = start;
        }
        this.circular = circular;
        this.length = length;
        this.complementLetterMatrix = getAlphabet().letterComplementMatrix();
    }
    
    public SequenceRegion(Sequence parent, Interval interval, boolean reverse, boolean circular)
    {
        this(parent, reverse ? interval.getTo() : interval.getFrom(), interval.getLength(), reverse, circular);
    }
    
    public SequenceRegion(Sequence parent, int start, int length, int sequenceStart, boolean reverse, boolean circular)
    {
        this(parent, start, length, reverse, circular);
        setStart(sequenceStart);
    }
    
    public static StreamEx<Sequence> withReversed(Sequence... sequences)
    {
        return StreamEx.of( sequences ).flatMap( seq -> Stream.of(seq, getReversedSequence( seq )) );
    }
    
    public static SequenceRegion getReversedSequence(Sequence sequence)
    {
        return new SequenceRegion(sequence, sequence.getLength(), sequence.getLength(), true, false);
    }

    @Override
    public int getLength()
    {
        return length;
    }
    
    @Override
    public int getStart()
    {
        return sequenceStart;
    }
    
    public void setStart(int sequenceStart)
    {
        this.sequenceStart = sequenceStart;
    }

    @Override
    public boolean isCircular()
    {
        return circular;
    }
    
    public Sequence getParentSequence()
    {
        return parent;
    }
    
    /**
     * Translate position on region to position on original sequence
     */
    public int translatePosition(int position)
    {
        if(reverse)
            return start - position + getStart();
        else
            return start + position - getStart();
    }

    /**
     * Translate position on original sequence to position on region
     */
    public int translatePositionBack(int position)
    {
        if(reverse)
            return start - position + getStart();
        else
            return position - start + getStart();
    }
    
    /**
     * Translates strand of site on original sequence to strand on region
     */
    public int translateStrand(int strand)
    {
        if(reverse)
        {
            if(strand == Site.STRAND_MINUS) return Site.STRAND_PLUS;
            return Site.STRAND_MINUS;
        }
        return strand;
    }

    @Override
    public byte getLetterAt(int position)
    {
        if( !reverse )
        // Beacase of counting begins from 1 we reduce "position"...
        {
            return parent.getLetterAt(translatePosition(position));
        }
        // here we also reduse "position"

        return complementLetterMatrix[parent.getLetterAt(translatePosition(position))];
    }

    @Override
    public void setLetterAt(int position, byte letter)
    {
        if( !reverse )
        {
            parent.setLetterAt(translatePosition(position), letter);
        }
        else
        {
            parent.setLetterAt(translatePosition(position), complementLetterMatrix[letter]);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null || !(obj instanceof Sequence) )
            return false;
        return SequenceFactory.compareSequences((Sequence)this, (Sequence)obj);
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
        int len = getLength();
        byte[] sequence = new byte[len];
        for( int i = 0; i < len; i++ )
            sequence[i] = getLetterAt(i + 1);
        return new String(sequence);
    }
}
