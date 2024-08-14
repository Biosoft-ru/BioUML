package ru.biosoft.access;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Collection that is union of several collections.
 * <b>This class has its own agreement of names</b>.
 *
 * @pending high It is supposed that collections have not the objects with
 * the same names.
 * @todo HIGH if some of data collections in union have data elements with same name
 * then we can access to it only through iterator. get() always return first data element.
 *
 */
public class DataCollectionUnion extends AbstractDataCollection implements CollectionUnion
{
    /**
     * Make union of all collections contained in specified data collection.
     * <ul>Required properties:
     * <li>{@link ru.biosoft.access.core.DataCollection#DataCollectionConfigConstants.NAME_PROPERTY}</li>
     * </ul>
     * @param dc Collection which contain data collections for union.
     */
    public DataCollectionUnion( DataCollection<?> dc, Properties properties )
    {
        super( dc,properties );
    }

    public DataCollectionUnion( String name, DataCollection<?> dc, Properties properties)
    {
        super(name, dc, properties);
    }

    @Override
    public void addCollection(DataCollection dc)
    {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void removeCollection(String name)
    {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Returns the number of elements in this data collection union.
     * @return Size of this data collection.
     */
    @Override
    public int getSize()
    {
        int size = 0;
        DataCollection<?> dco = getOrigin();
        Iterator<DataCollection<?>> i = (Iterator<DataCollection<?>>)getOrigin().iterator();
        while( i.hasNext() )
        {
            size += i.next().getSize();
        }
        return size;
    }

    /**
     * Returns <code>true</code> if this data collection contains
     * the specified data element.
     */
    @Override
    public boolean contains(DataElement element)
    {
        if( element==null )
        {
            return false;
        }
        DataCollection<?> elOrigin = element.getOrigin();
        if( elOrigin==null )
        {
            return false;
        }

        Iterator<DataCollection<?>> i = (Iterator<DataCollection<?>>)getOrigin().iterator();
        while( i.hasNext() )
        {
            DataCollection<?> child = i.next();
            if( child==elOrigin )
            {
                if( child.contains(element) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the <code>ru.biosoft.access.core.DataElement</code> with the specified composite name.
     * Returns <code>null</code> if the data collection
     * contains no data element for this name.
     *
     * @param name Name in format <i>'data_collection_name/data_element_name'</i>.
     *             <i>data_collection_name</i> - name of collection in which
     *             <i>data_element_name</i> element contains.
     * @return {@link ru.biosoft.access.core.DataElement} with specified composite name or <tt>null</tt> if
     *         collection have no data element with specified name.
     * @throws IllegalArgumentException if argument invalid.
     * @throws Exception if an error occurs.
     */
    @Override
    public DataElement get( String name ) throws Exception
    {
        int pos = name.indexOf('/');
        if( pos<=0 )
            throw new IllegalArgumentException("Name "+name+" is not composite.");

        String originName  = name.substring(0,pos);
        String elementName = name.substring(pos+1);

        Iterator<DataCollection<?>> i = (Iterator<DataCollection<?>>)getOrigin().iterator();
        while(i.hasNext())
        {
            DataCollection<?> child = i.next();
            if( child.getName().equals(originName) )
            {
                return child.get( elementName );
            }
        }
        return null;
    }

     /**
     * Returns an iterator over the data elements in this collection.
     * There are no guarantees concerning the order in which the elements
     * are returned. If the data collection is modified while an iteration
     * over it is in progress, the results of the iteration are undefined.
     */
    @Override
    public @Nonnull Iterator<DataElement> iterator()
    {
        return new Iterator<DataElement>()
            {
            Iterator<DataElement> parentI = (Iterator<DataElement>)DataCollectionUnion.this.getOrigin().iterator();
                Iterator<DataElement> childI;
                {
                    if( !parentI.hasNext() )
                        childI = parentI;
                    else
                    {
                        childI = ( (DataCollection)parentI.next() ).iterator();
                        while( parentI.hasNext() && !childI.hasNext() )
                            childI = ( (DataCollection)parentI.next() ).iterator();
                    }
                }

                /**
                 * Returns <tt>true</tt> if the iteration has more elements. (In other
                 * words, returns <tt>true</tt> if <tt>next</tt> would return an element
                 * rather than throwing an exception.)
                 *
                 * @return <tt>true</tt> if the iterator has more elements.
                 */
                @Override
                public boolean hasNext()
                {
                    return childI.hasNext();
                }

                /**
                 * Returns the next element in the iteration.
                 *
                 * @return the next element in the iteration.
                 * @exception NoSuchElementException iteration has no more elements.
                 */
                @Override
                public DataElement next()
                {
                    DataElement retval = childI.next();
                    while( parentI.hasNext() && !childI.hasNext() )
                        childI = ( (DataCollection)parentI.next() ).iterator();
                    return retval;
                }
            };
    }

    /**
     * Return name list of all data elements in this union.
     * Each name in name list is composite. Format is 'data_collection_name/data_element_name'.
//     * Returns an unmodifiable view of the data element name list.
//     * Query operations on the returned list "read through" to the internal name list,
//     * and attempts to modify the returned list, whether direct or via its iterator,
//     * result in an <code>UnsupportedOperationException</code>.
//     *
//     * The returned list is backed by the data collection,
//     * so changes to the data collection are reflected in the returned list.
//     *
//     * The name list can be sorted or unsorted depending on the ru.biosoft.access.core.DataCollection
//     * implementing class.
//     *
     * @todo HIGH Implement above specification.
     */
    @Override
    public @Nonnull List<String> getNameList()
    {
        List<String> nameList = new ArrayList<>();
        Iterator<DataCollection<?>> i = (Iterator<DataCollection<?>>)getOrigin().iterator();
        while(i.hasNext())
        {
            DataCollection<?> child = i.next();
            List<String>   childNames = child.getNameList();
            String childName  = child.getName();
            for(String grandChild: childNames)
                nameList.add(childName+"/"+grandChild);
        }
        return nameList;
    }
}