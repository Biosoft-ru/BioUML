package ru.biosoft.table.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class TableModifyColumnException extends TableColumnException
{
    public static final ExceptionDescriptor ED_TABLE_MODIFY_COLUMN = new ExceptionDescriptor( "ModifyColumn", LoggingLevel.TraceIfNoCause,
            "Cannot modify column '$column$' in table $path$.");

    public TableModifyColumnException(Throwable t, TableDataCollection tdc, String columnName)
    {
        super(t, ED_TABLE_MODIFY_COLUMN, tdc, columnName);
    }
}
