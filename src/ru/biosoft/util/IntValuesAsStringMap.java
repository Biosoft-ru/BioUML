package ru.biosoft.util;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Map with Integer values built upon Map<T, Integer> and converting values to Strings
 * @author lan
 */
public class IntValuesAsStringMap<T> extends AbstractMap<T, String>
{
    private TObjectIntMap<T> origin;
    
    public IntValuesAsStringMap(TObjectIntMap<T> origin)
    {
        this.origin = origin;
    }


    @Override
    public Set<Entry<T, String>> entrySet()
    {
        return new AbstractSet<Entry<T,String>>()
        {
            @Override
            public Iterator<Entry<T, String>> iterator()
            {
                final TObjectIntIterator<T> iterator = origin.iterator();
                return new Iterator<Entry<T,String>>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<T, String> next()
                    {
                        if(!hasNext())
                            throw new NoSuchElementException();
                        iterator.advance();
                        final T key = iterator.key();
                        final String value = String.valueOf(iterator.value());
                        return new Entry<T, String>()
                        {
                            @Override
                            public T getKey()
                            {
                                return key;
                            }

                            @Override
                            public String getValue()
                            {
                                return value;
                            }

                            @Override
                            public String setValue(String value)
                            {
                                int oldValue = iterator.setValue(Integer.parseInt(value));
                                return String.valueOf(oldValue);
                            }
                        };
                    }

                    @Override
                    public void remove()
                    {
                        iterator.remove();
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
        return origin.containsKey(key);
    }


    @Override
    public String get(Object key)
    {
        Integer value = origin.get(key);
        return value == null ? null : String.valueOf(value);
    }


    @Override
    public String put(T key, String value)
    {
        Integer oldValue = origin.put(key, Integer.parseInt(value));
        return oldValue == null ? null : String.valueOf(oldValue);
    }


    @Override
    public String remove(Object key)
    {
        Integer oldValue = origin.remove(key);
        return oldValue == null ? null : String.valueOf(oldValue);
    }

    @Override
    public int size()
    {
        return origin.size();
    }
}
