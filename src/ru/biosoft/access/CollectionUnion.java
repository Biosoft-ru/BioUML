package ru.biosoft.access;

import ru.biosoft.access.core.DataCollection;

/**
 * Collection that is union of several collections.
 */
public interface CollectionUnion extends DataCollection
{
    /**
     * Add all elements from specified collection to union.
     *
     * If specified collection already contains in union then all it elements
     * readded again.
     *
     * @param dc Collection with data elements which will be added to union.
     */
    void addCollection( DataCollection dc );

    /**
     * Removes all elements contained in collection with specified name from union.
     *
     * @param name Simple name of data collection which contains elements to remove
     *             from union.
     */
    void removeCollection( String name );
}
