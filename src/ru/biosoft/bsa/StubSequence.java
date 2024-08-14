package ru.biosoft.bsa;

public class StubSequence extends SequenceSupport
{
    public StubSequence(String name, Alphabet alphabet)
    {
        super( name, alphabet );
    }

    @Override
    public int getLength()
    {
        return Integer.MAX_VALUE/2;
    }

    @Override
    public int getStart()
    {
        return 1;
    }

    @Override
    public byte getLetterAt(int position) throws RuntimeException
    {
        return 'N';
    }

    @Override
    public void setLetterAt(int position, byte letter) throws RuntimeException
    {
        throw new UnsupportedOperationException();
    }

}
