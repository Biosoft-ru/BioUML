package ru.biosoft.access.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.util.ReadAheadIterator;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 */
public class SqlUtil
{
    public static boolean hasResult(Connection c, String query) throws BiosoftSQLException
    {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            return rs.next();
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(c, query, e);
        }
    }
    
    public static boolean hasResult(Connection c, Query query) throws BiosoftSQLException
    {
        return hasResult(c, query.get());
    }
    
    public static boolean hasTable(Connection connection, String tableName)
    {
        String query = "SELECT 1 FROM " + quoteIdentifier( tableName ) + " limit 0";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query))
        {
        }
        catch( SQLSyntaxErrorException e )
        {
            return false;
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(connection, query, e);
        }
        return true;
    }

    public static boolean hasUser(Connection connection, String userName)
    {
        try
        {
            return SqlUtil.hasResult(connection, "SELECT 1 FROM mysql.user WHERE user=" + SqlUtil.quoteString(userName));
        }
        catch( BiosoftSQLException e )
        {
            return false;
        }
    }

    public static boolean hasDatabase( Connection connection, String dbName )
    {
        try
        {
            return SqlUtil.hasResult( connection, 
                "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = " + SqlUtil.quoteString( dbName ) );
        }
        catch( BiosoftSQLException e )
        {
            return false;
        }
    }

    /**
     * Returns int value queried from the database
     * @param c
     * @param query
     * @param defaultValue
     * @return
     * @throws BiosoftSQLException
     */
    public static int queryInt(Connection c, String query, int defaultValue) throws BiosoftSQLException
    {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            if(rs.next()) return rs.getInt(1); else return defaultValue;
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(c, query, e);
        }
    }
    
    public static int queryInt(Connection c, String query) throws BiosoftSQLException
    {
        return queryInt(c, query, 0);
    }
    
    public static int queryInt(Connection c, Query query) throws BiosoftSQLException
    {
        return queryInt(c, query.get(), 0);
    }
    
    public static int queryInt(Connection c, Query query, int defaultValue) throws BiosoftSQLException
    {
        return queryInt(c, query.get(), defaultValue);
    }

    /**
     * Returns long value queried from the database
     * @param c
     * @param query
     * @param defaultValue
     * @return
     * @throws BiosoftSQLException
     */
    public static long queryLong(Connection c, String query, long defaultValue) throws BiosoftSQLException
    {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            if(rs.next()) return rs.getLong(1); else return defaultValue;
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(c, query, e);
        }
    }
    
    public static long queryLong(Connection c, String query) throws BiosoftSQLException
    {
        return queryLong(c, query, 0);
    }
    
    public static long queryLong(Connection c, Query query) throws BiosoftSQLException
    {
        return queryLong(c, query.get(), 0);
    }
    
    public static long queryLong(Connection c, Query query, long defaultValue) throws BiosoftSQLException
    {
        return queryLong(c, query.get(), defaultValue);
    }

    /**
     * Returns String value queried from the database
     * @param c
     * @param query
     * @param defaultValue
     * @return
     * @throws BiosoftSQLException
     */
    public static String queryString(Connection c, String query) throws BiosoftSQLException
    {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            if(rs.next()) return rs.getString(1);
            return null;
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(c, query, e);
        }
    }
    

    /**
     * Returns String value queried from the database
     * @param c
     * @param query
     * @param defaultValue
     * @return
     * @throws BiosoftSQLException
     */
    public static String queryString(Connection c, Query query) throws BiosoftSQLException
    {
        return queryString(c, query.get());
    }
    /**
     * Returns list of strings queried from the database
     * @param c
     * @param query
     * @return
     * @throws BiosoftSQLException
     */
    public static @Nonnull List<String> queryStrings(Connection c, String query) throws BiosoftSQLException
    {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            List<String> result = new ArrayList<>();
            while(rs.next()) result.add(rs.getString(1));
            return result;
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(c, query, e);
        }
    }
    
    /**
     * Returns list of strings queried from the database
     * @param c
     * @param query
     * @return
     * @throws BiosoftSQLException
     */
    public static @Nonnull List<String> queryStrings(Connection c, Query query) throws BiosoftSQLException
    {
        return queryStrings(c, query.get());
    }
    
    /**
     * Returns list of integers queried from the database
     * @param c
     * @param query
     * @return
     * @throws BiosoftSQLException
     */
    public static @Nonnull List<Integer> queryInts(Connection c, String query) throws BiosoftSQLException
    {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            List<Integer> result = new ArrayList<>();
            while(rs.next()) result.add(rs.getInt(1));
            return result;
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(c, query, e);
        }
    }
    
    public static @Nonnull List<Integer> queryInts(Connection c, Query query) throws BiosoftSQLException
    {
        return queryInts(c, query.get());
    }
    
    public static @Nonnull List<Object[]> queryRows(Connection c, String query, Class<?>... classes) throws BiosoftSQLException
    {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            List<Object[]> result = new ArrayList<>();
            while(rs.next())
            {
                Object[] row = new Object[classes.length];
                for(int i=0; i<classes.length; i++)
                {
                    Object value;
                    Class<?> clazz = classes[i];
                    value = getValue(rs, i, clazz);
                    row[i] = value;
                }
                result.add(row);
            }
            return result;
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(c, query, e);
        }
    }
    
    /**
     * @param c connection
     * @param query query which returns rows consisting of two strings
     * @return Map first -> second
     * @throws BiosoftSQLException
     */
    public static Map<String, String> queryMap(Connection c, String query) throws BiosoftSQLException
    {
        Map<String, String> result = new HashMap<>();
        iterate( c, query, rs -> result.put( rs.getString( 1 ), rs.getString( 2 ) ) );
        return result;
    }
    
    public static Map<String, String> queryMap(Connection c, Query query) throws BiosoftSQLException
    {
        return queryMap(c, query.get());
    }

    /**
     * @param c connection
     * @param query query which returns rows consisting of two strings
     * @return Map first -> set of second
     * @throws BiosoftSQLException
     */
    public static Map<String, Set<String>> queryMapSet(Connection c, String query) throws BiosoftSQLException
    {
        Map<String, Set<String>> result = new HashMap<>();
        iterate( c, query, rs -> result.computeIfAbsent( rs.getString( 1 ), k -> new HashSet<>() ).add( rs.getString( 2 ) ) );
        return result;
    }
    
    public static Object[] queryRow(Connection c, String query, Class<?>... classes) throws BiosoftSQLException
    {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            if(!rs.next()) return null;
            Object[] result = new Object[classes.length];
            for(int i=0; i<classes.length; i++)
            {
                Object value;
                Class<?> clazz = classes[i];
                value = getValue(rs, i, clazz);
                result[i] = value;
            }
            return result;
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(c, query, e);
        }
    }
    
    public static Object[] queryRow(Connection c, Query query, Class<?>... classes) throws BiosoftSQLException
    {
        return queryRow(c, query.get(), classes);
    }

    private static Object getValue(ResultSet rs, int i, Class<?> clazz) throws SQLException
    {
        Object value;
        if(clazz.equals(int.class) || clazz.equals(Integer.class))
            value = rs.getInt(i+1);
        else if(clazz.equals(long.class) || clazz.equals(Long.class))
            value = rs.getLong(i+1);
        else if(clazz.equals(short.class) || clazz.equals(Short.class))
            value = rs.getShort(i+1);
        else if(clazz.equals(float.class) || clazz.equals(Float.class))
            value = rs.getFloat(i+1);
        else if(clazz.equals(double.class) || clazz.equals(Double.class))
            value = rs.getDouble(i+1);
        else if(clazz.equals(String.class))
            value = rs.getString(i+1);
        else if(clazz.equals(boolean.class) || clazz.equals(Boolean.class))
            value = rs.getBoolean(i+1);
        else
            value = TextUtil2.fromString(clazz, rs.getString(i+1));
        return value;
    }
    
    public static boolean execute(Connection connection, String query) throws BiosoftSQLException
    {
        try (Statement st = connection.createStatement())
        {
            return st.execute(query);
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(connection, query, e);
        }
    }
    
    public static boolean execute(Connection connection, Query query) throws BiosoftSQLException
    {
        return execute(connection, query.get());
    }
    
    public static int executeUpdate(Connection connection, String query) throws BiosoftSQLException
    {
        try (Statement st = connection.createStatement())
        {
            return st.executeUpdate(query);
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(connection, query, e);
        }
    }
    
    public static int executeUpdate(Connection connection, Query query) throws BiosoftSQLException
    {
        return executeUpdate(connection, query.get());
    }
    
    public static int insertGeneratingKey(Connection con, String query)
    {
        try (Statement st = con.createStatement())
        {
            st.executeUpdate( query, Statement.RETURN_GENERATED_KEYS );
            try (ResultSet rs = st.getGeneratedKeys())
            {
                rs.next();
                return rs.getInt( 1 );
            }
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException( con, query, e );
        }
    }
    
    public static int insertGeneratingKey(Connection con, Query query)
    {
        return insertGeneratingKey( con, query.get() );
    }
    
    public static ResultSet executeQuery(Statement st, String query) throws BiosoftSQLException
    {
        try
        {
            return st.executeQuery(query);
        }
        catch( SQLException e )
        {
            Connection connection = null;
            try
            {
                connection = st.getConnection();
            }
            catch( SQLException e1 )
            {
            }
            throw new BiosoftSQLException(connection, query, e);
        }
    }
    
    public static ResultSet executeQuery(Statement st, Query query) throws BiosoftSQLException
    {
        return executeQuery(st, query.get());
    }
    
    public static ResultSet executeAndAdvance(Statement st, String query) throws BiosoftSQLException
    {
        ResultSet resultSet = executeQuery(st, query);
        try
        {
            if(resultSet.next())
            {
                ResultSet rs = resultSet;
                resultSet = null;
                return rs;
            }
        }
        catch( SQLException e )
        {
            Connection connection = null;
            try
            {
                connection = st.getConnection();
            }
            catch( SQLException e1 )
            {
            }
            throw new BiosoftSQLException(connection, query, e);
        }
        finally
        {
            close(null, resultSet);
        }
        return null;
    }
    
    public static ResultSet executeAndAdvance(Statement st, Query query) throws BiosoftSQLException
    {
        return executeAndAdvance(st, query.get());
    }
    
    public static Statement createStatement(Connection c) throws BiosoftSQLException
    {
        try
        {
            return c.createStatement();
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException(c, null, e);
        }
    }
    
    public static void dropTable(Connection connection, String tableName) throws BiosoftSQLException
    {
        execute(connection, "DROP TABLE IF EXISTS "+quoteIdentifier(tableName));
    }
    
    public static int getRowsCount(Connection connection, String tableName) throws BiosoftSQLException
    {
        return queryInt(connection, "SELECT COUNT(*) FROM "+quoteIdentifier(tableName));
    }
    
    /**
     * Returns disk size of the table in bytes
     */
    public static long getTableSize(Connection connection, String tableName) throws BiosoftSQLException
    {
        String query = "SELECT DATA_LENGTH, INDEX_LENGTH from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA=DATABASE() and TABLE_NAME=" + quoteString(tableName);
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            if(!rs.next())
                return 0;
            return rs.getLong(1)+rs.getLong(2);
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(connection, query, e);
        }
    }
    
    public static long getAvgRowLength(Connection connection, String tableName) throws BiosoftSQLException
    {
        String query = "SELECT AVG_ROW_LENGTH from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA=DATABASE() and TABLE_NAME=" + quoteString(tableName);
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query))
        {
            if(!rs.next())
                return 0;
            return rs.getLong(1);
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(connection, query, e);
        }
    }
    
    /**
     * @param connection
     * @throws SQLException
     */
    public static void checkConnection(Connection connection) throws BiosoftSQLException
    {
        execute(connection, "SELECT 1");
    }
    
    public static String quoteIdentifier(String str)
    {
        return "`" + str.replace("`", "``") + "`";
    }
    
    public static @Nonnull String quoteString(String str)
    {
        if(str == null)
            return "null";
        return "'" + str.replace("'", "''").replace("\\", "\\\\") + "'";
    }
    
    public static void close(Statement st, ResultSet rs)
    {
        try
        {
            if(rs != null) rs.close();
        }
        catch( SQLException e )
        {
        }
        try
        {
            if(st != null) st.close();
        }
        catch( SQLException e )
        {
        }
    }
    
    /**
     * Creates stream of objects mapped from each {@link ResultSet} row
     * @param c connection to use
     * @param query query to execute
     * @param transformer transformer which accepts {@link ResultSet} already positioned to the row and returns an object associated with given row
     * @return stream of objects created by transformer
     * @throws BiosoftSQLException if underlying {@link SQLException} occurs (either during statement creation or query execution or thrown by transformer).
     */
    public static <T> StreamEx<T> stream(Connection c, Query query, ResultSetMapper<T> transformer) throws BiosoftSQLException
    {
        return stream( c, query.get(), transformer );
    }
    
    /**
     * Creates stream of objects mapped from each {@link ResultSet} row
     * @param c connection to use
     * @param query query to execute
     * @param transformer transformer which accepts {@link ResultSet} already positioned to the row and returns an object associated with given row
     * @return stream of objects created by transformer
     * @throws BiosoftSQLException if underlying {@link SQLException} occurs (either during statement creation or query execution or thrown by transformer).
     * Note that current implementation is somewhat broken: it's necessary to iterate the result set completely,
     * so some stream idioms may leave result set unclosed.
     */
    public static <T> StreamEx<T> stream(Connection c, String query, ResultSetMapper<T> transformer) throws BiosoftSQLException
    {
        Statement st = null;
        ResultSet rs = null;
        try
        {
            st = c.createStatement();
            rs = st.executeQuery( query );
        }
        catch( SQLException e )
        {
            close( st, rs );
            throw new BiosoftSQLException( c, query, e );
        }
        Statement finalSt = st;
        ResultSet finalRs = rs;
        return StreamEx.of( Spliterators.spliteratorUnknownSize( new ReadAheadIterator<T>()
        {
            @Override
            protected T advance()
            {
                try
                {
                    if(!finalRs.next())
                    {
                        close(finalSt, finalRs);
                        return null;
                    }
                    return transformer.map( finalRs );
                }
                catch( SQLException e )
                {
                    close(finalSt, finalRs);
                    throw new BiosoftSQLException(c, query, e);
                }
                catch( Throwable t )
                {
                    close(finalSt, finalRs);
                    throw t;
                }
            }
        }, Spliterator.ORDERED | Spliterator.IMMUTABLE));
    }
    
    public static StreamEx<String> stringStream(Connection c, String query) throws BiosoftSQLException
    {
        return stream( c, query, rs -> rs.getString( 1 ) );
    }
    
    public static StreamEx<String> stringStream(Connection c, Query query) throws BiosoftSQLException
    {
        return stream( c, query, rs -> rs.getString( 1 ) );
    }
    
    public static void iterate(Connection c, String query, ResultSetIteration rsi) throws BiosoftSQLException
    {
        try(Statement st = c.createStatement(); ResultSet rs = st.executeQuery( query ))
        {
            while(rs.next())
            {
                rsi.accept( rs );
            }
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException( c, query, e );
        }
    }
    
    public static void iterate(Connection c, Query query, ResultSetIteration rsi) throws BiosoftSQLException
    {
        iterate( c, query.get(), rsi );
    }
    
    /**
     * 
     * @param adminConnection - connection (should have enough rights for operation)
     * @param database - database to create
     * @param username
     * @param password
     * @throws BiosoftSQLException
     */
    public static void createDatabase(Connection adminConnection, String database, String username, String password) throws BiosoftSQLException
    {
        execute(adminConnection, "CREATE DATABASE "+quoteIdentifier(database));
        execute(adminConnection, "GRANT ALL ON "+quoteIdentifier(database)+".* TO "+quoteString(username)+"@'%' IDENTIFIED BY "+quoteString(password));
        execute(adminConnection, "GRANT ALL ON "+quoteIdentifier(database)+".* TO "+quoteString(username)+"@'localhost' IDENTIFIED BY "+quoteString(password));
    }
    
    public static Function<String, String> getStringByName(ResultSet rs) throws BiosoftSQLException
    {
        return name -> {
            try
            {
                return rs.getString( name );
            }
            catch( SQLException e )
            {
                Connection conn = null;
                try
                {
                    conn = rs.getStatement().getConnection();
                }
                catch( SQLException e1 )
                {
                    // ignore
                }
                throw new BiosoftSQLException( conn, null, e );
            }
        };
    }
    
    public static boolean isIndexExists(Connection c, String table, String indexName)
    {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                + "WHERE table_schema=DATABASE() AND table_name=" + quoteString( table ) + " AND index_name=" + quoteString( indexName );
        return queryInt( c, sql ) > 0;
    }
    
    public static boolean isIndexExists(Connection c, String table, String indexName, String database)
    {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                + "WHERE table_schema=" + quoteString( database ) + " AND table_name=" + quoteString( table ) + " AND index_name=" + quoteString( indexName );
        return queryInt( c, sql ) > 0;
    }

}
