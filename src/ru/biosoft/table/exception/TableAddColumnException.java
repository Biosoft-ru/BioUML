package ru.biosoft.table.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class TableAddColumnException extends TableColumnException
{
    public static final ExceptionDescriptor ED_TABLE_ADD_COLUMN = new ExceptionDescriptor( "AddColumn", LoggingLevel.TraceIfNoCause,
            "Cannot add column '$column$' to table $path$.");

    public TableAddColumnException(Throwable t, TableDataCollection tdc, String columnName)
    {
        super(t, ED_TABLE_ADD_COLUMN, tdc, columnName);
    }
}
