package ru.biosoft.table;

import javax.swing.table.TableModel;

import com.developmentontheedge.beans.swing.table.SortedTableModel;

/**
 * Extended version of {@link SortedTableModel} which can inform the client whether sorting is actually supported
 * @author lan
 */
public interface BiosoftTableModel extends SortedTableModel, TableModel
{
    public boolean isSortingSupported();
    
    /**
     * Get the real value of given cell (not Property returned by getValueAt())
     * This method is equivalent to
     * <code>((Property)getValueAt(row, column)).getValue()</code>
     * except that it might be optimized
     * @param row - row number
     * @param column - column number
     * @return real value of given cell. 
     */
    public Object getRealValue(int row, int column);

    /**
     * Get the name of the row
     * @param row row number
     * @return row name if available or null otherwise
     */
    public String getRowName(int row);

    /**
     * Limits list of available rows to given range. 
     * Calling this method before getRealValue series may increase the performance.
     * @param rowFrom first row in range
     * @param rowTo last row in range (excluding)
     */
    public void setRange(int rowFrom, int rowTo);
}
