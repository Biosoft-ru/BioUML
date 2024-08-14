package ru.biosoft.util;

import java.util.Arrays;

/**
 * Dynamic array of integers based on primitive int instead of Integer to spare memory and prevent boxing
 * Not thread-safe, not synchronized
 * @todo replace everywhere with Trove TIntArrayList and remove this class
 * @author lan
 */
public class IntArray
{
    public static final int DEFAULT_INITIAL_CAPACITY = 100;
    
    private int[] data;
    private int length;
    
    public IntArray()
    {
        this(DEFAULT_INITIAL_CAPACITY);
    }
    
    public IntArray(int initialCapacity)
    {
        this.length = 0;
        this.data = new int[initialCapacity];
    }
    
    public IntArray(int[] initData)
    {
        this.length = initData.length;
        this.data = new int[initData.length];
        System.arraycopy(initData, 0, data, 0, data.length);
    }
    
    public void add(int element)
    {
        if(length == data.length) grow();
        data[length++] = element;
    }
    
    /**
     * Enlarges the array length to the given size (does nothing if current size is bigger or equal)
     * New elements will be set to zero
     */
    public void growTo(int newLength)
    {
        if(length >= newLength) return;
        if(data.length >= newLength)
        {
            length = newLength;
            return;
        }
        int[] newData = new int[newLength];
        System.arraycopy(data, 0, newData, 0, length);
        data = newData;
        length = newLength;
    }
    
    public void set(int pos, int newElement)
    {
        data[pos] = newElement;
    }
    
    public void clear()
    {
        this.data = new int[DEFAULT_INITIAL_CAPACITY];
        this.length = 0;
    }
    
    public final int get(int pos)
    {
        return data[pos];
    }
    
    public final int size()
    {
        return length;
    }
    
    public void sort()
    {
        Arrays.sort(data, 0, length);
    }

    /**
     * @return raw array data. Note that array length may be bigger than actual IntArray length. Better to call after compress()
     */
    public int[] data()
    {
        return data;
    }
    
    /**
     * Shrinks array size to the data. Useful to call after array filling is finished to spare memory
     */
    public void compress()
    {
        if(data.length == length) return;
        int[] newData = new int[length];
        System.arraycopy(data, 0, newData, 0, length);
        data = newData;
    }
    
    private void grow()
    {
        int[] newData = new int[data.length*2];
        System.arraycopy(data, 0, newData, 0, length);
        data = newData;
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        for(int i=0; i<size(); i++)
        {
            if(i > 0)
                sb.append(',');
            sb.append(get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
