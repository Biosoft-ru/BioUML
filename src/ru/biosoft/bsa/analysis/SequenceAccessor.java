package ru.biosoft.bsa.analysis;

import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;

public class SequenceAccessor
{
    private Sequence sequence;

    public SequenceAccessor(Sequence sequence)
    {
        this.sequence = sequence;
    }

    public synchronized CachedSequenceRegion getSubSequence(int start, int length, boolean reverse)
    {
        return new CachedSequenceRegion(sequence, start, length, reverse);
    }

    public static class CachedSequenceRegion extends SequenceRegion
    {
        private byte[] data;
        private int dataStart;
        private boolean dataReversed;

        public CachedSequenceRegion(Sequence parent, int start, int length, boolean reverse)
        {
            super(parent, start, length, parent.getStart(), reverse, false);
            if( parent instanceof CachedSequenceRegion )
            {
                CachedSequenceRegion parentRegion = (CachedSequenceRegion)parent;
                data = parentRegion.data;
                dataStart = parentRegion.dataStart + start - getStart();
                dataReversed = parentRegion.dataReversed ^ reverse;
            }
            else
            {
                data = new byte[length];
                for( int i = 0; i < data.length; i++ )
                    data[i] = super.getLetterAt(i + getStart());
            }
        }

        @Override
        public byte getLetterAt(int pos)
        {
            return dataReversed ? complementLetterMatrix[data[dataStart - pos + getStart()]]
                                : data[dataStart + pos - getStart()];
        }
        
        public CachedSequenceRegion getReverseComplement()
        {
            return new CachedSequenceRegion( this, getInterval().getTo(), getLength(), true );
        }
    }

}
