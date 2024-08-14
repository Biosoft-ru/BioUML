package ru.biosoft.bsa;

import ru.biosoft.access.core.DataCollection;

/**
 * Collection of sites which can tell its size if it's less than specified limit
 * May be useful if exact size counting is expensive
 * @author lan
 *
 */
public interface LimitedSizeSitesCollection extends DataCollection<Site>
{
    /**
     * Implement this to get advantage of limit parameter.
     * @param limit - max number of elements to report size correctly
     * @return size of collection (=getSize()) if its less than limit or any value >=limit otherwise
     */
    public int getSizeLimited(int limit);
}
