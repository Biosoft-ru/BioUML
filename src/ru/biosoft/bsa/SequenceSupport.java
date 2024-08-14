package ru.biosoft.bsa;

import ru.biosoft.access.core.DataCollection;

public abstract class SequenceSupport implements Sequence
{
    private String name;
    private DataCollection origin;
    protected Alphabet alphabet;
    protected byte[] letterToCodeMatrix;
    protected byte[] codeToLetterMatrix;
    
    public SequenceSupport(String name, Alphabet alphabet)
    {
        this(name, null, alphabet);
    }

    public SequenceSupport(String name, DataCollection origin, Alphabet alphabet)
    {
        this.name = name;
        this.origin = origin;
        this.alphabet = alphabet;
        this.letterToCodeMatrix = alphabet.letterToCodeMatrix();
        this.codeToLetterMatrix = alphabet.codeToLetterMatrix();
    }
    
    protected void setOrigin(DataCollection origin)
    {
        this.origin = origin;
        this.name = origin.getName();
    }
    
    @Override
    public DataCollection getOrigin()
    {
        return origin;
    }

    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public byte[] getBytes()
    {
        byte[] result = new byte[getLength()];
        for(int i = 0; i < result.length; i++)
            result[i] = getLetterAt(i + getStart());
        return result;
    }

    @Override
    public Interval getInterval()
    {
        return new Interval(getStart(), getStart()+getLength()-1);
    }

    @Override
    public Alphabet getAlphabet()
    {
        return alphabet;
    }

    @Override
    public byte getLetterCodeAt(int position)
    {
        return letterToCodeMatrix[getLetterAt(position)];
    }

    @Override
    public byte getLetterCodeAt(int position, Alphabet alphabet)
    {
        if(alphabet.codeLength() == 1)
            return alphabet.letterToCodeMatrix()[getLetterAt(position)];
        byte[] b = new byte[alphabet.codeLength()];
        for(int i=0; i<b.length; i++)
            b[i] = getLetterAt(position+i);
        return alphabet.lettersToCode(b, 0);
    }

    @Override
    public void setLetterCodeAt(int position, byte code)
    {
        setLetterAt(position, codeToLetterMatrix[code]);
    }

    /**
     * @return <code>false</code>.
     */
    @Override
    public boolean isCircular()
    {
        return false;
    }
}
