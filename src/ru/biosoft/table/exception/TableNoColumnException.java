package ru.biosoft.table.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class TableNoColumnException extends TableColumnException
{
    public static final ExceptionDescriptor ED_TABLE_NO_COLUMN = new ExceptionDescriptor( "NoColumn", LoggingLevel.Summary,
            "No column '$column$' in table $path$.");

    public TableNoColumnException(TableDataCollection tdc, String columnName)
    {
        super(null, ED_TABLE_NO_COLUMN, tdc, columnName);
    }
}
