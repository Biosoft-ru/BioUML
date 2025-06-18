package ru.biosoft.server.servlets.webservices.imports;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.util.TextUtil2;

public class ImportDAO
{
    private static final Logger log = Logger.getLogger( ImportDAO.class.getName() );

    public void insertNewRecord(ImportRecord rec)
    {
        initTable();
        String sql = "INSERT INTO imports(user, status, start_time, target_folder, "
                + "file_name, file_path, file_size, upload_progress, format_list, format, "
                + "format_options, import_progress, upload_id, import_id, omics_type, source_url, de_name)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement( sql, Statement.RETURN_GENERATED_KEYS ))
        {
            initStatementFromRecord( rec, ps );
            ps.executeUpdate();
            try(ResultSet rs = ps.getGeneratedKeys())
            {
                rs.next();
                int id = rs.getInt( 1 );
                rec.setId( id );
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    public void updateRecord(ImportRecord rec)
    {
        initTable();
        String sql = "UPDATE imports SET user=?, status=?, start_time=?"
                + ", target_folder=?, file_name=?, file_path=?, file_size=?, upload_progress=?"
                + ", format_list=?, format=?, format_options=?, import_progress=?, upload_id=?, import_id=?, omics_type=?"
                + ", source_url=?, de_name=?"
                + " WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement( sql ))
        {
            initStatementFromRecord( rec, ps );
            ps.setInt( 18, rec.getId() );
            ps.executeUpdate();
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }

    }

    public Optional<ImportRecord> findByIdAndUser(int id, String user)
    {
        initTable();
        String sql = "SELECT * FROM imports WHERE id=? AND user=?";
        try(PreparedStatement ps = getConnection().prepareStatement( sql ))
        {
            ps.setInt(1, id);
            ps.setString( 2, user );
            try(ResultSet rs = ps.executeQuery())
            {
                if(!rs.next())
                    return Optional.empty();
                ImportRecord rec = createRecrodFromResultSet( rs );
                return Optional.of( rec );
            }
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(e);
        }
    }

    public void removeById(int id)
    {
        initTable();
        String sql = "DELETE FROM imports where id=?";
        try(PreparedStatement ps = getConnection().prepareStatement( sql ))
        {
            ps.setInt( 1, id );
            ps.executeUpdate();
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException( e );
        }
    }

    public Optional<ImportRecord> findByUploadIdAndUser(String uploadId, String user)
    {
        initTable();
        String sql = "SELECT * FROM imports WHERE upload_id=? AND user=?";
        try(PreparedStatement ps = getConnection().prepareStatement( sql ))
        {
            ps.setString( 1, uploadId);
            ps.setString( 2, user );
            try(ResultSet rs = ps.executeQuery())
            {
                if(!rs.next())
                    return Optional.empty();
                ImportRecord rec = createRecrodFromResultSet( rs );
                return Optional.of( rec );
            }
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(e);
        }
    }

    public Optional<ImportRecord> findByImportIdAndUser(String importId, String user)
    {
        initTable();
        String sql = "SELECT * FROM imports WHERE import_id=? AND user=?";
        try(PreparedStatement ps = getConnection().prepareStatement( sql ))
        {
            ps.setString( 1, importId);
            ps.setString( 2, user );
            try(ResultSet rs = ps.executeQuery())
            {
                if(!rs.next())
                    return Optional.empty();
                ImportRecord rec = createRecrodFromResultSet( rs );
                return Optional.of( rec );
            }
        }
        catch(SQLException e)
        {
            throw new BiosoftSQLException(e);
        }
    }


    /**
     * Fetch the list of ImportRecords for a given user.
     * The returned list is sorted by ImportRecord.startTime
     */
    public List<ImportRecord> fetchImports(String user)
    {
        initTable();
        List<ImportRecord> result = new ArrayList<>();
        String sql = "SELECT * FROM imports WHERE user=? ORDER BY start_time";
        try(PreparedStatement ps = getConnection().prepareStatement( sql ))
        {
            ps.setString( 1, user );
            try(ResultSet rs = ps.executeQuery())
            {
                while(rs.next())
                {
                    try
                    {
                        ImportRecord rec = createRecrodFromResultSet(rs);
                        result.add( rec );
                    } catch(Exception e)
                    {
                        log.log(Level.SEVERE,  "Error reading existing import record", e );
                    }
                }
            }
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException( e );
        }
        return result;
    }
    
    public List<ImportRecord> fetchActiveRemoteUploads()
    {
        return queryRecords("SELECT * FROM imports WHERE status='" + ImportStatus.UPLOADING + "' AND source_url IS NOT NULL");
    }
    
    public List<ImportRecord> fetchActiveImports()
    {
        return queryRecords( "SELECT * FROM imports WHERE status='" + ImportStatus.IMPORTING + "'" );
    }
    
    private List<ImportRecord> queryRecords(String query)
    {
        initTable();
        List<ImportRecord> result = new ArrayList<>();
        try(Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(query))
        {
                while(rs.next())
                {
                    try
                    {
                        ImportRecord rec = createRecrodFromResultSet(rs);
                        result.add( rec );
                    } catch(Exception e)
                    {
                        log.log(Level.SEVERE,  "Error reading existing import record", e );
                    }
                }
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException( e );
        }
        return result;
    }

    private Connection getConnection() {
        return GlobalDatabaseManager.getDatabaseConnection();
    }

    private void initTable()
    {
        if( SqlUtil.hasTable( getConnection(), "imports" ) )
            return;
        String query = "CREATE TABLE imports ("
                + "id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,"
                + "user VARCHAR(255) NOT NULL,"
                + "status VARCHAR(32) NOT NULL DEFAULT 'UPLOAD_CREATED',"
                + "start_time datetime NOT NULL,"
                + "target_folder VARCHAR(255) NOT NULL,"
                + "file_name VARCHAR(255) NOT NULL,"
                + "file_path VARCHAR(255),"
                + "file_size BIGINT UNSIGNED NOT NULL DEFAULT 0,"
                + "upload_progress INTEGER UNSIGNED NOT NULL DEFAULT 0,"
                + "format_list TEXT,"
                + "format VARCHAR(255),"
                + "format_options TEXT,"
                + "import_progress INTEGER UNSIGNED NOT NULL DEFAULT 0,"
                + "upload_id VARCHAR(255),"
                + "import_id VARCHAR(255),"
                + "omics_type VARCHAR(32),"
                + "source_url TEXT,"
                + "de_name VARCHAR(255),"
                + "PRIMARY KEY(id), KEY(user, status),"
                + "KEY(upload_id), KEY(import_id)"
                +") ENGINE=MyISAM DEFAULT CHARSET=utf8";
        SqlUtil.execute(getConnection(), query);
    }


    private void initStatementFromRecord(ImportRecord rec, PreparedStatement ps) throws Exception
    {
        ps.setString( 1, rec.getUser() );
        ps.setString( 2, rec.getStatus().name() );
        ps.setTimestamp( 3, new Timestamp( rec.getStartTime() ) );
        ps.setString( 4, rec.getTargetFolder().toString() );
        ps.setString( 5, rec.getFileName() );
        ps.setString( 6, rec.getFile() == null ? null : rec.getFile().getAbsolutePath() );
        ps.setLong( 7, rec.getFileSize() );
        ps.setInt( 8, rec.getUploadProgress() );
        String formatListStr = rec.getFormatList().isEmpty() ? null : String.join( ";", rec.getFormatList() );
        ps.setString( 9, formatListStr );
        ps.setString( 10, rec.getFormat() );


        ps.setString( 11, getFormatOptionsStr(rec) );
        ps.setInt( 12, rec.getImportProgress() );

        ps.setString( 13, rec.getUploadId() );
        ps.setString( 14, rec.getImportId() );
        String omicsTypeStr = rec.getOmicsType() == null ? null : rec.getOmicsType().toString();
        ps.setString( 15, omicsTypeStr );
        ps.setString( 16, rec.getSourceURL() == null ? null : rec.getSourceURL().toString() );
        ps.setString( 17, rec.getDEName() );
    }

    private String getFormatOptionsStr(ImportRecord rec) throws Exception
    {
        Object bean = rec.getFormatOptions();
        if(bean == null)
            return null;
        ComponentModel model = ComponentFactory.getModel(bean, Policy.DEFAULT, true);
        JSONArray json = JSONUtils.getModelAsJSON( model );
        return json.toString();
    }

    private ImportRecord createRecrodFromResultSet(ResultSet rs) throws SQLException
    {
        int id = rs.getInt( 1 );
        String user = rs.getString( 2 );
        ImportStatus status = ImportStatus.valueOf( rs.getString( 3 ) );
        Timestamp startTime = rs.getTimestamp( 4 );
        String targetFolder = rs.getString( 5 );
        String fileName = rs.getString( 6 );
        String filePath = rs.getString( 7 );
        long fileSize = rs.getLong( 8 );
        int uploadProgress = rs.getInt( 9 );
        String formatListStr = rs.getString( 10 );
        String format = rs.getString( 11 );
        String formatOptionsStr = rs.getString( 12 );
        int importProgress = rs.getInt( 13 );
        String uploadId = rs.getString( 14 );
        String importId = rs.getString( 15 );
        String omicsTypeStr = rs.getString( 16 );
        String sourceURLSpec =rs.getString( 17 );
        String deName = rs.getString( 18 );

        ImportRecord rec = new ImportRecord( );
        rec.setId( id );
        rec.setUser( user );
        rec.setStatus( status );
        rec.setStartTime( startTime.getTime() );
        rec.setTargetFolder( DataElementPath.create( targetFolder ) );
        rec.setFileName( fileName );
        if(filePath != null)
            rec.setFile( new File(filePath) );
        rec.setFileSize( fileSize );
        rec.setUploadProgress( uploadProgress );

        List<String> formatList = new ArrayList<>();
        if(formatListStr != null)
        {
            for( String f : TextUtil2.split( formatListStr, ';' ) )
                formatList.add( f );
        }
        rec.setFormatList( formatList  );
        rec.setFormat( format );
        rec.setImportProgress( importProgress );

        if( formatOptionsStr != null )
        {
            rec.setFormatOptionsStr( formatOptionsStr );
        }

        rec.setUploadId( uploadId );
        rec.setImportId( importId );
        rec.setOmicsType( omicsTypeStr == null ? null : OmicsType.valueOf( omicsTypeStr ) );
        URL sourceURL = null;
        if(sourceURLSpec != null)
        {
            try
            {
                sourceURL = new URL( sourceURLSpec );
            }
            catch( MalformedURLException e )
            {
                log.log(Level.WARNING, "Invalid source URL", e );
            }
        }
        rec.setSourceURL( sourceURL );
        rec.setDEName( deName );

        return rec;
    }

}
