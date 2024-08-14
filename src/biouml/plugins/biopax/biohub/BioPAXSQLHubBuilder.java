package biouml.plugins.biopax.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.TextUtil;

/**
 * @author anna
 *
 */
public class BioPAXSQLHubBuilder implements BioHubBuilder
{
    protected Logger log = Logger.getLogger(BioPAXSQLHubBuilder.class.getName());
    protected Properties properties = null;
    protected Connection connection = null;
    private boolean isWorking = false;
    private String tableName = null;

    private static String checkTableQuery = "SELECT * FROM #HUB_TABLE# LIMIT 1";
    private static String createIdTableQuery = "CREATE TABLE #HUB_TABLE#_id (id INT(7) AUTO_INCREMENT NOT NULL PRIMARY KEY, accession VARCHAR(100), complete_name VARCHAR(255) NOT NULL, INDEX (accession))";
    private static String createTableQuery = "CREATE TABLE #HUB_TABLE# (up_id INT(7) NOT NULL, down_id INT(7) NOT NULL, parent INT(7) NOT NULL, INDEX (up_id), INDEX (down_id), UNIQUE(up_id, down_id, parent))";
    private static String insertDataQuery = "INSERT IGNORE INTO #HUB_TABLE# VALUES(?,?,?)";
    private static String insertIdDataQuery = "INSERT INTO #HUB_TABLE#_id (accession, complete_name) VALUES(?,?)";

    private static String createMatchingTable = "CREATE TABLE #HUB_TABLE#_matching (inner_acc VARCHAR(100), outer_acc VARCHAR(100), typeName varchar(50), is_main tinyint(1), KEY(inner_acc,typeName), KEY(outer_acc, typeName))";
    private static String insertMatchingQuery = "INSERT IGNORE INTO #HUB_TABLE#_matching VALUES(?,?,?,?)";

    public static final String HUB_TABLE_PROPERTY = "hub-table";
    private static final int MIN_LINKS_FOR_STOP_LIST = 180;
    
    Map<String, Integer> name2id = new HashMap<>();

    public BioPAXSQLHubBuilder(Properties properties)
    {
        this.properties = properties;
        init();
    }

    private void init()
    {
        if( isWorking )
            return;
        Connection conn = getConnection();
        try
        {
            tableName = properties.getProperty(HUB_TABLE_PROPERTY);
            if( tableName == null )
            {
                log.log(Level.SEVERE, "No BioPAX hub table name specified");
                return;
            }
            try
            {
                SqlUtil.execute(conn, checkTableQuery.replaceAll("#HUB_TABLE#", tableName));
            }
            catch( BiosoftSQLException e1 )
            {
                SqlUtil.execute(conn, createIdTableQuery.replaceAll("#HUB_TABLE#", tableName));
                SqlUtil.execute(conn, createTableQuery.replaceAll("#HUB_TABLE#", tableName));
                SqlUtil.execute(conn, createMatchingTable.replaceAll("#HUB_TABLE#", tableName));
            }

            String matchingTypesProperty = properties.getProperty(ReferenceType.MATCHING_TYPE_PROPERTY);
            if( matchingTypesProperty != null )
            {
                String[] typeNames = TextUtil.split( matchingTypesProperty, ';' );
                for( String typeName : typeNames )
                {
                    ReferenceType type = ReferenceTypeRegistry.optReferenceType(typeName);
                    if( type != null )
                    {
                        matchingTypes.put(type.getDisplayName(), type);
                    }
                }
            }
            isWorking = true;
        }
        catch( BiosoftSQLException e )
        {
            log.log(Level.SEVERE, "Error while BioPAX hub builder initialisation", e);
        }
    }


    @Override
    public void addReference(Element elementFrom, Element elementTo, TargetOptions dbOptions, String relationType, int length, int direction)
    {
        if( !isWorking )
            return;
        Connection conn = getConnection();
        try (PreparedStatement st = conn.prepareStatement( insertDataQuery.replaceAll( "#HUB_TABLE#", tableName ) ))
        {
            int idFrom = getId(elementFrom.getPath());
            int idTo = getId(elementTo.getPath());
            int idParent = getId(relationType);
            st.setInt(1, idFrom);
            st.setInt(2, idTo);
            st.setInt(3, idParent);
            st.execute();
        }
        catch( SQLException e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private final Map<String, ReferenceType> matchingTypes = new HashMap<>();
    @Override
    public void addReference(Element input, ReferenceType inputType, Element output, ReferenceType outputType, boolean isMain)
    {
        if( !isWorking )
            return;
        Connection conn = getConnection();
        PreparedStatement st = null;
        try
        {
            st = conn.prepareStatement(insertMatchingQuery.replace("#HUB_TABLE#", tableName));
            st.setString(1, input.getAccession());
            st.setString(2, output.getAccession());
            st.setString(3, outputType.getSource());
            st.setBoolean(4, isMain);
            st.execute();
            if( !matchingTypes.containsKey(outputType.getDisplayName()) )
                matchingTypes.put(outputType.getDisplayName(), outputType);
        }
        catch( SQLException e )
        {
        }
        finally
        {
            SqlUtil.close(st, null);
        }
    }
    
    @Override
    public void finalizeBuilding()
    {
        try
        {
            Connection conn = getConnection();
            SqlUtil.execute(conn, "create table hub_tmp(id int,parent int,key(id))");
            SqlUtil.execute(conn, "insert into hub_tmp select up_id,parent from "+tableName);
            SqlUtil.execute(conn, "insert into hub_tmp select down_id,parent from "+tableName);
            SqlUtil.execute(conn, "create table stop_list_references (id int primary key)");
            SqlUtil.execute(conn, "insert into stop_list_references select id from hub_tmp group by id having count(*)>"+MIN_LINKS_FOR_STOP_LIST);
            SqlUtil.execute(conn, "delete from "+tableName+" where up_id in (select id from stop_list_references)");
            SqlUtil.execute(conn, "delete from "+tableName+" where down_id in (select id from stop_list_references)");
            SqlUtil.dropTable(conn, "hub_tmp");
        }
        catch( BiosoftSQLException e )
        {
            e.log();
            log.log(Level.SEVERE, "Cannot create stop-list: "+e.getMessage());
        }
    }

    @Override
    public ReferenceType[] getMatchingTypes()
    {
        return matchingTypes.values().toArray(new ReferenceType[matchingTypes.size()]);
    }

    private int getId(String completeName) throws SQLException
    {
        if( name2id.containsKey(completeName) )
            return name2id.get(completeName);
        Connection conn = getConnection();
        int id = 0;
        try (PreparedStatement stId = conn.prepareStatement( insertIdDataQuery.replaceAll( "#HUB_TABLE#", tableName ),
                Statement.RETURN_GENERATED_KEYS ))
        {
            stId.setString(1, DataElementPath.create(completeName).getName());
            stId.setString(2, completeName);
            stId.execute();

            try (ResultSet res = stId.getGeneratedKeys())
            {
                if( res.next() )
                    id = res.getInt( 1 );
            }

        }
        name2id.put(completeName, id);
        return id;
    }

    protected Connection getConnection()
    {
        try
        {
            if( connection == null )
            {
                connection = SqlConnectionPool.getPersistentConnection(properties);
            }
        }
        catch( Exception e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Connection error", e);
        }
        return connection;
    }


}
