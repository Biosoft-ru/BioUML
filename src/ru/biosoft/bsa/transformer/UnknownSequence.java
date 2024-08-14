package ru.biosoft.bsa.transformer;

import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.SequenceSupport;

/**
 * Sequence which consists of 'N'-s
 * @author lan
 */
public class UnknownSequence extends SequenceSupport
{
    private int start;
    private int end;

    public UnknownSequence(String name, int start, int end)
    {
        super(name, Nucleotide5LetterAlphabet.getInstance());
        this.start = start;
        this.end = end;
    }
    
    @Override
    public int getLength()
    {
        return end-start+1;
    }

    @Override
    public int getStart()
    {
        return start;
    }

    @Override
    public byte getLetterAt(int position) throws RuntimeException
    {
        return 'N';
    }

    @Override
    public void setLetterAt(int position, byte letter) throws RuntimeException
    {
    }
}