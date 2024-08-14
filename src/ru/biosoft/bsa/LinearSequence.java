
package ru.biosoft.bsa;

import java.nio.charset.StandardCharsets;

/**
 * PENDING:
 *  - comments
 */
public class LinearSequence extends SequenceSupport
{
    /** The sequence. */
    protected byte[] sequence;
    protected int start;

    ////////////////////////////////////////
    // Constructor
    //

    /**
     * Creates Sequence
     */
    public LinearSequence(String name, int start, byte[] sequence, Alphabet alphabet)
    {
        super(name, alphabet);
        this.sequence = sequence;
        this.start = start;
    }

    public LinearSequence(String name, byte[] sequence, Alphabet alphabet)
    {
        this(name, 1, sequence, alphabet);
    }

    public LinearSequence(String name, String sequence, Alphabet alphabet)
    {
        this( name, sequence.getBytes( StandardCharsets.ISO_8859_1 ), alphabet );
    }
    /**
     * Creates Sequence
     */
    public LinearSequence(byte[] sequence, Alphabet alphabet)
    {
        this(null, sequence, alphabet);
    }

    public LinearSequence(String sequence, Alphabet alphabet)
    {
        this(null, sequence.getBytes( StandardCharsets.ISO_8859_1 ), alphabet);
    }
    
    public LinearSequence(Sequence seq)
    {
        this(seq.getName(), seq.getStart(), seq.getBytes(), seq.getAlphabet());
    }

    ////////////////////////////////////////
    // public
    //

    @Override
    public int getLength()
    {
        return sequence.length;
    }
    
    @Override
    public int getStart()
    {
        return start;
    }

    @Override
    public byte getLetterAt(int position)
    {
        return sequence[position - start];
    }

    @Override
    public void setLetterAt(int position, byte letter)
    {
        sequence[position - start] = letter;
    }

    @Override
    public String toString()
    {
        return new String(sequence, StandardCharsets.ISO_8859_1);
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
}
