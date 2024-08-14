package ru.biosoft.bsa;

public class DiscontinuousSequence extends SequenceSupport
{
    private Sequence parent;
    private DiscontinuousCoordinateSystem coordSystem;
    public DiscontinuousSequence(String name, Sequence parent, DiscontinuousCoordinateSystem coordSystem)
    {
        super( name, parent.getAlphabet() );
        this.parent = parent;
        this.coordSystem = coordSystem;
    }

    @Override
    public int getLength()
    {
        return coordSystem.getLength();
    }

    @Override
    public int getStart()
    {
        return 0;
    }

    @Override
    public byte getLetterAt(int position) throws RuntimeException
    {
        int parentPosition = coordSystem.translateCoordinateBack( position );
        byte letter = parent.getLetterAt( parentPosition );
        if(coordSystem.isReverse())
            letter = parent.getAlphabet().letterComplementMatrix()[letter];
        return letter;
    }

    @Override
    public void setLetterAt(int position, byte letter) throws RuntimeException
    {
        int parentPosition = coordSystem.translateCoordinateBack( position );
        parent.setLetterAt( parentPosition, letter );
    }
}
