package ru.biosoft.table.exception;

import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public abstract class TableColumnException extends RepositoryException
{
    private static final String KEY_COLUMN = "column";

    public TableColumnException(Throwable t, ExceptionDescriptor descriptor, TableDataCollection tdc, String columnName)
    {
        super(t, descriptor, tdc.getCompletePath());

        properties.put( KEY_COLUMN, columnName );
    }
}
