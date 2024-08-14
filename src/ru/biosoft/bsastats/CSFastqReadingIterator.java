package ru.biosoft.bsastats;

import java.io.File;
import java.io.IOException;

public class CSFastqReadingIterator extends FastqReadingIterator
{
    private boolean decode;

    public CSFastqReadingIterator(File file, String encoding, boolean rightAlignment, boolean decode) throws IOException
    {
        super(file, encoding, rightAlignment);
        this.decode = decode;
    }
    
    @Override
    protected Task advance()
    {
        Task t = super.advance();
        if( t == null )
            return null;
        if(decode)
        {
            byte[] sequence = SolidReadingIterator.decode( t.getSequence() );
            t = new Task(sequence, t.getQuality(), t.getData());
        }
        return t;
    }

}
