package ru.biosoft.util;

import java.util.ArrayList;
import java.util.List;

/**
 * String builder which doesn't actually read appended CharSequences until being read by itself
 * @author lan
 */
public class LazyStringBuilder implements CharSequence, Appendable
{
    private List<CharSequence> chunks = new ArrayList<>();
    private final StringBuilder data = new StringBuilder();
    
    private void update()
    {
        for(CharSequence chunk: chunks)
        {
            data.append(chunk);
        }
        data.trimToSize();
        chunks = new ArrayList<>();
    }

    @Override
    public Appendable append(CharSequence csq)
    {
        chunks.add(csq);
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end)
    {
        chunks.add(csq.subSequence(start, end));
        return this;
    }

    @Override
    public Appendable append(char c)
    {
        chunks.add(String.valueOf(c));
        return this;
    }

    @Override
    public int length()
    {
        update();
        return data.length();
    }

    @Override
    public char charAt(int index)
    {
        update();
        return data.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return new LazySubSequence(this, start, end);
    }

    @Override
    public String toString()
    {
        update();
        return data.toString();
    }
}
