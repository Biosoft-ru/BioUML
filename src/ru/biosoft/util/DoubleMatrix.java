package ru.biosoft.util;

/**
 * Memory-efficient implementation of 2D-array with ability to add rows
 * @author lan
 */
public class DoubleMatrix extends AbstractMatrix<double[]>
{
    public DoubleMatrix(int rowSize)
    {
        super(rowSize, double[].class);
    }
    
    public double get(int row, int col)
    {
        return data.get(row/chunkSize)[row%chunkSize*rowSize+col];
    }
    
    public void set(int row, int col, double value)
    {
        data.get(row/chunkSize)[row%chunkSize*rowSize+col] = value;
    }
}
