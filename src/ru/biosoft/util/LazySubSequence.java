package ru.biosoft.util;

/**
 * CharSequence which represents subsequence of another CharSequence and doesn't read original CharSequence until necessary
 * @author lan
 */
public class LazySubSequence implements CharSequence
{
    private CharSequence parent;
    private int start;
    private int end;

    public LazySubSequence(CharSequence parent, int start, int end)
    {
        this.parent = parent;
        this.start = start;
        this.end = end;
    }

    @Override
    public int length()
    {
        if(end == -1) return parent.length()-start;
        return end-start;
    }

    @Override
    public char charAt(int index)
    {
        return parent.charAt(index+start);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        if(end == -1) return new LazySubSequence(parent, start + this.start, end + this.start);
        if( this.end != -1 && end + this.start > this.end )
            throw new ArrayIndexOutOfBoundsException();
        return new LazySubSequence(parent, start + this.start, end + this.start);
    }

    @Override
    public String toString()
    {
        if(end == -1) return parent.toString().substring(start);
        return parent.toString().substring(start, end);
    }
}
