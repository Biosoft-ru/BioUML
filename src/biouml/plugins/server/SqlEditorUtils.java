package biouml.plugins.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SqlEditorUtils
{
    public static List<String[]> getTableStructure(Connection connection, String fullTableName) throws SQLException
    {
        String sqlCols = "DESC ";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery( sqlCols + fullTableName ))
        {
            List<String[]> cm = new ArrayList<>();
            while( rs.next() )
            {
                String name = rs.getString("Field");
                String type = rs.getString("Type");
                cm.add(new String[] {name, type});
            }
            return cm;
        }
    }

    public static Map<String, List<String[]>> getTablesStructure(Connection connection) throws SQLException
    {
        Map<String, List<String[]>> result = new HashMap<>();
        String sql = "SHOW DATABASES";
        String sqlTbl = "SHOW TABLES FROM ";
        String sqlCols = "DESC ";
        try (PreparedStatement pstmt = connection.prepareStatement( sql ); ResultSet rs = pstmt.executeQuery();)
        {
            while( rs.next() )
            {
                String databaseName = rs.getString("Database");
                if( databaseName.equals("information_schema") )
                    continue;

                try (PreparedStatement pstmtTbl = connection.prepareStatement( sqlTbl + databaseName ))
                {
                    ResultSet rsTbl = pstmtTbl.executeQuery();
                    while( rsTbl.next() )
                    {
                        String tableName = rsTbl.getString(1);
                        String fullTableName = databaseName + "." + tableName;
                        List<String[]> cm;
                        try (PreparedStatement pstmtCols = connection.prepareStatement( sqlCols + fullTableName ))
                        {
                            ResultSet rsCols = pstmtCols.executeQuery();
                            cm = new ArrayList<>();
                            while( rsCols.next() )
                            {
                                String name = rsCols.getString("Field");
                                String type = rsCols.getString("Type");
                                cm.add(new String[] {name, type});
                            }
                        }
                        result.put(fullTableName, cm);
                    }
                }
            }
        }
        return result;
    }

    public static Set<String> getTables(Connection connection) throws SQLException
    {
        Set<String> result = new HashSet<>();
        String sql = "SHOW DATABASES";
        String sqlTbl = "SHOW TABLES FROM ";
        try (PreparedStatement pstmt = connection.prepareStatement( sql ); ResultSet rs = pstmt.executeQuery())
        {
            while( rs.next() )
            {
                String databaseName = rs.getString("Database");
                if( databaseName.equals("information_schema") )
                    continue;

                try (PreparedStatement pstmtTbl = connection.prepareStatement( sqlTbl + databaseName ))
                {
                    ResultSet rsTbl = pstmtTbl.executeQuery();
                    while( rsTbl.next() )
                    {
                        String tableName = rsTbl.getString(1);
                        String fullTableName = databaseName + "." + tableName;
                        result.add(fullTableName);
                    }
                }
            }
        }
        return result;
    }

    public static List<List<String>> getQueryResult(Connection connection, String query, int start, int length) throws SQLException
    {
        List<List<String>> result = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement( query ); ResultSet rs = pstmt.executeQuery())
        {
            ResultSetMetaData rsmd = rs.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();
            String columnName;
            List<String> columns = new ArrayList<>();
            for( int i = 1; i <= numberOfColumns; i++ )
            {
                columnName = rsmd.getColumnName( i );
                if( columnName == null || columnName.isEmpty() )
                    columnName = "Column" + i;
                columns.add( columnName );
            }
            result.add( columns );

            int rowCnt = 0;
            while( rs.next() )
            {
                if( rowCnt >= start && rowCnt < start + length )
                {
                    List<String> values = new ArrayList<>();
                    for( int i = 1; i <= numberOfColumns; i++ )
                    {
                        values.add( rs.getString( i ) );
                    }
                    result.add( values );
                }
                rowCnt++;
            }
        }
        return result;
    }

}
