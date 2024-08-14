package ru.biosoft.access.security;

import java.sql.Connection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionHolder;

/**
 * @author lan
 *
 */
public class ProtectedSqlConnectionHolder implements SqlConnectionHolder
{
    private DataCollection<?> collection;

    public ProtectedSqlConnectionHolder(DataCollection<?> collection)
    {
        this.collection = collection;
    }

    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return DataCollectionUtils.getSqlConnection(collection);
    }

}
