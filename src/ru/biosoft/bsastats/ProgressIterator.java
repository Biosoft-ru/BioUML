package ru.biosoft.bsastats;

import java.util.Iterator;

/**
 * @author lan
 *
 */
public interface ProgressIterator<E> extends Iterator<E>
{
    /**
     * @return value between 0 and 1 indicating how much of the set was already iterated
     */
    public float getProgress();
}
