package ru.biosoft.util;

import java.util.AbstractList;
import java.util.List;

/**
 * Makes read-only list reversed on the fly to the original without modifying the original one
 * @author lan
 * @param <E> type of list element
 */
public class ReversedList<E> extends AbstractList<E>
{
    private List<? extends E> origin;
    
    public ReversedList(List<? extends E> origin)
    {
        this.origin = origin;
    }
    
    @Override
    public E get(int index)
    {
        return this.origin.get(this.origin.size()-index-1);
    }

    @Override
    public int size()
    {
        return this.origin.size();
    }
}
