package ru.biosoft.access.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetMapper<T>
{
    T map(ResultSet rs) throws SQLException;
}
