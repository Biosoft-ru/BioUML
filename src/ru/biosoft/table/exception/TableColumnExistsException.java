package ru.biosoft.table.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class TableColumnExistsException extends TableColumnException
{
    public static final ExceptionDescriptor ED_TABLE_COLUMN_EXISTS = new ExceptionDescriptor( "ColumnExists", LoggingLevel.TraceIfNoCause,
            "Column already exists.");

    public TableColumnExistsException(TableDataCollection tdc, String columnName)
    {
        super(null, ED_TABLE_COLUMN_EXISTS, tdc, columnName);
    }
}
