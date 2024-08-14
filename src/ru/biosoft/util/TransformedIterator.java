package ru.biosoft.util;

import java.util.Iterator;

/**
 * @author lan
 *
 */
public abstract class TransformedIterator<E1, E2> implements Iterator<E2>
{
    private final Iterator<? extends E1> iterator;
    
    public TransformedIterator(Iterator<? extends E1> iterator)
    {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext()
    {
        return iterator.hasNext();
    }

    @Override
    public E2 next()
    {
        return transform(iterator.next());
    }

    protected abstract E2 transform(E1 value);

    @Override
    public void remove()
    {
        iterator.remove();
    }
}
