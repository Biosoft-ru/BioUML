
package ru.biosoft.bsa;

/** PENDING: - comments for normalize - error processing */
public class CircularSequence extends LinearSequence
{
    /** Indicates whether the sequence is circular. */
    protected boolean circular;

    /** For the circular sequence normalize the position. */
    protected int normalize(int position)
    {
        while( position < 1 || position > sequence.length )
        {
            if( position == 0 )
                break;
            if( position < 1 )
                position += sequence.length + 1;
            else
                position -= sequence.length;
        }

        return position;
    }

    ////////////////////////////////////////
    // Constructor
    //

    /** Creates Sequence */
    public CircularSequence(byte[] sequence, Alphabet alphabet, boolean circular)
    {
        super(sequence, alphabet);
        this.circular = circular;
    }
    
    public CircularSequence(String sequence, Alphabet alphabet, boolean circular)
    {
        super(sequence, alphabet);
        this.circular = circular;
    }

    ////////////////////////////////////////
    // public
    //

    @Override
    public boolean isCircular()
    {
        return circular;
    }

    @Override
    public byte getLetterAt(int position)
    {
        if( circular && ( position > sequence.length || position < 1 ) )
            position = normalize(position);

        return super.getLetterAt(position);
    }

    @Override
    public void setLetterAt(int position, byte letter)
    {
        if( circular && ( position > sequence.length || position < 1 ) )
            position = normalize(position);

        super.setLetterAt(position, letter);
    }
}
