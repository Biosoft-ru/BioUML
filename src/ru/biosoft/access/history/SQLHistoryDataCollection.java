package ru.biosoft.access.history;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.SqlDataCollection;

/**
 * Implementation of {@link HistoryDataCollection} based on SQL-tables
 */
public class SQLHistoryDataCollection extends SqlDataCollection implements HistoryDataCollection
{
    public SQLHistoryDataCollection(DataCollection parent, Properties properties) throws Exception
    {
        super(parent, properties);
    }

    @Override
    public String getNextID()
    {
        List<String> names = getNameList();
        if( names.size() == 0 )
            return "0";
        String previous = names.get(names.size() - 1);
        return Integer.toString(Integer.parseInt(previous) + 1);
    }

    @Override
    public int getNextVersion(DataElementPath elementPath)
    {
        int result = 0;
        String query = "SELECT MAX(" + HistorySQLTransformer.JDBC_VERSION_COLUMN + ") FROM "
                + ( (HistorySQLTransformer)getTransformer() ).getTable() + " WHERE " + HistorySQLTransformer.JDBC_PATH_COLUMN + "='"
                + elementPath.toString() + "'";
        try (Statement statement = getConnection().createStatement(); ResultSet resultSet = statement.executeQuery( query ))
        {
            if( resultSet.next() )
                result = resultSet.getInt(1) + 1;
        }
        catch( SQLException e )
        {
            throw sqlError(e, query);
        }
        return result;
    }

    @Override
    public List<String> getHistoryElementNames(DataElementPath elementPath, int minVersion)
    {
        List<String> result = new ArrayList<>();
        String query = "SELECT " + HistorySQLTransformer.JDBC_ID_COLUMN + " FROM " + ( (HistorySQLTransformer)getTransformer() ).getTable()
                + " WHERE " + HistorySQLTransformer.JDBC_PATH_COLUMN + "='" + elementPath.toString() + "' AND "
                + HistorySQLTransformer.JDBC_VERSION_COLUMN + ">=" + minVersion + " ORDER BY " + HistorySQLTransformer.JDBC_VERSION_COLUMN
                + " DESC";
        try (Statement statement = getConnection().createStatement(); ResultSet resultSet = statement.executeQuery( query ))
        {
            while( resultSet.next() )
                result.add(resultSet.getString(1));
        }
        catch( SQLException e )
        {
            throw sqlError(e, query);
        }
        return result;
    }
}
