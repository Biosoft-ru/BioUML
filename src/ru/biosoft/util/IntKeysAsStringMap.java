package ru.biosoft.util;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Map with Integer keys built upon TIntObjectMap<T> and converting keys to Strings
 * @author lan
 */
public class IntKeysAsStringMap<T> extends AbstractMap<String, T>
{
    private final TIntObjectMap<T> origin;
    
    public IntKeysAsStringMap(TIntObjectMap<T> origin)
    {
        this.origin = origin;
    }

    @Override
    public Set<Entry<String, T>> entrySet()
    {
        return new AbstractSet<Entry<String,T>>()
        {
            @Override
            public Iterator<Entry<String, T>> iterator()
            {
                final TIntObjectIterator<T> iterator = origin.iterator();
                return new Iterator<Entry<String, T>>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<String, T> next()
                    {
                        if(!hasNext())
                            throw new NoSuchElementException();
                        iterator.advance();
                        final String key = String.valueOf(iterator.key());
                        final T value = iterator.value();
                        return new Entry<String, T>()
                        {
                            @Override
                            public String getKey()
                            {
                                return key;
                            }

                            @Override
                            public T getValue()
                            {
                                return value;
                            }

                            @Override
                            public T setValue(T value)
                            {
                                return iterator.setValue(value);
                            }
                        };
                    }
                };
            }

            @Override
            public int size()
            {
                return origin.size();
            }
        };
    }

    @Override
    public boolean containsKey(Object key)
    {
        return origin.containsKey(Integer.parseInt(key.toString()));
    }

    @Override
    public T get(Object key)
    {
        return origin.get(Integer.parseInt(key.toString()));
    }

    @Override
    public T put(String key, T value)
    {
        return origin.put(Integer.parseInt(key), value);
    }

    @Override
    public T remove(Object key)
    {
        return origin.remove(Integer.parseInt(key.toString()));
    }

    @Override
    public int size()
    {
        return origin.size();
    }
}
