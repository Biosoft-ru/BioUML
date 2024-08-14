package ru.biosoft.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * @author lan
 *
 */
public class ListUtil
{
    public static int sumTotalSize(Collection<? extends Collection<?>> collection)
    {
        return collection.stream().mapToInt( Collection::size ).sum();
    }

    public static int sumTotalSize(Map<?, ? extends Collection<?>> map)
    {
        return sumTotalSize(map.values());
    }

    public static boolean isEmpty(Collection<? extends Collection<?>> collection)
    {
        return collection.stream().allMatch( Collection::isEmpty );
    }

    public static boolean isEmpty(Map<?, ? extends Collection<?>> map)
    {
        return isEmpty(map.values());
    }

    public static <T extends Comparable<? super T>> void sortAll(Collection<List<T>> collection)
    {
        for(List<T> subCollection: collection)
        {
            Collections.sort(subCollection);
        }
    }

    public static <T extends Comparable<? super T>> void sortAll(Map<?, List<T>> map)
    {
        sortAll(map.values());
    }

    /**
     * Use this method instead of Collections.emptyList for proper null annotation
     * @return
     */
    @Nonnull
    public static <T> List<T> emptyList()
    {
        return Collections.emptyList();
    }

    @Nonnull
    public static <T> Iterator<T> emptyIterator()
    {
        return Collections.<T>emptyList().iterator();
    }
}
