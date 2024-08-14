package ru.biosoft.bsa;

/**
 * @author lan
 *
 */
public abstract class LazyLinearSequence extends LinearSequence
{
    public LazyLinearSequence(String name, Alphabet alphabet)
    {
        super(name, (byte[])null, alphabet);
    }

    public LazyLinearSequence(Alphabet alphabet)
    {
        this(null, alphabet);
    }

    private synchronized void init()
    {
        if(sequence == null) sequence = createSequence();
    }

    protected abstract byte[] createSequence();

    @Override
    public byte getLetterAt(int position)
    {
        init();
        return super.getLetterAt(position);
    }

    @Override
    public void setLetterAt(int position, byte letter)
    {
        init();
        super.setLetterAt(position, letter);
    }

    @Override
    public String toString()
    {
        init();
        return super.toString();
    }

    @Override
    public boolean equals(Object toCompare)
    {
        init();
        return super.equals(toCompare);
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLength()
    {
        init();
        return super.getLength();
    }
}
