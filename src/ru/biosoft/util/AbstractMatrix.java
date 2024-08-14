package ru.biosoft.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lan
 *
 */
public abstract class AbstractMatrix<T>
{
    private static final int CHUNK_SIZE = 1000000;
    protected List<T> data = new ArrayList<>();
    protected int rowSize;
    protected int chunkSize;
    protected int length = 0;
    protected Class<?> type;

    @SuppressWarnings ( "unchecked" )
    protected AbstractMatrix(int rowSize, Class<T> type)
    {
        this.rowSize = rowSize;
        this.chunkSize = CHUNK_SIZE/rowSize;
        this.type = type.getComponentType();
        data.add((T)Array.newInstance(this.type, Math.min(10,chunkSize)*rowSize));
    }
    
    @SuppressWarnings ( "unchecked" )
    public void add()
    {
        if( data.size() == 1 )
        {
            T data1 = data.get(0);
            int length1 = Array.getLength(data1);
            if( length * rowSize == length1 )
            {
                if( length == chunkSize )
                    data.add((T)Array.newInstance(this.type, chunkSize * rowSize));
                else
                {
                    T newData = (T)Array.newInstance(this.type, Math.min(length1 * 2, chunkSize * rowSize));
                    System.arraycopy(data1, 0, newData, 0, length * rowSize);
                    data.set(0, newData);
                }
            }
        }
        else if( length == data.size() * chunkSize )
            data.add((T)Array.newInstance(this.type, chunkSize * rowSize));
        length++;
    }
    
    public T get(int row)
    {
        @SuppressWarnings ( "unchecked" )
        T result = (T)Array.newInstance(this.type, rowSize);
        System.arraycopy(data.get(row/chunkSize), row%chunkSize*rowSize, result, 0, rowSize);
        return result;
    }
    
    public void add(T row)
    {
        add();
        System.arraycopy(row, 0, data.get((length-1)/chunkSize), (length-1)%chunkSize*rowSize, rowSize);
    }
    
    public int size()
    {
        return length;
    }
}
