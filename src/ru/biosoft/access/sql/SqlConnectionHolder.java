package ru.biosoft.access.sql;

import java.sql.Connection;
import ru.biosoft.access.exception.BiosoftSQLException;

/**
 * interface to hold SQL connection
 */
public interface SqlConnectionHolder
{
    public Connection getConnection() throws BiosoftSQLException;
}
