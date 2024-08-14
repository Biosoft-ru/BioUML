package ru.biosoft.table.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class TableRemoveColumnException extends TableColumnException
{
    public static final ExceptionDescriptor ED_TABLE_REMOVE_COLUMN = new ExceptionDescriptor( "RemoveColumn", LoggingLevel.TraceIfNoCause,
            "Cannot remove column '$column$' from table $path$.");

    public TableRemoveColumnException(Throwable t, TableDataCollection tdc, String columnName)
    {
        super(t, ED_TABLE_REMOVE_COLUMN, tdc, columnName);
    }
}
