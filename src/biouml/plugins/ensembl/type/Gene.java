package biouml.plugins.ensembl.type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.WithSite;
import ru.biosoft.util.HashMapSoftValues;

/**
 * Specification of relationship between BuiUML Gene and
 * Ensembl Gene types:
 * <pre>
 * biouml.standard.type.Gene properties <-> org.ensembl.datamodel.Gene properties
 *
 * chromosome                           <-> location (converted to the "chromosome" coordinate system)
 *
 * regulation                           <-> ?
 *
 * species                              <-> ?
 *
 * structureReferences                  <-> ?
 *
 * completeName                         <-> ?
 *
 * synonyms                             <-> ?
 *
 * databaseReferences                   <-> ? (with some changes) externalRefs
 *
 * description                          <-> description
 *
 * literatureReferences                 <-> ?
 *
 * comment                              <-> ?
 *
 * date                                 <-> modifiedDate
 *
 * title                                <-> displayName
 *
 * name                                 <-> accesionID
 *
 * ?                                    <-> status
 *
 * ?                                    <-> interproIDs
 *
 * ?                                    <-> version
 *
 * ?                                    <-> createdDate
 *
 * ?                                    <-> analysis
 *
 * ?                                    <-> analysisID
 *
 * ?                                    <-> driver (internal Ensembl Java API features)
 *
 * ?                                    <-> internalID (internal Ensembl Java API features)
 * </pre>
 */
@SuppressWarnings ( "serial" )
public class Gene extends biouml.standard.type.Gene implements WithSite, SqlConnectionHolder
{
    private static Logger log = Logger.getLogger(Gene.class.getName());
    protected String status = "";
    protected int version = 0;
    protected String createdDate = null;
    //protected CoreDriver driver;
    private Site site;
    private final DataCollection<?> owner;
    private static Map<Long, String> databases = new HashMapSoftValues();

    public Gene(DataCollection<?> origin, String name, Site site)
    {
        super(origin, name);
        //this.driver = driver;
        this.site = site;
        this.owner = origin;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        String oldValue = this.status;
        this.status = status;
        firePropertyChange("status", oldValue, status);
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        int oldValue = this.version;
        this.version = version;
        firePropertyChange("version", oldValue, version);
    }

    public String getCreatedDate()
    {
        return createdDate;
    }

    public void setCreatedDate(String createdDate)
    {
        String oldValue = this.createdDate;
        this.createdDate = createdDate;
        firePropertyChange("createdDate", oldValue, createdDate);
    }

    @SuppressWarnings ( "unchecked" )
    @Override
    public biouml.standard.type.DatabaseReference[] getDatabaseReferences()
    {
        try
        {
            List<DatabaseReference> reff = fetchReferences( getName() );
            if ( reff != null && reff.size ( ) > 0 )
            {
                return reff.toArray( new DatabaseReference[0] );
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While fetching references for gene "+getName(), e);
        }
        return new biouml.standard.type.DatabaseReference[]{};
    }

    @Override
    public Site getSite()
    {
        return site;
    }

    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getConnection( owner );
    }

    private List<DatabaseReference> fetchReferences(String name)
    {
        Set<DatabaseReference> externalRefs = new HashSet<>();
        ResultSet rs = null;
        Connection conn = null;
        PreparedStatement ps = null;
        String sql = null;
        try
        {
            conn = getConnection();
            sql = " SELECT    xf.xref_id,           \n   xf.dbprimary_acc,     \n   xf.external_db_id,     \n   xf.display_label,       \n   xf.version,          \n   xf.description,       \n   ox.ensembl_object_type, \n   ox.object_xref_id, \n   xf.info_type, \n   xf.info_text \n FROM    xref xf       \n    LEFT JOIN object_xref ox ON ox.xref_id = xf.xref_id\n WHERE   xf.display_label = ?";
            ps = conn.prepareStatement( sql );
            ps.setString( 1, name );
            rs = ps.executeQuery();
            externalRefs.addAll( buildExternalRefs( rs ) );
            sql = "SELECT    xf.xref_id,           \n   xf.dbprimary_acc,     \n   xf.external_db_id,     \n   xf.display_label,       \n   xf.version,          \n   xf.description,       \n   ox.ensembl_object_type, \n   ox.object_xref_id, \n   xf.info_type, \n   xf.info_text \nFROM                  \n  xref xf,             \n  object_xref ox \nWHERE                 \n   ox.xref_id = xf.xref_id \n   AND                   \n   dbprimary_acc = ?";
            ps = conn.prepareStatement( sql );
            ps.setString( 1, name );
            rs = ps.executeQuery();
            externalRefs.addAll( buildExternalRefs( rs ) );
            sql = "SELECT    xf.xref_id,           \n   xf.dbprimary_acc,     \n   xf.external_db_id,     \n   xf.display_label,       \n   xf.version,          \n   xf.description,       \n   ox.ensembl_object_type, \n   ox.object_xref_id, \n   xf.info_type, \n   xf.info_text \nFROM                      \n  xref xf,                \n  external_synonym es,      \n  object_xref ox           \nWHERE                     \n  ox.xref_id = xf.xref_id  \n  AND                     \n  xf.xref_id = es.xref_id   \n  AND                     \n  es.synonym = ?";
            ps = conn.prepareStatement( sql );
            ps.setString( 1, name );
            rs = ps.executeQuery();
            externalRefs.addAll( buildExternalRefs( rs ) );
        }
        catch (SQLException e)
        {
            throw new BiosoftSQLException( this, sql, e );
        }
        finally
        {
            SqlUtil.close( ps, rs );
        }
        return new ArrayList<>( externalRefs );
    }

    private List<DatabaseReference> buildExternalRefs(ResultSet rs) throws SQLException
    {
        ArrayList<DatabaseReference> externalRefs = new ArrayList();

        while ( rs.next() )
        {
            long internalID = rs.getLong( "xref_id" );
            long dbInternalID = rs.getLong( "external_db_id" );

            DatabaseReference databaseReference = new DatabaseReference();
            String externalDatabaseName = fetchDatabaseName( dbInternalID );
            databaseReference.setDatabaseName( externalDatabaseName );
            databaseReference.setId( rs.getString( "display_label" ) );
            databaseReference.setInfo( rs.getString( "info_text" ) );
            databaseReference.setComment( rs.getString( "description" ) );
            databaseReference.setAc( rs.getString( "dbprimary_acc" ) );
            databaseReference.setVersion( rs.getString( "version" ) );
            //????databaseReference.setSynonyms ( ref.getSynonyms ( ) );
            externalRefs.add( databaseReference );
        }
        return externalRefs;
    }

    private synchronized String fetchDatabaseName(long externalDbId)
    {
        if( !databases.containsKey( externalDbId ) )
        {
            String dbName = null;
            String query = "SELECT  db_name FROM external_db WHERE external_db_id=" + externalDbId;
            try (Statement st = getConnection().createStatement(); ResultSet rs = st.executeQuery( query ))
            {
                if( rs.next() )
                {
                    dbName = rs.getString( "db_name" );
                }
            }
            catch (SQLException e)
            {
            }
            databases.put( externalDbId, dbName );
        }

        return databases.get( externalDbId );
    }
}
