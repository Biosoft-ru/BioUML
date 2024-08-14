package ru.biosoft.util;

/**
 * Memory-efficient implementation of 2D-array with ability to add rows
 * @author lan
 */
public class ByteMatrix extends AbstractMatrix<byte[]>
{
    public ByteMatrix(int rowSize)
    {
        super(rowSize, byte[].class);
    }
    
    public byte get(int row, int col)
    {
        return data.get(row/chunkSize)[row%chunkSize*rowSize+col];
    }
    
    public void set(int row, int col, byte value)
    {
        data.get(row/chunkSize)[row%chunkSize*rowSize+col] = value;
    }
}
