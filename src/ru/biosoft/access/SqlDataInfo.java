package ru.biosoft.access;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementRemoveException;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.table.SqlTableDataCollection;


/**
 * Utility class for SqlDataElement elements
 * @author lan
 * @see DataElementsSqlTransformer
 */
public class SqlDataInfo
{
    private static final Query ID_QUERY = new Query( "SELECT id FROM data_element WHERE parent = $parent$ AND name = $name$" );
    private static final Query DELETE_QUERY = new Query( "DELETE FROM data_element WHERE parent = $parent$ AND name = $name$" );
    private static final Query DELETE_INFO = new Query( "DELETE FROM de_info WHERE data_element_id = $id$" );

    private static final Pattern VARCHAR_FIELD_PATTERN = Pattern.compile("varchar\\((\\d+)\\)", Pattern.CASE_INSENSITIVE);

    public static final String ID_PROPERTY = "SqlId";
    public static final int INITIAL_COLUMN_LENGTH = 16;
    public static final int MAX_VARCHAR_LENGTH = 8192;
    public static Map<String, AtomicInteger> tableName2counter = new ConcurrentHashMap<String, AtomicInteger>();

    /**
     * RegEx Patterns to convert SQL types to Java types
     */
    private static final Map<String, Class<?>> columnTypePattern = new HashMap<>();
    
    static
    {
        columnTypePattern.put("(int).*", Integer.class);
        columnTypePattern.put("(float|double|decimal).*", Float.class);
        columnTypePattern.put("(varchar|char|text).*", String.class);
    }

    /**
     * Converts SQL column type to Java type using COLUMN_TYPE_PATTERN
     */
    public static Class<?> getJavaColumnType(String type)
    {
        return StreamEx.ofValues(columnTypePattern,
                    key -> Pattern.compile(key, Pattern.CASE_INSENSITIVE).matcher(type).matches())
                .findAny().orElse(null);
    }

    
    /**
     * Will return data element Id from data_element table using given connection
     * @param conn DB connection
     * @param completeName complete name of the element
     */
    public static int getIdByName(Connection conn, String completeName) throws BiosoftSQLException
    {
        DataElementPath elementPath = DataElementPath.create(completeName);
        String parentName = elementPath.getParentPath().toString(), name = elementPath.getName();
        return SqlUtil.queryInt( conn, ID_QUERY.str( "parent", parentName ).str( "name", name ), -1 );
    }

    // MySQL table name length is limited to 62 or 64
    // Thus we limit base length to 56 (as sometimes we need to attach a number also)
    private static final int MAX_TABLE_NAME_BASE_LENGTH = 56;
    
    private static String escapeTableNameElement(String tableNameElement)
    {
        String result = tableNameElement;
        result = result.replaceAll("[\\.\\-\\ ]", "_");
        result = result.replaceAll("\\W", "");
        return result;
    }

    /**
     * Generate unique table name based on specified DataElementPath and return it
     */
    public static String generateTableId(Connection conn, DataElementPath path, String prefix) throws BiosoftSQLException
    {
        String[] pathElements = path.toString().split(DataElementPath.PATH_SEPARATOR);
        String postfix = escapeTableNameElement(pathElements[pathElements.length-1]);
        String baseTableName = prefix+"_"+postfix;
        StringBuilder prefixBuilder = new StringBuilder(prefix);
        for(int i=1; i<pathElements.length; i++)
        {
            if( i % 2 == 0 )
                postfix = escapeTableNameElement( pathElements[pathElements.length - i / 2 - 1] ) + "_" + postfix;
            else
                prefixBuilder.append( "_" ).append( escapeTableNameElement( pathElements[i / 2] ) );

            if( prefixBuilder.length() + postfix.length() + 2 > MAX_TABLE_NAME_BASE_LENGTH )
                break;

            baseTableName = prefixBuilder + ( i < pathElements.length - 1 ? "__" : "_" ) + postfix;
        }
        // name was too long even without any path components: trim it
        if(baseTableName.length() > MAX_TABLE_NAME_BASE_LENGTH)
        {
            baseTableName = baseTableName.substring(0, MAX_TABLE_NAME_BASE_LENGTH);
        }
        tableName2counter.computeIfAbsent( baseTableName, v -> new AtomicInteger( 0 ) );
        //Table name will always have suffix _1 etc. to avoid race condition in name creation
        String tableName = baseTableName + "_" + tableName2counter.get( baseTableName ).incrementAndGet();
        while(true)
        {
            if(!SqlUtil.hasTable(conn, tableName))
                return tableName;

            tableName = baseTableName + "_" + tableName2counter.get( baseTableName ).incrementAndGet();
        }
    }

    /**
     * Returns properties of data element by given id
     * @param conn DB connection
     * @param id data element id
     * @return Properties object containing properties (empty if no such data element found or it contains no properties)
     * @throws SQLException
     */
    public static Properties getProperties(Connection conn, int id) throws SQLException
    {
        Properties prop;
        try(PreparedStatement st = conn.prepareStatement("SELECT name,parent FROM data_element WHERE id = ?"))
        {
            st.setInt(1, id);
            try(ResultSet rs = st.executeQuery())
            {
                if( !rs.next() ) return null;
                prop = (Properties)DataElementPath.create(rs.getString(2)).getDataCollection().getInfo().getProperties().clone();
                prop.remove(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
                prop.remove(DataCollectionConfigConstants.CLASS_PROPERTY);
                prop.remove(DataCollectionConfigConstants.CONFIG_FILE_PROPERTY);
                prop.remove(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
                prop.put(DataCollectionConfigConstants.NAME_PROPERTY, rs.getString(1));
                prop.put(ID_PROPERTY, String.valueOf(id));
            }
            if( id <= 0 )
                return prop;
        }
        try(PreparedStatement st = conn.prepareStatement("SELECT name,value FROM de_info WHERE data_element_id = ?"))
        {
            st.setInt(1, id);
            try(ResultSet rs = st.executeQuery())
            {
                while( rs.next() )
                {
                    if(rs.getString(2) != null)
                        prop.put(rs.getString(1), rs.getString(2));
                }
            }
        }
        return prop;
    }

    /**
     * Store properties for new data element
     */
    public static void storeProperties(Connection conn, int id, Properties prop) throws SQLException
    {
        if(prop == null) return;
        SqlUtil.execute( conn, DELETE_INFO.num( id ) );
        try(PreparedStatement st = conn.prepareStatement("INSERT INTO de_info(data_element_id,name,value) VALUES(?,?,?)"))
        {
            st.setInt(1, id);
            for(Object keyObj: prop.keySet())
            {
                String key = keyObj.toString();
                if(key.equals(ID_PROPERTY) || key.equals(DataCollectionConfigConstants.NAME_PROPERTY) || key.equals(SqlDataCollection.JDBC_DRIVER_PROPERTY)
                        || key.equals(SqlDataCollection.JDBC_URL_PROPERTY) || key.equals(SqlDataCollection.JDBC_USER_PROPERTY)
                        || key.equals(SqlDataCollection.JDBC_PASSWORD_PROPERTY)) continue;
                st.setString(2, key);
                st.setString(3, prop.getProperty(key));
                st.execute();
            }
        }
    }

    public static void storeProperties(Connection conn, String completeName, Properties prop) throws SQLException
    {
        storeProperties(conn, getIdByName(conn, completeName), prop);
    }
    
    /**
     * Creates new sql data element. Usually called from SqlDataElement subclass constructor
     * @param conn database connection to use
     * @param origin new data element parent collection
     * @param prop properties
     * @param prefix table prefix for this element
     */
    public static String initDataElement(Connection conn, DataCollection<?> origin, Properties prop, String prefix)
    {
        if(origin == null)
            throw new InvalidParameterException("No origin supplied");
        String name = prop.getProperty(DataCollectionConfigConstants.NAME_PROPERTY);
        if(name == null || name.equals(""))
            throw new InvalidParameterException("No name supplied");
        if(origin.contains(name))
        {
            try
            {
                origin.remove(name);
            }
            catch( Exception e )
            {
                throw new DataElementRemoveException( e, origin.getCompletePath().getChildPath( name ) );
            }
        }
        if(!DataCollectionUtils.checkPrimaryElementType(origin, SqlTableDataCollection.class))
        {
            String id = generateTableId(conn, DataElementPath.create(origin, name), prefix);
            id = id.substring(prefix.length()+1);
            return id;
        } else
        {
            SqlUtil.execute( conn, DELETE_QUERY.str( "name", name ).str( "parent", origin.getCompletePath().toString() ) );
            String query = "INSERT INTO data_element(name,parent) VALUES(?,?)";
            try(PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
            {
                ps.setString(1, name);
                ps.setString(2, origin.getCompletePath().toString());
                ps.execute();
                try(ResultSet rs = ps.getGeneratedKeys())
                {
                    if(!rs.next())
                        throw new SQLException("Internal error: no id generated");
                    int id = rs.getInt(1);
                    storeProperties(conn, id, prop);
                    return String.valueOf(id);
                }
            }
            catch( SQLException e )
            {
                throw new BiosoftSQLException( conn, query, e );
            }
        }
    }

    public static int getColumnContentLength(String columnType)
    {
        if(columnType.toUpperCase().contains("TEXT"))
            return Integer.MAX_VALUE;
        Matcher matcher = VARCHAR_FIELD_PATTERN.matcher(columnType);
        if(matcher.matches())
        {
            return Integer.parseInt(matcher.group(1));
        }
        return INITIAL_COLUMN_LENGTH;
    }
}