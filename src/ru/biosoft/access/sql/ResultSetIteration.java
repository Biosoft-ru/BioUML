package ru.biosoft.access.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetIteration
{
    public void accept(ResultSet rs) throws SQLException;
}
